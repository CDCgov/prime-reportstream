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
apt -y install openjdk-11-jdk; \
apt-get --yes install lsb-release gpg; \
curl https://packages.microsoft.com/keys/microsoft.asc \
    | gpg --dearmor \
    | tee "/etc/apt/trusted.gpg.d/microsoft.gpg"; \
echo "deb [arch=amd64] https://packages.microsoft.com/repos/microsoft-ubuntu-$(lsb_release -cs)-prod $(lsb_release -cs) main" \
    | tee "/etc/apt/sources.list.d/dotnetdev.list"; \
apt-get update; \
apt-get --yes install azure-functions-core-tools-3; \
apt remove azure-cli -y && apt autoremove -y; \
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash; \
apt-get install python3.6 -y; \
apt-get install python3-venv -y; \
apt-get -y install unzip; \
apt -y install gradle; \
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -; \
echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list; \
apt-get update; \
apt-get -y install postgresql-11; \
curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose; \
curl -fsSL https://apt.releases.hashicorp.com/gpg | apt-key add -; \
apt-add-repository "deb [arch=$(dpkg --print-architecture)] https://apt.releases.hashicorp.com $(lsb_release -cs) main"; \
apt install terraform=1.0.5 -y; \
apt-get install make -y; \
chmod +x /usr/local/bin/docker-compose; \
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash; \
'
