locals {
  environment = "prod"
  address_id  = 7
  init = {
    environment         = local.environment
    location            = "eastus"
    is_metabase_env     = true
    resource_group_name = "prime-data-hub-${local.environment}"
    resource_prefix     = "pdh${local.environment}"
    okta_redirect_url   = "https://prime.cdc.gov/download"
    okta_base_url       = "hhs-prime.okta.com"
  }
  key_vault = {
    app_config_kv_name    = "pdh${local.init.environment}-appconfig"
    application_kv_name   = "pdh${local.init.environment}-keyvault"
    client_config_kv_name = "pdh${local.init.environment}-clientconfig"
    tf_secrets_vault      = "pdh${local.init.environment}-keyvault"
  }
  ad = {
    terraform_object_id       = "4d81288c-27a3-4df8-b776-c9da8e688bc7"
    aad_object_keyvault_admin = "5c6a951e-a4c2-4890-b62c-0ed8179501bb"
    aad_group_postgres_admin  = "c4031f1f-229c-4a8a-b3b9-23bae9dbf197"
  }
  security = {
    rsa_key_2048                  = null
    rsa_key_4096                  = null
    https_cert_names              = ["prime-cdc-gov", "reportstream-cdc-gov"]
    delete_pii_storage_after_days = 60
  }
  database = {
    db_sku_name         = "GP_Gen5_16"
    db_version          = "11"
    db_storage_mb       = 5120
    db_auto_grow        = true
    db_prevent_destroy  = true
    db_threat_detection = true
    db_replica          = true
  }
  app = {
    app_tier                 = "PremiumV2"
    app_size                 = "P3v2"
    function_runtime_version = "~4"
  }
  network = {
    use_cdc_managed_vnet        = true
    dns_vnet                    = "East-vnet"
    dns_ip                      = "172.17.0.135"
    terraform_caller_ip_address = ["162.224.209.174", "24.163.118.70", "75.191.122.59", "108.48.23.191"]
    config = {
      "East-vnet" = {
        "address_space"           = "172.17.${local.address_id}.0/25"
        "dns_servers"             = ["172.17.0.135"]
        "location"                = "East Us"
        "nsg_prefix"              = "eastus-"
        "network_security_groups" = ["private", "public", "container", "endpoint"]
        "subnets"                 = ["public", "private", "container", "endpoint"]
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
        "address_space"           = "10.0.0.0/16"
        "dns_server"              = [""]
        "location"                = "East Us"
        "subnets"                 = ["public", "private", "container", "endpoint", "GatewaySubnet"]
        "nsg_prefix"              = ""
        "network_security_groups" = ["private", "public", "container"]
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
        "address_space"           = "10.1.0.0/16"
        "dns_servers"             = [""]
        "location"                = "West Us"
        "subnets"                 = ["private", "endpoint"]
        "nsg_prefix"              = ""
        "network_security_groups" = [""]
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
      }
    }
  }
}
