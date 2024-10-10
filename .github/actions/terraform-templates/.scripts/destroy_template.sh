#!/bin/bash

set -e

# Function to check if a directory exists
check_directory() {
  if [ ! -d "$1" ]; then
    echo "Error: The specified template directory '$1' does not exist."
    exit 1
  fi
}

# Function to check if a file exists
check_file() {
  if [ ! -f "$1" ]; then
    echo "Error: The file '$1' does not exist."
    exit 1
  fi
}

# Function to check if a value is empty
check_empty() {
  if [ -z "$1" ]; then
    echo "Error: No value provided for $2. Please enter a valid value."
    exit 1
  fi
}

# Prompt for the path to the environment directory
read -p "Enter the template environment (01): " env_path

# Check if env_path is empty
check_empty "$env_path" "env_path"

# Set the environment directory path
env_dir="azure/env/$env_path"

# Check if the environment directory exists
check_directory "$env_dir"

# Set the JSON file path
json_file="$env_dir/_override.tf.json"

# Check if the JSON file exists
check_file "$json_file"

# Read values from the JSON file
storage_account_name=$(grep -oP '(?<="storage_account_name": ")[^"]*' "$json_file")
resource_group_name=$(grep -oP '(?<="resource_group_name": ")[^"]*' "$json_file")
container_name=$(grep -oP '(?<="container_name": ")[^"]*' "$json_file")
state_file_key=$(grep -oP '(?<="key": ")[^"]*' "$json_file")

# Check if the storage account exists
if ! az storage account show --name "$storage_account_name" --resource-group "$resource_group_name" >/dev/null 2>&1; then
  echo "Storage account '$storage_account_name' does not exist. Skipping all actions."
  exit 0
fi

# Check if the container exists
if ! az storage container exists --account-name "$storage_account_name" --name "$container_name" --query "exists" -o tsv --only-show-errors | grep -q "^true$"; then
  echo "Container '$container_name' does not exist in storage account '$storage_account_name'. Skipping Terraform destroy."
else
  # Check if the state file exists in the container
  if az storage blob exists --account-name "$storage_account_name" --container-name "$container_name" --name "$state_file_key" --query "exists" -o tsv --only-show-errors | grep -q "^true$"; then
    # Run terraform destroy if the state file exists
    terraform -chdir="$env_dir" destroy -auto-approve
  else
    echo "Skipping terraform destroy as the state file does not exist in the container."
  fi
fi

# Construct the az storage account delete command
delete_command="az storage account delete --name $storage_account_name --resource-group $resource_group_name --yes"

# Print the command
echo "Running command: $delete_command"

# Run the command
eval "$delete_command"
