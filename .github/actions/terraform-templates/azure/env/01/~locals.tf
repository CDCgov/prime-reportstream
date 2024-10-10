variable "environment" {}
variable "owner_object_id" {}
variable "owner_email" {}
variable "uid" {}
variable "location" {}
variable "resource_group" {}

locals {
  env = var.environment
  common = {
    env            = local.env
    uid            = var.uid
    subscription   = data.azurerm_subscription.default
    tenant_id      = data.azurerm_subscription.default.tenant_id
    location       = var.location
    resource_group = data.azurerm_resource_group.default
    owner_email    = var.owner_email
    owner          = data.azuread_users.owner.users[0]
  }
  key_vaults = {
    primary = {
      secrets = {
        "${local.env}-SECRET" = {
          value = "PLACEHOLDER_${local.env}-SECRET"
        }
        "${local.env}-KEY" = {
          value = "PLACEHOLDER_${local.env}-KEY"
        }
        "${local.env}-API-ENDPOINT" = {
          value = "PLACEHOLDER_${local.env}-API-ENDPOINT"
        }
        "${local.env}-API-URL" = {
          value = "PLACEHOLDER_${local.env}-API-URL"
        }
      }
    }
  }
  dev_roles = ["Contributor", "Storage Table Data Contributor", "Storage Blob Data Contributor", "Key Vault Administrator"]
}
