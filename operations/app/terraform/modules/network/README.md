# Network Module

This module manages the network infrastructure for the ReportStream project.

## Purpose

The network module is responsible for provisioning and configuring the core networking components required by ReportStream. It sets up Azure Virtual Networks, subnets, and related resources to support the application's networking needs.

## Key Components

- Azure Virtual Network
- Subnets
- Network Security Groups
- Route Tables

## Usage

To use this module in your Terraform configuration:

1. Reference this module in your main Terraform file:

``` hcl
module "network" {
  source = "./network"
  
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
  azure_vns       = var.azure_vns
}
```

2. Provide all necessary variables as defined in the `~input.tf` file.

3. Execute the Terraform workflow:

``` bash
terraform init
terraform plan
terraform apply
```

## Module Inputs

The following inputs are required for this module:

- `environment`: Target Environment
- `resource_group`: Resource Group Name
- `resource_prefix`: Resource Prefix
- `location`: Network Location
- `azure_vns`: Azure Virtual Network configuration

## Resources Created

This module creates the following resources:

- Azure Virtual Network
- Subnets for various purposes (e.g., app, data, mgmt)
- Network Security Groups associated with subnets
- Route Tables for custom routing

## Outputs

The module provides several useful outputs, including:

- Virtual Network ID
- Subnet IDs
- Network Security Group IDs
- Route Table IDs

## Network Configuration

- The Virtual Network is created with a CIDR block defined in the `azure_vns` variable.
- Multiple subnets are created within the VNet for different purposes.
- Network Security Groups are associated with subnets to control inbound and outbound traffic.
- Route Tables are set up for custom routing requirements.

## Contributing

When working on this module:

1. Follow Azure networking best practices and Terraform standards.
2. Ensure any changes align with ReportStream's network architecture and security requirements.
3. Test all modifications thoroughly in a non-production environment before applying to production.

For more information on ReportStream's network infrastructure, consult the main project documentation.
