sudo apt update
sudo apt install -y p7zip-full

cd /mnt/storage/
echo "restart script if slow download speed (<5Mbps):"
curl -L https://archive.org/download/stackexchange/stackoverflow.com-Tags.7z --output Tags.7z
7z x Tags.7z

python stackoverflow_in_pg/python_src/so2pg-tags.py Tags.xml > tags.sql

echo -e "Create table and insert data:\n=================="
cat << EOF
CREATE TABLE Tags (
    id          INTEGER UNIQUE NOT NULL,
    name        TEXT UNIQUE NOT NULL,
    count       INTEGER,
    excerptpost INTEGER,
    wikipost    INTEGER
);

export PGPASSWORD="<password>"
host="<cluster name>.postgres.cosmos.azure.com"
user="citus"
db="citus"
schema="public"
psql \
-h $host \
-d $db \
-U $user \
-f /mnt/storage/tags.sql
EOF
echo -e "=================="
