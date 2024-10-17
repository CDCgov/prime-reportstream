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
        postgresUser = {
          value = "${local.env}admin"
        }
        postgresPass = {
          value = random_password.sql_password.result
        }
        Uid = {
          value = var.uid
        }
      }
    }
  }
  cosmosdb_postgresql = {
    dev = {}
  }
  vnets = {
    primary = {
      address_space = {
        value = ["10.5.0.0/16"]
      }
      subnets = {
        service = {
          address_prefixes = {
            value = ["10.5.1.0/24"]
          }
          link_service_policies = true
          endpoint_policies     = false
        }
        endpoint = {
          address_prefixes = {
            value = ["10.5.2.0/24"]
          }
          link_service_policies = false
          endpoint_policies     = true
        }
      }
    }
  }
  dns_zones = {
    postgreshsc = {
      name = "privatelink.postgreshsc.database.azure.com"
      vnet_links = {
        primary = {
          registration_enabled = false
          vnet_key             = "primary"
        }
      }
    }
  }
  private_endpoints = {
    cosmosdb_postgresql = {
      vnet_key             = "primary"
      subnet_key           = "endpoint"
      dns_zone_key         = "privatelink.postgreshsc.database.azure.com"
      resource_id_key      = "dev"
      subresource_names    = ["coordinator"]
      is_manual_connection = false
    }
  }
  storage_accounts = {
    dev = {
      account_tier = "Premium"     //Standard, Premium
      account_kind = "FileStorage" //StorageV2, FileStorage, BlockBlobStorage, BlobStorage
    }
  }
  container_instances = {
    dev = {
      storage_account_key = "dev"
      os_type             = "Linux"
      image               = "mcr.microsoft.com/azure-dev-cli-apps:latest"
      cpu_cores           = 4
      mem_gb              = 16
      commands            = ["/bin/bash", "-c", "sleep infinity"]
      exec                = "/bin/bash"
      shares              = { storage = { mount_path = "/mnt/storage", gb = 1000, tier = "Premium" } } //TransactionOptimized, Premium, Hot, Cool
      repos               = { terraform-templates = { url = "https://github.com/JosiahSiegel/terraform-templates.git", mount_path = "/app/repo1" }, so2pg = { url = "https://github.com/JosiahSiegel/stackoverflow_in_pg.git", mount_path = "/app/repo2" } }
    }
  }
  dev_roles = ["Contributor", "Storage Table Data Contributor", "Storage Blob Data Contributor", "Key Vault Administrator", "Storage File Data Privileged Contributor", "Storage File Data SMB Share Elevated Contributor"]
}
