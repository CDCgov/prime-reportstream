#!/bin/bash

wsl -d $1 -e bash -c \
' \
touch /home/'"$3"'/.hushlogin; \
touch /home/'"$3"'/.landscape; \
touch /home/'"$3"'/.motd_shown; \
add-apt-repository -y ppa:apt-fast/stable; \
apt-get update; \
DEBIAN_FRONTEND=noninteractive apt-get install -y apt-fast; \
echo debconf apt-fast/maxdownloads string 16 | debconf-set-selections; \
echo debconf apt-fast/dlflag boolean true | debconf-set-selections; \
echo debconf apt-fast/aptmanager string apt-get | debconf-set-selections; \
echo "alias apt-get='\''apt-fast'\''" >> ~/.bashrc; \
apt update; \
apt-get update; \
apt-get -y upgrade; \
apt-get update; \
apt-get install cmake automake clang-15 bison flex libfuse-dev libudev-dev pkg-config libc6-dev-i386 \
gcc-multilib libcairo2-dev libgl1-mesa-dev curl libglu1-mesa-dev libtiff5-dev \
libfreetype6-dev git git-lfs libelf-dev libxml2-dev libegl1-mesa-dev libfontconfig1-dev \
libbsd-dev libxrandr-dev libxcursor-dev libgif-dev libavutil-dev libpulse-dev \
libavformat-dev libavcodec-dev libswresample-dev libdbus-1-dev libxkbfile-dev \
libssl-dev libstdc++-12-dev -y \
apt-get update; \
apt-get install make -y; \
apt-get install jq -y; \
'
