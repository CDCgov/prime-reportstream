#!/bin/bash

wsl -d $1 -u $3 -e bash -c \
' \
cd ~/repos/prime-reportstream/prime-router/; \
DB_USER='\''prime'\''; \
DB_PASSWORD='\''changeIT!'\''; \
./gradlew ktlintCheck; \
docker-compose -f docker-compose.postgres.yml up -d; \
./gradlew package -x fatjar -Pshowtests; \
mkdir -p .vault/env; \
touch .vault/env/.env.local; \
docker-compose -f docker-compose.yml up -d vault; \
sleep 30; \
docker-compose -f docker-compose.yml up -d prime_dev sftp azurite azurite-stage; \
./gradlew reloadTables; \
./gradlew reloadSettings; \
./gradlew testIntegration -Pshowtests; \
docker-compose exec -T sftp chmod 777 /home/foo/upload; \
export $(xargs < .vault/env/.env.local); \
./prime create-credential --type=UserPass --persist=DEFAULT-SFTP --user foo --pass pass; \
./gradlew testSmoke; \
'
