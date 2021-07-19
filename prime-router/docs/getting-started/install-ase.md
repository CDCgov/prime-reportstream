# Installing Azure Storage Explorer

Azure Storage Explorer is a handy client-side tool that enables you to peer into your Azure Storage Blobs.

See these links for more information:

* https://azure.microsoft.com/en-us/features/storage-explorer/
* https://www.storageexplorer.com

There is also a helpful [Getting Started Guide](https://docs.microsoft.com/en-us/azure/vs-azure-tools-storage-manage-with-storage-explorer) available.

## Windows

See https://docs.microsoft.com/en-us/azure/vs-azure-tools-storage-manage-with-storage-explorer?tabs=windows for further instructions.

## macOS

See https://docs.microsoft.com/en-us/azure/vs-azure-tools-storage-manage-with-storage-explorer?tabs=macos for further instructions

## Linux

### Snap

See https://snapcraft.io/storage-explorer

### Tarball

1. Download the Tarball from [the instructions](https://docs.microsoft.com/en-us/azure/vs-azure-tools-storage-manage-with-storage-explorer?tabs=linux)
2. Install the dependencies as listed in [the instructinos](https://docs.microsoft.com/en-us/azure/storage/common/storage-explorer-troubleshooting?tabs=Windows%2C2004#linux-dependencies)
    1. .NET Core

        ```bash
        # For Ubuntu 20.04
        wget https://packages.microsoft.com/config/ubuntu/20.04/packages-microsoft-prod.deb -O packages-microsoft-prod.deb; \
            sudo dpkg -i packages-microsoft-prod.deb; \
            sudo apt-get update; \
            sudo apt-get install -y apt-transport-https && \
            sudo apt-get update && \
            sudo apt-get install -y dotnet-runtime-2.1
        ```

    2. You may also have to install these dependencies:
        * iproute2
        * libasound2
        * libatm1
        * libgconf2-4
        * libnspr4
        * libnss3
        * libpulse0
        * libsecret-1-0
        * libx11-xcb1
        * libxss1
        * libxtables11
        * libxtst6
        * xdg-utils
3. Extract the Tarball and run `./StorageExplorer` in the extracted directory (which should already be `+x` by default)
