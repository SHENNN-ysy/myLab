#!/bin/sh

# 通过 Docker Registry V2 HTTP API 查询 tag/digest 或删除 manifest。
set -eu

command=${1:-}
registry_host=${2:-}
repository=${3:-}
reference=${4:-}

fail() {
    echo "Registry API 操作失败: $*" >&2
    exit 1
}

usage() {
    echo "用法: $0 ping <host> | tags <host> <repo> | digest <host> <repo> <tag> | delete <host> <repo> <digest>" >&2
    exit 2
}

[ -n "$command" ] && [ -n "$registry_host" ] || usage
command -v curl >/dev/null 2>&1 || fail "缺少 curl"

registry_host=${registry_host%/}
case "$registry_host" in
    http://*|https://*) registry_url=$registry_host ;;
    *) registry_url="http://$registry_host" ;;
esac

api_url="$registry_url/v2"
manifest_accept='application/vnd.docker.distribution.manifest.v2+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.oci.image.index.v1+json'

case "$command" in
    ping)
        curl --fail --silent --show-error "$api_url/" >/dev/null
        ;;
    tags)
        [ -n "$repository" ] || usage
        command -v jq >/dev/null 2>&1 || fail "缺少 jq"
        curl --fail --silent --show-error "$api_url/$repository/tags/list?n=10000" \
            | jq -r '.tags[]?'
        ;;
    digest)
        [ -n "$repository" ] && [ -n "$reference" ] || usage
        headers_file=$(mktemp)
        trap 'rm -f "$headers_file"' EXIT HUP INT TERM
        curl --fail --silent --show-error --head \
            -H "Accept: $manifest_accept" \
            "$api_url/$repository/manifests/$reference" > "$headers_file"
        digest=$(tr -d '\r' < "$headers_file" \
            | awk 'tolower($0) ~ /^docker-content-digest:/ { sub(/^[^:]*:[[:space:]]*/, ""); print; exit }')
        printf '%s' "$digest" | grep -Eq '^sha256:[0-9a-f]{64}$' \
            || fail "$repository:$reference 未返回有效 digest"
        printf '%s\n' "$digest"
        ;;
    delete)
        [ -n "$repository" ] && [ -n "$reference" ] || usage
        printf '%s' "$reference" | grep -Eq '^sha256:[0-9a-f]{64}$' \
            || fail "无效 digest: $reference"
        curl --fail --silent --show-error \
            -X DELETE \
            -H "Accept: $manifest_accept" \
            "$api_url/$repository/manifests/$reference" >/dev/null
        ;;
    *)
        usage
        ;;
esac
