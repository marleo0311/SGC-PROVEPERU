#!/bin/sh
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_root=$(CDPATH= cd -- "$script_dir/../.." && pwd)
compose_file="$project_root/docker-compose.production.yml"
env_file="$project_root/.env.production"
backup_dir="$project_root/database/backups"
label=${1:-}

case "$label" in
  *[!A-Za-z0-9_-]*) echo "La etiqueta solo admite letras, números, guion y guion bajo." >&2; exit 2 ;;
esac

test -f "$env_file" || { echo "Falta .env.production" >&2; exit 2; }
mkdir -p "$backup_dir"
timestamp=$(date +%Y%m%d-%H%M%S)
suffix=""
test -z "$label" || suffix="-$label"
filename="sgc-proveperu-$timestamp$suffix.dump"
container_file="/tmp/$filename"
destination="$backup_dir/$filename"

compose() {
  docker compose --env-file "$env_file" -f "$compose_file" "$@"
}

cleanup() {
  compose exec -T postgres rm -f "$container_file" >/dev/null 2>&1 || true
}
trap cleanup EXIT HUP INT TERM

echo "Creando respaldo consistente de PostgreSQL..."
compose exec -T postgres sh -ec \
  'umask 077; exec pg_dump --format=custom --compress=9 --no-owner --no-acl --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" --file="$1"' \
  sh "$container_file"
compose cp "postgres:$container_file" "$destination"
test -s "$destination" || { echo "El respaldo quedó vacío." >&2; exit 3; }
chmod 600 "$destination"
echo "Respaldo creado: $destination"
echo "Verifícalo antes de copiarlo fuera del servidor."
