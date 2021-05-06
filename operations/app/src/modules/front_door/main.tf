terraform {
    required_version = ">= 0.14"
}

locals {
    name = var.environment != "dev" ? "prime-data-hub-${var.environment}" : "prime-data-hub-${var.resource_prefix}"
    functionapp_address = "${var.resource_prefix}-functionapp.azurewebsites.net"
    metabase_address = (var.environment == "test" || var.environment == "prod" ? "${var.resource_prefix}-metabase.azurewebsites.net" : null)
    frontend_endpoints = (length(var.https_cert_names) > 0) ? concat(["DefaultFrontendEndpoint"], var.https_cert_names) : ["DefaultFrontendEndpoint"]
}

// TODO: Terraform does not support Azure's rules engine yet
// We have an HSTS rule that must be manually configured
// Ticket tracking rules engine in Terraform: https://github.com/terraform-providers/terraform-provider-azurerm/issues/7455

resource "azurerm_frontdoor" "front_door" {
    name = local.name
    resource_group_name = var.resource_group
    enforce_backend_pools_certificate_name_check = true
    backend_pools_send_receive_timeout_seconds = 90
    friendly_name = local.name

    backend_pool_load_balancing {
      name = "functionsLoadBalancingSettings"
      sample_size = 4
      successful_samples_required = 2
      additional_latency_milliseconds = 0
    }

    dynamic "backend_pool_load_balancing" {
      for_each = (var.environment == "test" || var.environment == "prod" ? [1] : [])
      content {
        name = "metabaseLoadBalancingSettings"
        sample_size = 4
        successful_samples_required = 2
        additional_latency_milliseconds = 0
      }
    }

    dynamic "backend_pool_health_probe" {
        for_each = (var.environment == "test" || var.environment == "prod" ? [1] : [])
        content {
          name = "metabaseHealthProbeSettings"
          path = "/"
          interval_in_seconds = 30
          protocol = "Https"
          probe_method = "HEAD"
        }
    }

    dynamic "backend_pool" {
      for_each = (var.environment == "test" || var.environment == "prod" ? [1] : [])
      content {
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

    frontend_endpoint {
        name = "DefaultFrontendEndpoint"
        host_name = "${local.name}.azurefd.net"
        custom_https_provisioning_enabled = false
    }

    dynamic "frontend_endpoint" {
        for_each = var.https_cert_names
        content {
          name = frontend_endpoint.value
          host_name = replace(frontend_endpoint.value, "-", ".") // This will change test-prime-cdc-gov to test.prime.cdc.gov
          custom_https_provisioning_enabled = true
          
          custom_https_configuration {
            certificate_source = "AzureKeyVault"
            azure_key_vault_certificate_secret_name = frontend_endpoint.value
            azure_key_vault_certificate_secret_version = "Latest"
            azure_key_vault_certificate_vault_id = var.key_vault_id
          }
        }
      }

    routing_rule {
        name = "HttpToHttpsRedirect"
        frontend_endpoints = local.frontend_endpoints
        accepted_protocols = ["Http"]
        patterns_to_match = (var.environment == "test" || var.environment == "prod" ? ["/", "/*", "/api/*", "/download", "/metabase", "/metabase/*"] : ["/", "/*", "/api/*", "/download"])

        redirect_configuration {
            redirect_protocol = "HttpsOnly"
            redirect_type = "Moved"
        }
    }

    routing_rule {
        name = "download"
        frontend_endpoints = local.frontend_endpoints
        accepted_protocols = ["Https"]
        patterns_to_match = ["/", "/download"]

        forwarding_configuration {
            backend_pool_name = "functions"
            forwarding_protocol = "HttpsOnly"
            custom_forwarding_path = "/api/download"
        }
    }

    dynamic "routing_rule" {
      for_each = (var.environment == "test" || var.environment == "prod" ? [1] : [])
      content {
        name = "metabase"
        frontend_endpoints = local.frontend_endpoints
        accepted_protocols = ["Https"]
        patterns_to_match = ["/metabase", "/metabase/*"]

        forwarding_configuration {
          backend_pool_name = "metabase"
          forwarding_protocol = "HttpsOnly"
          custom_forwarding_path = "/"
        }
      }
    }

    routing_rule {
        name = "api"
        frontend_endpoints = local.frontend_endpoints
        accepted_protocols = ["Https"]
        patterns_to_match = ["/*", "/api/*"]

        forwarding_configuration {
            backend_pool_name = "functions"
            forwarding_protocol = "HttpsOnly"
        }
    }

  lifecycle {
    ignore_changes = [
      # The Azure endpoint does not support reconfiguring HTTPS profiles with latest at this time
      frontend_endpoint[0].custom_https_configuration,
      frontend_endpoint[1].custom_https_configuration,
      frontend_endpoint[2].custom_https_configuration
    ]
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
  eventhub_authorization_rule_id = var.eventhub_manage_auth_rule_id

  log {
    category = "FrontdoorAccessLog"
    enabled  = true

    retention_policy {
      days = 0
      enabled = false
    }
  }

  log {
    category = "FrontdoorWebApplicationFirewallLog"
    enabled  = false

    retention_policy {
      days = 0
      enabled = false
    }
  }

  metric {
    category = "AllMetrics"
    enabled = false

    retention_policy {
        days = 0
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
  eventhub_authorization_rule_id = var.eventhub_manage_auth_rule_id

  log {
    category = "FrontdoorAccessLog"
    enabled  = false

    retention_policy {
      days = 0
      enabled = false
    }
  }

  log {
    category = "FrontdoorWebApplicationFirewallLog"
    enabled  = true

    retention_policy {
      days = 0
      enabled = false
    }
  }

  metric {
    category = "AllMetrics"
    enabled = false

    retention_policy {
        days = 0
        enabled = false
    }
  }
}

data "azurerm_key_vault_secret" "https_cert" {
    count = length(var.https_cert_names)
    key_vault_id = var.key_vault_id
    name = var.https_cert_names[count.index]
}

output "id" {
    value = azurerm_frontdoor.front_door.id
}

output "cname" {
    value = azurerm_frontdoor.front_door.cname
}
