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
  mssql_instances = {
    primary = {
      databases = ["trialdb", "admindb"]
      epool     = true
    },
    secondary = {
      databases = ["trialdb"]
      epool     = false
    },
    demo = {
      databases = []
      epool     = true
    }
  }
  key_vaults = {
    primary = {
      secrets = {
        SqlUsername = {
          value = "${local.env}admin"
        }
        SqlPassword = {
          value = random_password.sql_password.result
        }
        Uid = {
          value = var.uid
        }
      }
    }
  }
  data_factories = {
    dev = {
      roles = toset(["Storage Blob Data Contributor", "Key Vault Reader", "Key Vault Secrets User"])
    }
    qa = {
      roles = toset(["Key Vault Reader", "Key Vault Secrets User"])
    }
    uat = {
      roles = toset(["Key Vault Reader", "Key Vault Secrets User"])
    }
    prod = {
      roles = toset(["Key Vault Reader", "Key Vault Secrets User"])
    }
  }
  dev_roles = ["Contributor", "Storage Table Data Contributor", "Storage Blob Data Contributor", "Key Vault Administrator"]
}
