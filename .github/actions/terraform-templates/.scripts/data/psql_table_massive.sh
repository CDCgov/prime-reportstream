sudo apt update
sudo apt install -y p7zip-full

cd /mnt/storage/
echo "restart script if slow download speed (<5Mbps):"
curl -L https://archive.org/download/stackexchange/stackoverflow.com-PostHistory.7z --output PostHistory.7z
7z x PostHistory.7z

python stackoverflow_in_pg/python_src/so2pg-posthistory.py PostHistory.xml > posthistory.sql

echo -e "Create table and insert data:\n=================="
cat << EOF
CREATE TABLE PostHistory (
    id                  INTEGER UNIQUE NOT NULL,
    type                INTEGER,
    postid              INTEGER,
    revisionguid        TEXT,
    creation            TIMESTAMP NOT NULL,
    userid              INTEGER,
    userdisplaymame     TEXT,
    text                TEXT
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
-f /mnt/storage/posthistory.sql
EOF
echo -e "=================="
