#!/bin/bash

echo
echo "Validating static webserver docker image"

dkr_nginx_image=$(docker build . -q --file ./Dockerfile --target nginx_host)
dkr_nginx_container=$(docker run -d --rm -p 8080:80 $dkr_nginx_image)

echo "Awaiting healthy startup"
while [ "healthy" != "`docker inspect -f {{.State.Health.Status}} $dkr_nginx_container`" ];
do
  echo "Health Status:" $(docker inspect -f '{{.State.Health.Status}}' $dkr_nginx_container)
  sleep 1;
done
echo "Health Status:" $(docker inspect -f '{{.State.Health.Status}}' $dkr_nginx_container)
echo

curl -s --head "http://localhost:8080/"

dkr_nginx_term=$(docker rm -fv $dkr_nginx_container)

echo
echo
