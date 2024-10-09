#!/bin/bash

source ./lib/waiting.sh

nohup wsl --install -d $1 &
sleep 30
wsl -d $1 -e bash -c \
' \
poweroff -f \
'
wsl.exe --shutdown
sleep 10
wsl --manage $1 --set-sparse true

echo "Creating dev env"
echo ""
echo "***"
echo "CHANGE ROOT PASSWORD FROM 'temp'!"
echo "User '"$3"'"
echo "***"
echo ""

wsl -d $1 -e bash -c \
' \
echo "root:temp" | chpasswd; \
useradd -m '"$3"'; \
echo "'"$3"' ALL=(ALL:ALL) NOPASSWD: ALL" | tee /etc/sudoers.d/'"$3"'; \
chown -R '"$3"':'"$3"' /home/'"$3"'/; \
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg; \
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | tee /etc/apt/sources.list.d/github-cli.list > /dev/null; \
apt-get update; \
apt-get install gh -y; \
echo "nameserver 1.1.1.1" >> /etc/resolv.conf; \
'

./lib/commands/gpg_import.sh $1 $2 $3 $4
./lib/commands/git_setup.sh $1 $2 $3 $4

wsl -d $1 -u $3 -e bash -c \
' \
echo "cd ~" >>~/.bashrc; \
'

# https://github.com/microsoft/WSL/issues/8022
wsl -d $1 -e bash -c \
' \
echo -e "[network] \ngenerateResolvConf = false" > /etc/wsl.conf; \
'
wsl --terminate $1
wsl -d $1 -e bash -c \
' \
rm -f /etc/resolv.conf; \
echo "nameserver 8.8.8.8" > /etc/resolv.conf; \
'

FILE=./modules/$2/install_distro.sh
if [[ -f "$FILE" ]]; then
    echo "Running module specific install"
    $FILE $1 $2 $3 $4 &>/dev/null & waiting
    echo "Completed module specific install"
fi
