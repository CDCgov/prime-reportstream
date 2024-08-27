locals {
  environment = "tst"
  init = {
    environment           = local.environment
    location              = "eastus"
    is_metabase_env       = true
    resource_group_name   = "ophdst-prim-tst-moderate-rest-rg"
    resource_prefix       = "pdh${local.environment}"
    okta_redirect_url     = "https://${local.environment}.prime.cdc.gov/download"
    okta_base_url         = "hhs-prime.oktapreview.com"
    OKTA_scope            = "simple_report_dev"
    RS_okta_redirect_url  = "https://prime-data-hub-XXXXXXX.azurefd.net/download"
    RS_okta_base_url      = "reportstream.oktapreview.com"
    RS_OKTA_scope         = "reportstream_dev"
    storage_queue_name    = ["process", "batch", "batch-poison", "elr-fhir-convert", "process-poison", "send", "send-poison", "elr-fhir-convert", "elr-fhir-convert-poison", "elr-fhir-route", "elr-fhir-translate", "elr-fhir-translate-poison", "process-elr"]
    sftp_container_module = true,
    etor_ti_base_url      = "https://cdcti-prd-api.azurewebsites.net"
  }
  key_vault = {
    app_config_kv_name    = "pdh${local.init.environment}-appconfig"
    application_kv_name   = "pdh${local.init.environment}-keyvault"
    client_config_kv_name = "pdh${local.init.environment}-clientconfig"
    tf_secrets_vault      = "pdh${local.init.environment}-keyvault"
  }
  ad = {
    terraform_object_id       = "4d81288c-27a3-4df8-b776-c9da8e688bc7"
    aad_object_keyvault_admin = "b35a2a63-aeb2-438c-913b-bebeb821adfe"
    aad_group_postgres_admin  = "c4031f1f-229c-4a8a-b3b9-23bae9dbf197"
  }
  security = {
    rsa_key_2048                  = null
    rsa_key_4096                  = null
    https_cert_names              = [] # ["${local.init.environment}-prime-cdc-gov", "${local.init.environment}-reportstream-cdc-gov"]
    delete_pii_storage_after_days = 60
  }
  database = {
    db_sku_name         = "GP_Standard_D4s_v3"
    db_version          = "14"
    db_storage_mb       = 524288
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
    github_repo            = "JosiahSiegel/hubot-slack-docker"
    github_target_branches = "temp1,demo1,demo2"
  }
  log_analytics_workspace = {
    law_retention_period = "30"
  }
  network = {
    use_cdc_managed_vnet        = true
    dns_vnet                    = "ophdst-prim-tst-moderate-rest-app-vnet"
    dns_ip                      = "172.17.0.135"
    terraform_caller_ip_address = jsondecode(data.azurerm_key_vault_secret.caller_ip_addresses.value)
    config = {
      "app-vnet" = {
        "name" = "ophdst-prim-tst-moderate-rest-app-vnet"
        "address_space"           = "172.18.211.64/26"
        "dns_servers"             = ["172.17.0.135"]
        "location"                = "East Us"
        "nsg_prefix"              = "ophdst-prim-"
        "network_security_groups" = ["ophdst-prim-tst-moderate-rest-default-sg"]
        "subnets"                 = ["ophdst-prim-tst-moderate-rest-app-subnet", "ophdst-prim-tst-moderate-container-subnet"]
      },
      "db-vnet" = {
            "name" = "ophdst-prim-tst-moderate-rest-db-vnet"
            "address_space"           = "172.18.211.32/27"
            "dns_servers"             = ["172.17.0.135"]
            "location"                = "East Us"
            "nsg_prefix"              = "ophdst-prim-"
            "network_security_groups" = ["ophdst-prim-tst-moderate-rest-default-sg"]
            "subnets"                 = ["ophdst-prim-tst-moderate-rest-db-subnet"]
        }
    }
  }
}
