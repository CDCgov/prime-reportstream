#!/bin/bash

wsl -d $1 -e bash -c \
' \
touch /home/'"$3"'/.hushlogin; \
touch /home/'"$3"'/.landscape; \
touch /home/'"$3"'/.motd_shown; \
apt update; \
apt -y upgrade; \
apt --yes install lsb-release gpg; \
curl https://packages.microsoft.com/keys/microsoft.asc \
    | gpg --dearmor \
    | tee "/etc/apt/trusted.gpg.d/microsoft.gpg"; \
echo "deb [arch=amd64] https://packages.microsoft.com/repos/microsoft-ubuntu-$(lsb_release -cs)-prod $(lsb_release -cs) main" \
    | tee "/etc/apt/sources.list.d/dotnetdev.list"; \
apt update; \
apt remove azure-cli -y && apt autoremove -y; \
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash; \
apt -y install unzip; \
apt update; \
curl -fsSL https://apt.releases.hashicorp.com/gpg | apt-key add -; \
apt-add-repository "deb [arch=$(dpkg --print-architecture)] https://apt.releases.hashicorp.com $(lsb_release -cs) main"; \
apt install terraform -y; \
apt install strongswan -y; \
apt install strongswan-pki -y; \
apt install make -y; \
apt install jq -y; \
'
