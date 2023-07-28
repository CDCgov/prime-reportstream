# Installing Azure Functions Core Tools

## Windows

See https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=windows for detailed instructions.

## macOS

```bash
# Using homebrew
brew update
brew tap azure/functions
brew install azure-functions-core-tools@4

# if you are upgrading on a machine that has 2.x or 3.x installed
brew link --overwrite azure-functions-core-tools@4
```

See https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=macos for detailed instructions.

## Linux

### Debian-based

```bash
sudo apt-get update
sudo apt-get --yes install curl lsb-release gpg # tools needed for subsequent commands

# Pull down Microsoft's Key and mark it as a trusted on
curl https://packages.microsoft.com/keys/microsoft.asc \
    | gpg --dearmor \
    | sudo cat > "/etc/apt/trusted.gpg.d/microsoft.gpg"

# Pick one of the following
# Ubuntu (you most likely want this if you are on any ubuntu derivate)
echo "deb [arch=amd64] https://packages.microsoft.com/repos/microsoft-ubuntu-$(lsb_release -cs)-prod $(lsb_release -cs) main" \
    | sudo tee "/etc/apt/sources.list.d/dotnetdev.list"
# Debian Add the right apt repository (do not use this one on Ubuntu)
echo "deb [arch=amd64] https://packages.microsoft.com/debian/$(lsb_release -rs | cut -d'.' -f 1)/prod $(lsb_release -cs) main" \
    | sudo tee /etc/apt/sources.list.d/dotnetdev.list

sudo apt-get update
sudo apt-get --yes install azure-functions-core-tools-4
```

See https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=linux for detailed instructions (including instructions for Ubuntu-specific ones).
