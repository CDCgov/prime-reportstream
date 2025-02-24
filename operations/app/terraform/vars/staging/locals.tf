locals {
  environment = "staging"
  address_id  = 6
  init = {
    environment              = local.environment
    location                 = "eastus"
    is_metabase_env          = true
    resource_group_name      = "prime-data-hub-${local.environment}"
    resource_prefix          = "pdh${local.environment}"
    okta_redirect_url        = "https://${local.environment}.prime.cdc.gov/download"
    okta_base_url            = "hhs-prime.oktapreview.com"
    OKTA_scope               = "simple_report_dev"
    RS_okta_redirect_url     = "https://prime-data-hub-XXXXXXX.azurefd.net/download"
    RS_okta_base_url         = "reportstream.oktapreview.com"
    RS_OKTA_scope            = "reportstream_dev"
    storage_queue_name       = ["process", "batch", "batch-poison", "elr-fhir-convert", "process-poison", "send", "send-poison", "elr-fhir-convert", "elr-fhir-convert-poison", "elr-fhir-route", "elr-fhir-translate", "elr-fhir-translate-poison", "process-elr"]
    sftp_container_module    = true
    etor_ti_base_url         = "https://cdcti-stg-api.azurewebsites.net"
    JAVA_OPTS                = "-Dfile.encoding=UTF-8"
    hikari_config_timeout_ms = 60000
  }
  key_vault = {
    app_config_kv_name    = "pdh${local.init.environment}-appconfig"
    application_kv_name   = "pdh${local.init.environment}-keyvault"
    client_config_kv_name = "pdh${local.init.environment}-clientconfig"
    tf_secrets_vault      = "pdh${local.init.environment}-keyvault"
  }
  ad = {
    terraform_object_id       = "a58ee002-62c7-4a91-a2dc-4a837663aa00"
    aad_object_keyvault_admin = "b35a2a63-aeb2-438c-913b-bebeb821adfe"
    aad_group_postgres_admin  = "c4031f1f-229c-4a8a-b3b9-23bae9dbf197"
  }
  security = {
    rsa_key_2048                  = null
    rsa_key_4096                  = "pdh${local.init.environment}-4096-key"
    https_cert_names              = ["${local.init.environment}-prime-cdc-gov", "${local.init.environment}-reportstream-cdc-gov"]
    delete_pii_storage_after_days = 60
  }
  database = {
    db_sku_name         = "GP_Gen5_16"
    db_version          = "11"
    db_storage_mb       = 566272
    db_auto_grow        = true
    db_prevent_destroy  = false
    db_threat_detection = true
    db_replica          = true
  }
  app = {
    app_tier                 = "PremiumV2"
    app_size                 = "P3v2"
    function_runtime_version = "~4"
  }
  chatops = {
    github_repo            = "JosiahSiegel/slack-boltjs-app"
    github_target_branches = "dummy"
  }
  log_analytics_workspace = {
    law_retention_period = "30"
  }
  network = {
    use_cdc_managed_vnet        = true
    dns_vnet                    = "East-vnet"
    dns_ip                      = "172.17.0.135"
    terraform_caller_ip_address = jsondecode(data.azurerm_key_vault_secret.caller_ip_addresses.value)
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
        "network_security_groups" = ["public", "private", "container"]
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
