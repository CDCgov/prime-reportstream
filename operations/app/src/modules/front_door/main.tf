terraform {
    required_version = ">= 0.14"
}

locals {
    name = var.environment != "dev" ? "prime-data-hub-${var.environment}" : "prime-data-hub-${var.resource_prefix}"
    functionapp_address = "${var.resource_prefix}-functionapp.azurewebsites.net"
    metabase_address = "${var.resource_prefix}-metabase.azurewebsites.net"
    prod_condition = var.environment == "prod" ? [1] : []
    https_cert_secret_name = "prime-cdc-gov"
}

// TODO: Terraform does not support Azure's rules engine yet
// We have an HSTS rule that must be manually configured
// Ticket tracking rules engine in Terraform: https://github.com/terraform-providers/terraform-provider-azurerm/issues/7455

resource "azurerm_frontdoor" "front_door" {
    name = local.name
    resource_group_name = var.resource_group
    enforce_backend_pools_certificate_name_check = true
    backend_pools_send_receive_timeout_seconds = 30
    friendly_name = local.name

    backend_pool_load_balancing {
        name = "functionsLoadBalancingSettings"
        sample_size = 4
        successful_samples_required = 2
        additional_latency_milliseconds = 0
    }

    backend_pool_health_probe {
        name = "functionsHealthProbeSettings"
        path = "/"
        interval_in_seconds = 30
        protocol = "Https"
        probe_method = "HEAD"
    }

    backend_pool {
        name = "functions"
        health_probe_name = "functionsHealthProbeSettings"
        load_balancing_name = "functionsLoadBalancingSettings"

        backend {
            address = local.functionapp_address
            host_header = local.functionapp_address
            http_port = 80
            https_port = 443
        }
    }

    backend_pool_load_balancing {
        name = "metabaseLoadBalancingSettings"
        sample_size = 4
        successful_samples_required = 2
        additional_latency_milliseconds = 0
    }

    backend_pool_health_probe {
        name = "metabaseHealthProbeSettings"
        path = "/"
        interval_in_seconds = 30
        protocol = "Https"
        probe_method = "HEAD"
    }

    backend_pool {
        name = "metabase"
        health_probe_name = "metabaseHealthProbeSettings"
        load_balancing_name = "metabaseLoadBalancingSettings"

        backend {
            address = local.metabase_address
            host_header = local.metabase_address
            http_port = 80
            https_port = 443
        }
    }

    frontend_endpoint {
        name = "DefaultFrontendEndpoint"
        host_name = "${local.name}.azurefd.net"
        custom_https_provisioning_enabled = false
    }

    dynamic "frontend_endpoint" {
        for_each = local.prod_condition
        content {
            name = "prime-cdc-gov"
            host_name = "prime.cdc.gov"
            custom_https_provisioning_enabled = true
            custom_https_configuration {
                certificate_source = "AzureKeyVault"
                azure_key_vault_certificate_secret_name = local.https_cert_secret_name
                azure_key_vault_certificate_secret_version = data.azurerm_key_vault_secret.https_cert.version
                azure_key_vault_certificate_vault_id = var.key_vault_id
            }
        }
    }

    routing_rule {
        name = "HttpToHttpsRedirect"
        frontend_endpoints = ["DefaultFrontendEndpoint"]
        accepted_protocols = ["Http"]
        patterns_to_match = [
            "/",
            "/*",
            "/api/*",
            "/download",
            "/metabase",
            "/metabase/*"
        ]

        redirect_configuration {
            redirect_protocol = "HttpsOnly"
            redirect_type = "Moved"
        }
    }

    routing_rule {
        name = "download"
        frontend_endpoints = ["DefaultFrontendEndpoint"]
        accepted_protocols = ["Https"]
        patterns_to_match = ["/", "/download"]

        forwarding_configuration {
            backend_pool_name = "functions"
            forwarding_protocol = "HttpsOnly"
            custom_forwarding_path = "/api/download"
        }
    }

    routing_rule {
        name = "api"
        frontend_endpoints = ["DefaultFrontendEndpoint"]
        accepted_protocols = ["Https"]
        patterns_to_match = ["/*", "/api/*"]

        forwarding_configuration {
            backend_pool_name = "functions"
            forwarding_protocol = "HttpsOnly"
        }
    }

    routing_rule {
        name = "metabase"
        frontend_endpoints = ["DefaultFrontendEndpoint"]
        accepted_protocols = ["Https"]
        patterns_to_match = ["/metabase", "/metabase/*"]

        forwarding_configuration {
            backend_pool_name = "metabase"
            forwarding_protocol = "HttpsOnly"
            custom_forwarding_path = "/"
        }
    }
}

module "frontdoor_access_log_event_hub_log" {
    source = "../event_hub_log"
    resource_type = "front_door"
    log_type = "access"
    eventhub_namespace_name = var.eventhub_namespace_name
    resource_group = var.resource_group
    resource_prefix = var.resource_prefix
}

resource "azurerm_monitor_diagnostic_setting" "frontdoor_access_log" {
  name = "${var.resource_prefix}-front_door-access-log"
  target_resource_id = azurerm_frontdoor.front_door.id
  eventhub_name = module.frontdoor_access_log_event_hub_log.event_hub_name
  eventhub_authorization_rule_id = module.frontdoor_access_log_event_hub_log.event_hub_resource_auth_rule_id

  log {
    category = "FrontdoorAccessLog"
    enabled  = true

    retention_policy {
      enabled = false
    }
  }
}

module "frontdoor_waf_log_event_hub_log" {
    source = "../event_hub_log"
    resource_type = "front_door"
    log_type = "waf"
    eventhub_namespace_name = var.eventhub_namespace_name
    resource_group = var.resource_group
    resource_prefix = var.resource_prefix
}

resource "azurerm_monitor_diagnostic_setting" "frontdoor_waf_log" {
  name = "${var.resource_prefix}-front_door-waf-log"
  target_resource_id = azurerm_frontdoor.front_door.id
  eventhub_name = module.frontdoor_waf_log_event_hub_log.event_hub_name
  eventhub_authorization_rule_id = module.frontdoor_waf_log_event_hub_log.event_hub_resource_auth_rule_id

  log {
    category = "FrontdoorWebApplicationFirewallLog"
    enabled  = true

    retention_policy {
      enabled = false
    }
  }
}

data "azurerm_key_vault_secret" "https_cert" {
    count = (var.environment == "prod" ? 1 : 0)
    key_vault_id = var.key_vault_id
    name = local.https_cert_secret_name
}

output "id" {
    value = azurerm_frontdoor.front_door.id
}

output "cname" {
    value = azurerm_frontdoor.front_door.cname
}
