#!/bin/bash

echo
echo "Building PRIME Data Hub frontend using docker"

if [[ -z "${ACR_REPO}" ]]; then
  # build locally, but without tagging

  echo
  docker build . -f ./Dockerfile

else
  # build with tag matchin CI/CD of PRIME Data Hub
  dkr_tag_name="${ACR_REPO}/${PREFIX}_frontend:latest"

  echo "Tagging static webserver docker image as: ${dkr_tag_name}"
  echo

  docker build . \
    --file ./Dockerfile \
    --target nginx_host \
    --tag $dkr_tag_name

  # TODO: push the docker image into the ACR_REPO
  # docker push $dkr_tag_name
fi

echo


if true; then
  echo
  echo "Extracting static website files from docker image"

  dkr_site_image=$(docker build . -q --file ./Dockerfile --target static_site)

  # create temporary docker container to access static website files
  dkr_site_container=$(docker create $dkr_site_image)

  # extracting static website into ./dist
  docker cp $dkr_site_container:/usr/app/_site/ ./dist

  # remove temporary docker container
  dkr_site_term=$(docker rm -fv $dkr_site_container)

  echo "Static website files are in ./dist:"
  ls ./dist
  echo
  echo
fi

