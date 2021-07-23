# Installing Azure CLI

## Windows

Execute the following in PowerShell
```powershell
Invoke-WebRequest -Uri https://aka.ms/installazurecliwindows -OutFile .\AzureCLI.msi; Start-Process msiexec.exe -Wait -ArgumentList '/I AzureCLI.msi /quiet'; rm .\AzureCLI.msi
```

See https://docs.microsoft.com/en-us/cli/azure/install-azure-cli-windows for further instructions.

## macOS

```bash
# Using homebrew
brew update
brew install azure-cli
```

See https://docs.microsoft.com/en-us/cli/azure/install-azure-cli-macos for further instructions.

## Linux

### Debian-based

```bash
sudo apt-get update
sudo apt-get --yes install azure-cli
```

See https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=linux for further instructions (including instructions for Ubuntu-specific ones).