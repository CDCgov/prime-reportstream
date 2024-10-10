#!/bin/bash

set -e

# Configuration
DEFAULT_TEMPLATE_ENVIRONMENT="01"
DEFAULT_RESOURCE_GROUP_NAME="myRG"
DEFAULT_ENVIRONMENT_NICKNAME="demo"
DEFAULT_LOCATION="eastus"
DEFAULT_OWNER_EMAIL="owner@example.com"
CONTAINER_NAME="terraformstate"
STATE_FILE_KEY="terraform.tfstate"
USE_AZUREAD_AUTH=false

parse_arguments() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --template-environment|-t)
        template_environment="$2"
        shift 2
        ;;
      --resource-group|-r)
        resource_group_name="$2"
        shift 2
        ;;
      --environment-nickname|-e)
        environment_nickname="$2"
        shift 2
        ;;
      --location|-l)
        location="$2"
        shift 2
        ;;
      --owner-email|-o)
        owner_email="$2"
        shift 2
        ;;
      *)
        echo "Unknown argument: $1"
        exit 1
        ;;
    esac
  done
}

# Parse command-line arguments
parse_arguments "$@"

# Function to prompt for user input with a default value
prompt_with_default() {
  local prompt=$1
  local default_value=$2
  read -p "$prompt [$default_value]: " value
  echo "${value:-$default_value}"
}

# Function to check if a directory exists
check_directory_exists() {
  local directory=$1
  if [ ! -d "$directory" ]; then
    echo "Error: The specified template directory '$directory' does not exist."
    exit 1
  fi
}

# Function to check if a resource group exists
check_resource_group_exists() {
  local resource_group=$1
  if ! az group show --name "$resource_group" >/dev/null 2>&1; then
    echo "Error: The specified resource group '$resource_group' does not exist."
    echo "Please create the resource group or provide an existing one."
    exit 1
  fi
}

# Function to generate the _override.tf.json file
generate_override_file() {
  local json_file="azure/env/$template_environment/_override.tf.json"
  cat > "$json_file" <<EOL
{
  "terraform": {
    "backend": {
      "azurerm": {
        "resource_group_name": "$resource_group_name",
        "storage_account_name": "$storage_account_name",
        "container_name": "$CONTAINER_NAME",
        "key": "$STATE_FILE_KEY",
        "use_azuread_auth": $USE_AZUREAD_AUTH
      }
    }
  },
  "variable": {
    "environment": {
      "default": "$environment_nickname"
    },
    "uid": {
      "default": "$random_suffix"
    },
    "location": {
      "default": "$location"
    },
    "owner_email": {
      "default": "$owner_email"
    },
    "resource_group": {
      "default": "$resource_group_name"
    },
    "owner_object_id": {
      "default": "$owner_object_id"
    }
  }
}
EOL

  if [ ! -f "$json_file" ]; then
    echo "Error: The JSON file '$json_file' was not created!"
    exit 1
  fi
}

# Function to create the storage account
create_storage_account() {
  if ! az storage account create --allow-blob-public-access false --name "$storage_account_name" --resource-group "$resource_group_name" --location "$location" --sku 'Standard_LRS' -o 'none'; then
    echo -e "Error: Failed to create the storage account '$storage_account_name'.\n"
    echo "Tips to resolve:"
    echo "- Re-authenticate with 'az login'."
    echo "- Ensure that the resource group '$resource_group_name' exists and you have the necessary permissions to create resources in it."
    echo "- Verify that the storage account name '$storage_account_name' is unique and meets the naming requirements."
    echo "- Check if the location '$location' is valid and supported for storage account creation."
    echo "- Make sure you have the required Azure CLI version and are authenticated with the correct Azure subscription."
    exit 1
  fi
}

# Function to create the storage container
create_storage_container() {
  if ! az storage container create --account-name "$storage_account_name" -n "$CONTAINER_NAME" --only-show-errors -o 'none'; then
    echo -e "Error: Failed to create the storage container '$CONTAINER_NAME' in the storage account '$storage_account_name'.\n"
    echo "Tips to resolve:"
    echo "- Re-authenticate with 'az login'."
    echo "- Ensure that the storage account '$storage_account_name' exists and you have the necessary permissions to create containers in it."
    echo "- Verify that the container name '$CONTAINER_NAME' is valid and meets the naming requirements."
    echo "- Check if you have the required Azure CLI version and are authenticated with the correct Azure subscription."
    echo "- If error 'SubscriptionNotFound', manually create a single resource (other than a Resource Group) if non-exist."
    exit 1
  fi
}

# Main script logic
# Check if owner_object_id is populated, fail the script if it is empty
if [ -z "$owner_object_id" ]; then
  echo "Error: owner_object_id is empty. Please make sure it is populated before running the script."
  exit 1
fi
echo -e "Owner object ID: $owner_object_id\n"

# Prompt for user inputs

# Check if the template environment directory exists
if [ -z "$template_environment" ]; then
  template_environment=$(prompt_with_default "Enter the template environment" "$DEFAULT_TEMPLATE_ENVIRONMENT")
fi
check_directory_exists "azure/env/$template_environment"
export template_path="azure/env/$template_environment"
echo -e "Using template path: $template_path\n"

# Check if the resource group exists
if [ -z "$resource_group_name" ]; then
  resource_group_name=$(prompt_with_default "Enter the resource group name" "$DEFAULT_RESOURCE_GROUP_NAME")
fi
check_resource_group_exists "$resource_group_name"

if [ -z "$environment_nickname" ]; then
  environment_nickname=$(prompt_with_default "Enter the environment nickname" "$DEFAULT_ENVIRONMENT_NICKNAME")
fi
environment_nickname=$(echo "$environment_nickname" | tr '[:upper:]' '[:lower:]' | tr -cd '[:alnum:]')

if [ -z "$location" ]; then
  location=$(prompt_with_default "Enter the location" "$DEFAULT_LOCATION")
fi

if [ -z "$owner_email" ]; then
  owner_email=$(prompt_with_default "Enter the owner email" "$DEFAULT_OWNER_EMAIL")
fi

# Generate a random alphanumeric string of length 3
random_suffix=$(echo $(date +%s%N) | sha256sum | tr '[:upper:]' '[:lower:]' | head -c 3)

# Set the storage account name
storage_account_name="sa${environment_nickname}${random_suffix}"
echo -e "Using storage account name: $storage_account_name\n"

# Generate the _override.tf.json file
generate_override_file

# Create the storage account
create_storage_account

# Wait for the storage account to be fully provisioned
sleep 5

# Create the storage container
create_storage_container
