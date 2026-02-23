# Using Azure Storage Explorer with Local Azurite

This guide explains how to use Azure Storage Explorer to interact with your local Azurite blob storage instances for managing schemas and other data during ReportStream development.

## Table of Contents

- [Using Azure Storage Explorer with Local Azurite](#using-azure-storage-explorer-with-local-azurite)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
  - [Prerequisites](#prerequisites)
  - [Installing Azure Storage Explorer](#installing-azure-storage-explorer)
    - [macOS](#macos)
    - [Windows](#windows)
    - [Linux](#linux)
  - [Connecting to Azurite Instances](#connecting-to-azurite-instances)
    - [Primary Azurite Instance (Port 10000)](#primary-azurite-instance-port-10000)
    - [Remote Azure instance](#remote-azure-instance)
  - [Navigating the Storage Structure](#navigating-the-storage-structure)
    - [Key Containers](#key-containers)
  - [Common Operations](#common-operations)
    - [Viewing Blob Containers](#viewing-blob-containers)
    - [Uploading Files](#uploading-files)
    - [Downloading Files](#downloading-files)
    - [Creating Folders](#creating-folders)
    - [Editing Files](#editing-files)
    - [Deleting Files](#deleting-files)
  - [Managing Schemas](#managing-schemas)
    - [Typical Schema Structure](#typical-schema-structure)
    - [Creating a New Schema](#creating-a-new-schema)
    - [Editing an Existing Schema](#editing-an-existing-schema)
    - [Syncing Schemas Between Environments](#syncing-schemas-between-environments)
  - [Additional Resources](#additional-resources)

## Overview

ReportStream uses Azurite, a local Azure Storage emulator, for blob storage during development. Azure Storage Explorer provides a user-friendly GUI to interact with these storage instances, making it easy to:

- Browse and manage blob containers
- Upload and download schemas
- View and edit configuration files
- Debug storage-related issues

There are two Azurite instances running in your local Docker environment:
- **Primary instance** (port 10000): Used by the main ReportStream application
- **Secondary instance** (port 11000): Used for testing and validation

## Prerequisites

Before proceeding, ensure you have:

1. Completed the [initial setup](README.md) for ReportStream
2. Docker Desktop running
3. Started your local ReportStream environment with `docker compose up`
4. Verified that Azurite containers are running:
   ```bash
   docker compose ps
   ```
   You should see both `azurite` and `azurite-stage` containers running.

## Installing Azure Storage Explorer

### macOS

1. Visit the [Azure Storage Explorer download page](https://azure.microsoft.com/en-us/products/storage/storage-explorer)
2. Download the macOS version
3. Open the downloaded `.dmg` file
4. Drag Azure Storage Explorer to your Applications folder
5. Launch Azure Storage Explorer from Applications

### Windows

1. Visit the [Azure Storage Explorer download page](https://azure.microsoft.com/en-us/products/storage/storage-explorer)
2. Download the Windows installer
3. Run the installer and follow the installation wizard
4. Launch Azure Storage Explorer from the Start menu

### Linux

1. Visit the [Azure Storage Explorer download page](https://azure.microsoft.com/en-us/products/storage/storage-explorer)
2. Download the appropriate package for your distribution
3. Install following your distribution's package manager instructions
4. Launch Azure Storage Explorer

## Connecting to Azurite Instances

### Primary Azurite Instance (Port 10000)

The primary Azurite instance is automatically detected by Azure Storage Explorer as the local development storage emulator.

**Automatic Connection**

Azure Storage Explorer should automatically detect and connect to your local Azurite instance. Look for:
- **Emulator & Attached** → **Storage Accounts** → **Emulator - Default Ports (Key)**

If you see this, you're already connected! Skip to [Navigating the Storage Structure](#navigating-the-storage-structure).

### Remote Azure instance

To connect to a remote Azure storage account, you'll need a connection string and access permissions from an administrator.

**Manual Connection**

1. In Azure Storage Explorer, right-click on **Storage Accounts**
2. Select **Connect to Azure Storage**
3. Choose **Storage account or service**
4. Select **Connection string (Key or SAS)**
5. Enter a display name and paste the connection string provided by your administrator
6. Click **Next** and then **Connect**

## Navigating the Storage Structure

Once connected, you'll see a hierarchical structure:

```
Storage Accounts
└── Emulator - Default Ports (Key) [or your custom name]
    └── Blob Containers
        ├── apidocs
        ├── metadata
        ├── receive
        └── reports
```

### Key Containers

- **metadata**: Contains FHIR and HL7 translation schemas
  - `fhir_mapping/` - FHIR transformation schemas
  - `hl7_mapping/` - HL7 transformation schemas
  - `valid-{timestamp}.txt` - Validation status markers
  
- **reports**: Stores generated reports and output files

- **receive**: Stores incoming data submissions

- **apidocs**: Contains Swagger UI documentation

## Common Operations

### Viewing Blob Containers

1. Expand your storage account in the left sidebar
2. Expand **Blob Containers**
3. Click on any container to view its contents
4. The right pane shows the folder structure and files

### Uploading Files

**Single File Upload:**

1. Navigate to the destination folder in the container
2. Click the **Upload** button in the toolbar
3. Select **Upload Files**
4. Choose your file(s)
5. Click **Upload**

**Folder Upload:**

1. Navigate to the destination in the container
2. Click the **Upload** button
3. Select **Upload Folder**
4. Choose the folder containing your files
5. Click **Upload**

### Downloading Files

1. Navigate to the file you want to download
2. Right-click on the file
3. Select **Download**
4. Choose the destination on your local machine
5. Click **Save**

Alternatively, double-click a file to download and open it with the default application.

### Creating Folders

1. Navigate to where you want to create a new folder
2. Click **New Folder** in the toolbar
3. Enter the folder name (e.g., `my-schema-folder`)
4. Press Enter

> **Note:** Azure Blob Storage doesn't have true folders; they're simulated using path prefixes. You must upload at least one file to a folder for it to persist.

### Editing Files

1. Double-click the file you want to edit
2. The file will be downloaded and opened in your default application
3. Make your changes and save the file
4. Azure Storage Explorer will prompt: "Do you want to upload the file?"
5. Click **Upload** to save your changes to blob storage

### Deleting Files

1. Right-click on the file or folder
2. Select **Delete**
3. Confirm the deletion

> **Warning:** Deletion is immediate and cannot be undone in the local Azurite instance.

## Managing Schemas

Schemas are stored in the `metadata` container and are organized by type and receiver/sender.

### Typical Schema Structure

```
metadata/
├── hl7_mapping/
│   ├── receivers/
│   │   └── STLTs/
│   │       └── CA/
│   │           ├── CA.yml
│   │           ├── input.fhir
│   │           └── output.hl7
│   └── valid-2025-02-12T10:30:00Z.txt
└── fhir_mapping/
    └── [similar structure]
```

### Creating a New Schema

1. Open Azure Storage Explorer and connect to your primary Azurite instance
2. Navigate to **Blob Containers** → **metadata**
3. Navigate to the appropriate directory (e.g., `hl7_mapping/receivers/STLTs/`)
4. Create a new folder for your schema (e.g., `MY_STATE`)
5. Create your schema files locally:
   - `transform.yml` or `{state}.yml` - The transformation schema
   - `input.fhir` - Sample input data
   - `output.hl7` or `output.fhir` - Expected output
6. Upload all three files to your new folder
7. Validate your schema using the CLI:
   ```bash
   ./prime validateSchemas --schema-type="HL7" \
     --blob-store-connect="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://localhost:10000/devstoreaccount1;" \
     --blob-store-container="metadata"
   ```

### Editing an Existing Schema

1. Navigate to the schema directory in the `metadata` container
2. Double-click the schema file (e.g., `CA.yml`)
3. Edit the file in your preferred editor
4. Save the file - Azure Storage Explorer will prompt to upload
5. Update the corresponding `output.hl7` or `output.fhir` file to match your changes
6. Validate the changes using the CLI (see command above)
7. Look for the updated `valid-{timestamp}.txt` file to confirm successful validation

### Syncing Schemas Between Environments

For detailed instructions on syncing schemas between local, staging, and production environments, see the [Managing Translation Schemas in Azure](../standard-operating-procedures/managing-translation-schemas-in-azure.md) guide.

## Additional Resources

- [Managing Translation Schemas in Azure](../standard-operating-procedures/managing-translation-schemas-in-azure.md) - Detailed workflow for schema management
- [Azure Storage Explorer Documentation](https://docs.microsoft.com/en-us/azure/vs-azure-tools-storage-manage-with-storage-explorer) - Official Microsoft documentation
- [Azurite GitHub Repository](https://github.com/Azure/Azurite) - Local Azure Storage emulator
- [Working with Docker](docker.md) - Docker-specific ReportStream documentation
