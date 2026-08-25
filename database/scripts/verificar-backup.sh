#!/bin/sh
set -eu

test "$#" -eq 1 || { echo "Uso: sh verificar-backup.sh archivo.dump" >&2; exit 2; }
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_root=$(CDPATH= cd -- "$script_dir/../.." && pwd)
compose_file="$project_root/docker-compose.production.yml"
env_file="$project_root/.env.production"
backup_dir="$project_root/database/backups"

case "$1" in
  /*) backup_file=$1 ;;
  *) backup_file="$backup_dir/$1" ;;
esac
backup_file=$(realpath "$backup_file")
backup_root=$(realpath "$backup_dir")
case "$backup_file" in
  "$backup_root"/*.dump) ;;
  *) echo "El respaldo debe ser .dump y estar dentro de database/backups." >&2; exit 2 ;;
esac

temporary_name="verify-$$.dump"
container_file="/tmp/$temporary_name"
compose() {
  docker compose --env-file "$env_file" -f "$compose_file" "$@"
}
cleanup() {
  compose exec -T postgres rm -f "$container_file" >/dev/null 2>&1 || true
}
trap cleanup EXIT HUP INT TERM

compose cp "$backup_file" "postgres:$container_file"
compose exec -T postgres pg_restore --list "$container_file"
echo "El catálogo del respaldo es legible."
