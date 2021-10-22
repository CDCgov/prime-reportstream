# Installing OpenJDK

Note that we currently target OpenJDK 11 through 15, examples use OpenJDK 11.

## Windows

See https://jdk.java.net/ for detailed instructions.

## macOS

```bash
brew update
# Set to 15 if you so choose
VERSION=11
brew install openjdk@${VERSION?}
```

## Linux

### Debian-based
```bash
sudo apt-get update
# Set to 15 if you so choose
VERSION=11
sudo apt-get --yes install openjdk-${VERSION?}-jdk
```