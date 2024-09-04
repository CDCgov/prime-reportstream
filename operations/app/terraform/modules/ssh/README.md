# SSH Module

This module manages the SSH-related resources for the ReportStream project.

## Purpose

The SSH module is responsible for setting up and configuring SSH access to the ReportStream infrastructure. It creates the necessary resources to enable secure remote access to the environment.

## Key Components

- SSH Public Key
- Azure Key Vault Secret (for storing the SSH public key)

## Usage

To use this module in your Terraform configuration:

1. Reference this module in your main Terraform file:

``` hcl
module "ssh" {
  source = "./modules/ssh"
  
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
}
```

2. Ensure all required variables are provided.

3. Run the Terraform workflow:

``` bash
terraform init
terraform plan
terraform apply
```

## Module Inputs

The following variables are required as inputs:

- `environment`: Target Environment
- `resource_group`: Resource Group Name
- `resource_prefix`: Resource Prefix
- `location`: Function App Location

## Resources Created

- Azure Key Vault Secret: Stores the SSH public key
- Random UUID: Generates a unique identifier for the SSH key

## Outputs

This module provides the following output:

- `ssh_public_key`: The generated SSH public key

## Security Considerations

- The SSH public key is stored securely in Azure Key Vault.
- Access to the Key Vault should be properly restricted and audited.
- Regularly rotate SSH keys as part of security best practices.

## Contributing

When modifying this module:

1. Follow Azure and Terraform best practices for managing SSH access.
2. Ensure any changes align with ReportStream's security requirements.
3. Test all modifications thoroughly in a non-production environment before applying to production.

For more details on ReportStream's infrastructure and security setup, refer to the main project documentation.
