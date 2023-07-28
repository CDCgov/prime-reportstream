# Installing Docker

Both docker and docker-compose must be installed on your system.

## Windows and macOS

See https://www.docker.com/get-started for detailed instructions.

## Linux

### Debian-based
```
sudo apt-get update
sudo apt-get --yes install lsb-release
echo "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee -a /etc/apt/sources.list.d/docker.list
sudo apt-get update # Pick up new repo
sudo apt-get --yes install docker-compose # This will pick up your docker runtime as well
```
If you get
```
Got permission denied while trying to connect to the Docker daemon socket at unix:///var/run/docker.sock:
```
when running docker commands, run
```
$ sudo usermod -aG docker $USER
```
and log in again to pick up the group change.
