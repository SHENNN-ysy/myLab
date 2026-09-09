#!/usr/bin/env bash
# 将本地数据库 resources 表中的 bucket 名批量更新为测试 bucket（默认 mylab-test）。
# 仅作用于本地 docker compose 的 postgres 容器，不影响生产环境。
#
# 用法：
#   bash scripts/db-update-bucket.sh                 # 更新为 mylab-test
#   bash scripts/db-update-bucket.sh my-other-bucket # 更新为指定 bucket
#
# 注意：执行后请同步把本地 .env / application-dev.yml 的 OSS_BUCKET 改为同名，
# 否则后端生成的访问 URL 与上传仍指向旧 bucket。

set -euo pipefail

NEW_BUCKET="${1:-mylab-test}"

# 在仓库根目录执行，确保 docker compose 能找到 compose 文件
cd "$(dirname "$0")/.."

# 通过容器内环境变量取连接参数，SQL 经标准输入传入
run_sql() {
  docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' <<<"$1"
}

echo "==> 更新前 bucket 分布："
run_sql "SELECT bucket, count(*) FROM resources GROUP BY bucket ORDER BY bucket;"

echo "==> 将全部记录更新为 bucket = '${NEW_BUCKET}' ..."
run_sql "
BEGIN;
UPDATE resources SET bucket = '${NEW_BUCKET}', updated_at = now() WHERE bucket <> '${NEW_BUCKET}';
COMMIT;
"

echo "==> 更新后 bucket 分布："
run_sql "SELECT bucket, count(*) FROM resources GROUP BY bucket ORDER BY bucket;"

echo "完成。请确认本地配置的 OSS_BUCKET 也已改为 '${NEW_BUCKET}'。"
