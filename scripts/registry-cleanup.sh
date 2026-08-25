#!/bin/sh

# 保留每个仓库最近 N 个 release tag，并额外保护最后一次成功部署版本。
set -eu

REGISTRY_API_HOST=${REGISTRY_API_HOST:-registry:5000}
REGISTRY_HOST=${REGISTRY_HOST:-127.0.0.1:5000}
REGISTRY_CONTAINER=${REGISTRY_CONTAINER:-myblog-registry}
REGISTRY_KEEP_TAGS=${REGISTRY_KEEP_TAGS:-5}
REGISTRY_COMPOSE_FILE=${REGISTRY_COMPOSE_FILE:-deploy/registry/docker-compose.yml}
REGISTRY_ENV_FILE=${REGISTRY_ENV_FILE:-/opt/myblog/deploy/.env}
DEPLOY_STATE_FILE=${DEPLOY_STATE_FILE:-/var/jenkins_home/deploy-state/myblog-current-release}
DRY_RUN=${DRY_RUN:-false}

release_pattern='^[0-9]{8}-[0-9]{6}-[0-9a-f]{7}$'
repositories='myblog-api myblog-web myblog-admin'
registry_was_stopped=false
work_directory=$(mktemp -d)

restore_registry() {
    if [ "$registry_was_stopped" = "true" ]; then
        docker compose --env-file "$REGISTRY_ENV_FILE" -f "$REGISTRY_COMPOSE_FILE" up -d registry >/dev/null 2>&1 || true
    fi
    rm -rf "$work_directory"
}
trap restore_registry EXIT HUP INT TERM

fail() {
    echo "Registry 清理终止: $*" >&2
    exit 1
}

case "$REGISTRY_KEEP_TAGS" in
    ''|*[!0-9]*) fail "REGISTRY_KEEP_TAGS 必须是正整数" ;;
    0) fail "REGISTRY_KEEP_TAGS 必须大于 0" ;;
esac

case "$DRY_RUN" in
    true|false) ;;
    *) fail "DRY_RUN 只能为 true 或 false" ;;
esac

command -v reg >/dev/null 2>&1 || fail "缺少 reg 工具"
command -v curl >/dev/null 2>&1 || fail "缺少 curl 工具"
command -v docker >/dev/null 2>&1 || fail "缺少 docker 工具"

registry_status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$REGISTRY_CONTAINER" 2>/dev/null || echo missing)
[ "$registry_status" = "healthy" ] || fail "Registry 容器状态为 $registry_status"
curl --fail --silent --show-error "http://$REGISTRY_API_HOST/v2/" >/dev/null || fail "Registry API 不可用"

[ -f "$DEPLOY_STATE_FILE" ] || fail "当前部署状态文件不存在: $DEPLOY_STATE_FILE"
current_tag=$(tr -d '\r\n' < "$DEPLOY_STATE_FILE")
printf '%s' "$current_tag" | grep -Eq "$release_pattern" || fail "当前部署 tag 格式错误: $current_tag"

# 先确认运行容器与状态文件完全一致，任何异常都发生在删除前。
for mapping in 'myblog-api:backend' 'myblog-web:frontend-web' 'myblog-admin:frontend-admin'; do
    repository=${mapping%%:*}
    service=${mapping#*:}
    container_ids=$(docker ps \
        --filter "label=com.docker.compose.project=myblog" \
        --filter "label=com.docker.compose.service=$service" \
        --format '{{.ID}}')
    [ "$(printf '%s\n' "$container_ids" | sed '/^$/d' | wc -l | tr -d ' ')" = "1" ] || fail "$service 运行容器数量不是 1"
    running_image=$(docker inspect --format '{{.Config.Image}}' "$container_ids")
    expected_image="$REGISTRY_HOST/$repository:$current_tag"
    [ "$running_image" = "$expected_image" ] || fail "$service 当前镜像 $running_image 与状态 $expected_image 不一致"
done

# 为全部仓库生成删除计划并验证当前 tag，完成后才进入删除阶段。
for repository in $repositories; do
    repository_directory="$work_directory/$repository"
    mkdir -p "$repository_directory"
    all_tags_file="$repository_directory/all-tags"
    keep_tags_file="$repository_directory/keep-tags"
    delete_tags_file="$repository_directory/delete-tags"
    keep_digests_file="$repository_directory/keep-digests"

    reg -f digest "$REGISTRY_API_HOST/$repository:$current_tag" >/dev/null 2>&1 || fail "$repository 缺少当前部署 tag $current_tag"
    reg -f tags "$REGISTRY_API_HOST/$repository" \
        | grep -E "$release_pattern" \
        | sort -r > "$all_tags_file"
    [ -s "$all_tags_file" ] || fail "$repository 没有有效 release tag"

    head -n "$REGISTRY_KEEP_TAGS" "$all_tags_file" > "$keep_tags_file"
    grep -Fxq "$current_tag" "$keep_tags_file" || printf '%s\n' "$current_tag" >> "$keep_tags_file"

    : > "$delete_tags_file"
    while IFS= read -r candidate_tag; do
        grep -Fxq "$candidate_tag" "$keep_tags_file" || printf '%s\n' "$candidate_tag" >> "$delete_tags_file"
    done < "$all_tags_file"

    : > "$keep_digests_file"
    while IFS= read -r protected_tag; do
        reg -f digest "$REGISTRY_API_HOST/$repository:$protected_tag" >> "$keep_digests_file"
    done < "$keep_tags_file"
    sort -u -o "$keep_digests_file" "$keep_digests_file"

    echo "$repository 保留 tag："
    sed 's/^/  - /' "$keep_tags_file"
    if [ -s "$delete_tags_file" ]; then
        echo "$repository 待删除 tag："
        sed 's/^/  - /' "$delete_tags_file"
    else
        echo "$repository 无需删除"
    fi
done

if [ "$DRY_RUN" = "true" ]; then
    echo "DRY_RUN 完成，未删除 manifest，也未执行垃圾回收。"
    exit 0
fi

for repository in $repositories; do
    repository_directory="$work_directory/$repository"
    delete_tags_file="$repository_directory/delete-tags"
    keep_digests_file="$repository_directory/keep-digests"
    deleted_digests_file="$repository_directory/deleted-digests"
    : > "$deleted_digests_file"

    while IFS= read -r delete_tag; do
        [ -n "$delete_tag" ] || continue
        delete_digest=$(reg -f digest "$REGISTRY_API_HOST/$repository:$delete_tag")
        if grep -Fxq "$delete_digest" "$keep_digests_file"; then
            echo "跳过 $repository:$delete_tag：manifest 仍被保留 tag 引用"
            continue
        fi
        if grep -Fxq "$delete_digest" "$deleted_digests_file"; then
            continue
        fi
        reg -f rm "$REGISTRY_API_HOST/$repository@$delete_digest"
        printf '%s\n' "$delete_digest" >> "$deleted_digests_file"
    done < "$delete_tags_file"
done

echo "停止 Registry 并执行垃圾回收..."
docker compose --env-file "$REGISTRY_ENV_FILE" -f "$REGISTRY_COMPOSE_FILE" stop registry
registry_was_stopped=true
docker compose --env-file "$REGISTRY_ENV_FILE" -f "$REGISTRY_COMPOSE_FILE" run --rm --no-deps registry \
    registry garbage-collect --delete-untagged /etc/docker/registry/config.yml
docker compose --env-file "$REGISTRY_ENV_FILE" -f "$REGISTRY_COMPOSE_FILE" up -d registry
registry_was_stopped=false

for attempt in $(seq 1 30); do
    registry_status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$REGISTRY_CONTAINER" 2>/dev/null || echo missing)
    if [ "$registry_status" = "healthy" ] && curl --fail --silent "http://$REGISTRY_API_HOST/v2/" >/dev/null 2>&1; then
        echo "Registry 清理完成并恢复健康。"
        exit 0
    fi
    echo "等待 Registry 恢复... ($attempt/30)"
    sleep 2
done

fail "Registry 垃圾回收后未恢复健康"
