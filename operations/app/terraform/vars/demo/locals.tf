locals {
  random_id   = lower(random_id.init.b64_url)
  is_temp_env = true
  address_id  = var.address_id
  init = {
    environment         = var.environment
    location            = "eastus"
    is_metabase_env     = false
    random_id           = local.random_id
    resource_group_name = "prime-data-hub-${var.environment}"
    resource_prefix     = "pdh${var.environment}"
    okta_redirect_url   = "https://${var.environment}.prime.cdc.gov/download"
    okta_base_url       = "hhs-prime.oktapreview.com"
  }
  key_vault = {
    app_config_kv_name    = "pdh${local.init.environment}-appconfig${local.init.random_id}"
    application_kv_name   = "pdh${local.init.environment}-keyvault${local.init.random_id}"
    client_config_kv_name = "pdh${local.init.environment}-clientconfig${local.init.random_id}"
    tf_secrets_vault      = "pdh${local.init.environment}-keyvault${local.init.random_id}"
  }
  ad = {
    terraform_object_id       = "4d81288c-27a3-4df8-b776-c9da8e688bc7"
    aad_object_keyvault_admin = "3c17896c-ff94-4298-a719-aaac248aa2c8"
    aad_group_postgres_admin  = "f94409a9-12b1-4820-a1b6-e3e0a4fa282d"
  }
  security = {
    rsa_key_2048                  = null
    rsa_key_4096                  = null
    https_cert_names              = ["${local.init.environment}-prime-cdc-gov", "${local.init.environment}-reportstream-cdc-gov"]
    delete_pii_storage_after_days = 30
  }
  database = {
    db_sku_name         = "GP_Gen5_4"
    db_version          = "11"
    db_storage_mb       = 5120
    db_auto_grow        = true
    db_prevent_destroy  = false
    db_threat_detection = false
    db_replica          = true
  }
  app = {
    app_tier                 = "PremiumV2"
    app_size                 = "P2v2"
    function_runtime_version = "~3"
  }
  network = {
    use_cdc_managed_vnet        = true
    dns_vnet                    = "East-vnet"
    dns_ip                      = "168.63.129.16"
    terraform_caller_ip_address = var.terraform_caller_ip_address
    config = {
      "East-vnet" = {
        "address_space"           = "172.17.${local.address_id}.0/25"
        "dns_servers"             = "172.17.0.135"
        "location"                = "East Us"
        "nsg_prefix"              = "eastus-"
        "network_security_groups" = ["private", "public", "container", "endpoint"]
        subnet_nsg_details = {
          public = {
            nsg = "public"
          }
          container = {
            nsg = "public"
          }
          private = {
            nsg = "private"
          }
          endpoint = {
            nsg = "private"
          }
        }
        "subnets" = ["public", "private", "container", "endpoint"]
        subnet_details = {
          public = {
            address_prefix     = "172.17.${local.address_id}.0/28"
            security_group     = "public"
            service_endpoints  = ["Microsoft.ContainerRegistry", "Microsoft.Storage", "Microsoft.Sql", "Microsoft.Web", "Microsoft.KeyVault"]
            service_delegation = ["Microsoft.Web/serverFarms"]
          }
          container = {
            address_prefix     = "172.17.${local.address_id}.16/28"
            security_group     = "public"
            service_endpoints  = ["Microsoft.Storage", "Microsoft.KeyVault"]
            service_delegation = ["Microsoft.ContainerInstance/containerGroups"]
          }
          private = {
            address_prefix     = "172.17.${local.address_id}.32/28"
            security_group     = "private"
            service_endpoints  = ["Microsoft.Storage", "Microsoft.Sql", "Microsoft.KeyVault"]
            service_delegation = ["Microsoft.Web/serverFarms"]
          }
          endpoint = {
            address_prefix     = "172.17.${local.address_id}.64/27"
            security_group     = "private"
            service_endpoints  = ["Microsoft.Storage", "Microsoft.KeyVault"]
            service_delegation = []
          }
        }
        "subnet_cidrs" = [
          {
            name     = "public"
            new_bits = 3
          },
          {
            name     = "container"
            new_bits = 3
          },
          {
            name     = "private"
            new_bits = 3
          },
          {
            name     = "endpoint"
            new_bits = 2
          },
        ]
      },
      "West-vnet" = {
        "address_space"           = "172.17.${local.address_id}.128/25"
        "dns_servers"             = ["172.17.0.135"]
        "location"                = "West Us"
        "subnets"                 = ["public", "private", "container", "endpoint"]
        "nsg_prefix"              = "westus-"
        "network_security_groups" = ["private", "public", "container", "endpoint"]
        subnet_nsg_details = {
          public = {
            nsg = "public"
          }
          container = {
            nsg = "public"
          }
          private = {
            nsg = "private"
          }
          endpoint = {
            nsg = "private"
          }
        }
        subnet_details = {
          public = {
            address_prefix     = "172.17.${local.address_id}.128/28"
            security_group     = "public"
            service_endpoints  = ["Microsoft.ContainerRegistry", "Microsoft.Storage", "Microsoft.Sql", "Microsoft.Web", "Microsoft.KeyVault"]
            service_delegation = ["Microsoft.Web/serverFarms"]
          }
          container = {
            address_prefix     = "172.17.${local.address_id}.144/28"
            security_group     = "public"
            service_endpoints  = ["Microsoft.Storage", "Microsoft.KeyVault"]
            service_delegation = ["Microsoft.ContainerInstance/containerGroups"]
          }
          private = {
            address_prefix     = "172.17.${local.address_id}.160/28"
            security_group     = "private"
            service_endpoints  = ["Microsoft.Storage", "Microsoft.Sql", "Microsoft.KeyVault"]
            service_delegation = ["Microsoft.Web/serverFarms"]
          }
          endpoint = {
            address_prefix     = "172.17.${local.address_id}.192/27"
            security_group     = "private"
            service_endpoints  = ["Microsoft.Storage", "Microsoft.KeyVault"]
            service_delegation = []
          }
        }
        "subnet_cidrs" = [
          {
            name     = "public"
            new_bits = 3
          },
          {
            name     = "container"
            new_bits = 3
          },
          {
            name     = "private"
            new_bits = 3
          },
          {
            name     = "endpoint"
            new_bits = 2
          },
        ]
      },
      "vnet" = {
        "address_space" = "10.0.0.0/16"
        "dns_server"    = [""]
        "location"      = "East Us"
        "subnets"       = ["public", "private", "container", "endpoint", "GatewaySubnet"]
        subnet_details = {
          public = {
            address_prefix     = "10.0.1.0/24"
            security_group     = "public"
            service_endpoints  = ["Microsoft.ContainerRegistry", "Microsoft.Storage", "Microsoft.Sql", "Microsoft.Web", "Microsoft.KeyVault"]
            service_delegation = ["Microsoft.Web/serverFarms"]
          }
          container = {
            address_prefix     = "10.0.2.0/24"
            security_group     = "public"
            service_endpoints  = ["Microsoft.Storage", "Microsoft.KeyVault"]
            service_delegation = ["Microsoft.ContainerInstance/containerGroups"]
          }
          private = {
            address_prefix     = "10.0.3.0/24"
            security_group     = "private"
            service_endpoints  = ["Microsoft.Storage", "Microsoft.Sql", "Microsoft.KeyVault"]
            service_delegation = ["Microsoft.Web/serverFarms"]
          }
          endpoint = {
            address_prefix     = "10.0.5.0/24"
            security_group     = ""
            service_endpoints  = ["Microsoft.Storage", "Microsoft.KeyVault"]
            service_delegation = []
          }
          GatewaySubnet = {
            address_prefix     = "10.0.4.0/24"
            security_group     = ""
            service_endpoints  = []
            service_delegation = []
          }
        }
        "nsg_prefix"              = ""
        "network_security_groups" = ["private", "public", "container"]
        subnet_nsg_details = {
          public = {
            nsg = "public"
          }
          container = {
            nsg = "public"
          }
          private = {
            nsg = "private"
          }
        }
        "subnet_cidrs" = [
          {
            name     = "GatewaySubnet"
            new_bits = 8
          },
          {
            name     = "public"
            new_bits = 8
          },
          {
            name     = "container"
            new_bits = 8
          },
          {
            name     = "private"
            new_bits = 8
          },
          {
            name     = "unused"
            new_bits = 8
          },
          {
            name     = "endpoint"
            new_bits = 8
          }
        ]
      },
      "vnet-peer" = {
        "address_space" = "10.1.0.0/16"
        "dns_servers"   = [""]
        "location"      = "West Us"
        "subnets"       = ["private", "endpoint"]
        subnet_details = {
          private = {
            address_prefix     = "10.1.3.0/24"
            security_group     = ""
            service_endpoints  = ["Microsoft.Storage", "Microsoft.Sql", "Microsoft.KeyVault"]
            service_delegation = ["Microsoft.Web/serverFarms"]
          }
          endpoint = {
            address_prefix     = "10.1.5.0/24"
            security_group     = ""
            service_endpoints  = ["Microsoft.Storage", "Microsoft.KeyVault"]
            service_delegation = []
          }
        }
        "nsg_prefix"              = ""
        "network_security_groups" = [""]
        subnet_nsg_details = {
        }
        "subnet_cidrs" = [
          {
            name     = "public"
            new_bits = 3
          },
          {
            name     = "container"
            new_bits = 3
          },
          {
            name     = "private"
            new_bits = 3
          },
          {
            name     = "endpoint"
            new_bits = 2
          }
        ]
      }
    }
  }
}
