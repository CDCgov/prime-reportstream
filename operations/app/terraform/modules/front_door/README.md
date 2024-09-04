# Front Door Module

This module sets up Azure Front Door for the ReportStream project, providing global load balancing and a fast, secure content delivery network.

## Purpose

The front_door module configures Azure Front Door to manage incoming traffic, enhance performance, and improve security for the ReportStream application.

## Key Components

- Azure Front Door
- Front Door WAF Policy
- Custom domains and SSL certificates
- Routing rules and backend pools

## Usage

To use this module in your Terraform configuration:

1. Reference this module in your main Terraform file:

``` hcl
module "front_door" {
  source = "./front_door"
  
  environment               = var.environment
  resource_group            = var.resource_group
  resource_prefix           = var.resource_prefix
  location                  = var.location
  https_cert_names          = var.https_cert_names
  is_metabase_env           = var.is_metabase_env
  public_primary_web_endpoint = var.public_primary_web_endpoint
  application_key_vault_id  = var.application_key_vault_id
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

- `environment`: Target Environment
- `resource_group`: Resource Group Name
- `resource_prefix`: Resource Prefix
- `location`: Function App Location
- `https_cert_names`: List of HTTPS cert names to associate with the front door
- `is_metabase_env`: Boolean indicating if Metabase should be deployed in this environment
- `public_primary_web_endpoint`: Public primary web endpoint
- `application_key_vault_id`: ID of the application Key Vault

## Key Features

- Sets up Azure Front Door with appropriate routing rules and backend pools
- Configures Web Application Firewall (WAF) policy for enhanced security
- Manages custom domains and SSL certificates
- Integrates with Azure Key Vault for secure certificate management

## Outputs

The module provides several outputs, including:

- Front Door ID
- Front Door Endpoint
- WAF Policy ID

## Security Considerations

- WAF policy is configured to protect against common web vulnerabilities
- SSL certificates are managed securely through Azure Key Vault
- Custom routing rules ensure traffic is directed appropriately

## Contributing

When modifying this module:

1. Follow Azure Front Door best practices and Terraform conventions
2. Ensure changes align with ReportStream's architecture and security requirements
3. Test thoroughly in a non-production environment before applying to production

For more details on ReportStream's infrastructure, refer to the main project documentation.
