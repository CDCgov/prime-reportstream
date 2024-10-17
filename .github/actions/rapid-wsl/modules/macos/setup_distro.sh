#!/bin/bash

wsl -d $1 -u $3 -e bash -c \
' \
git clone --recursive https://github.com/darlinghq/darling.git \
cd darling \
tools/uninstall \
mkdir build && cd build \
cmake .. \
make -j4 \
sudo make install \
darling shell \
sudo rm -rf /Library/Developer/CommandLineTools \
sudo xcode-select --install \
curl -fsSL -o install.sh https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh \
NONINTERACTIVE=1 /bin/bash install.sh \
(echo; echo 'eval "$(/usr/local/bin/brew shellenv)"') >> /Users/macuser/.profile \
eval "$(/usr/local/bin/brew shellenv)" \
brew tap homebrew/core \
brew install gradle \
brew install docker \
brew install docker-compose \
brew install openjdk@17 postgresql
'
