# Installing git

## Windows

Download the setup from [https://git-scm.com/download/win](https://git-scm.com/download/win) and run it; you'll want to install git-bash as well.

## macOS

Attempting to run a git command like `git --version` will prompt you to install dev tools, which includes git. Otherwise you can use [brew](https://brew.sh) which itself is used to install many other things. 

```bash
# Using homebrew
brew update
brew install git
```

For more instructions, see https://git-scm.com/download/mac

## Linux

### Debian-based
```bash
sudo apt-get update
sudo apt-get --yes install git
```