#!/bin/bash

echo "Building PRIME Data Hub Frontend using docker build"
docker build -f ./Dockerfile .
dkr_image=$(docker build -q -f ./Dockerfile .)
echo dkr_image $dkr_image

# create temporary docker container to access static website files
dkr_container=$(docker create $dkr_image)
echo dkr_container $dkr_container

echo "Extracting static website"
docker cp $dkr_container:/usr/app/_site/ ./dist

# remove temporary docker container
docker rm -v $dkr_container

echo "Static website files are in ./dist:"
ls ./dist
