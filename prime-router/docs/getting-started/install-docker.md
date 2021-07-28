aptitud# Installing Docker

Both docker and docker-compose must be installed on your system.

## Windows and macOS

See https://www.docker.com/get-started for detailed instructions.

## Linux

### Debian-based
```bash
# If you are on
sudo apt-get update
sudo apt-get --yes install lsb-release
echo "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee -a /etc/apt/sources.list.d/docker.list
sudo apt-get update # Pick up new repo
sudo apt-get --yes install docker-compose # This will pick up your docker runtime as well
```