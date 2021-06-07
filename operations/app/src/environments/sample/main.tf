variable "environment" {
  type = string
}
variable "location" {
  type = string
}

locals {
  resource_group  = "prime-data-hub-${var.environment}"
  resource_prefix = "pdh${var.environment}"
}

terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 2.61.0" # This version must also be changed in other environments
    }
  }

  # NOTE: injected at init time by calling terraform init with the "--backend-config=<path to .tfbackend file>"
  # See configurations directory
  # backend "azurerm" {
  # }
}

output "ENVIRONMENT" {
  value = var.environment
}

output "LOCATION" {
  value = var.location
}

output "RESOURCE_GROUP" {
  value = local.resource_group
}

output "RESOURCE_PREFIX" {
  value = local.resource_prefix
}
