locals {
  name        = var.environment != "dev" ? "prime-data-hub-${var.environment}" : "prime-data-hub-${var.resource_prefix}"
  name_static = var.environment != "dev" ? "prime-data-hub-${var.environment}-static" : "prime-data-hub-${var.resource_prefix}-static"

  functionapp_address = "${var.resource_prefix}-functionapp.azurewebsites.net"
  metabase_address    = var.is_metabase_env ? "${var.resource_prefix}-metabase.azurewebsites.net" : null
  static_address      = trimprefix(trimsuffix(var.public_primary_web_endpoint, "/"), "https://")

  function_certs     = [for cert in var.https_cert_names : cert if length(regexall("^[[:alnum:]]*?-?prime.*$", cert)) > 0]
  frontend_endpoints = (length(local.function_certs) > 0) ? concat(["DefaultFrontendEndpoint"], local.function_certs) : ["DefaultFrontendEndpoint"]

  static_certs     = [for cert in var.https_cert_names : cert if length(regexall("^[[:alnum:]]*?-?reportstream.*$", cert)) > 0]
  static_endpoints = local.static_certs

  metabase_env = var.is_metabase_env ? [1] : []
  static_env   = length(local.static_endpoints) > 0 ? [1] : []
  dev_env      = length(local.static_endpoints) == 0 ? [1] : []
}

data "azurerm_client_config" "current" {}

# TODO: Terraform does not support Azure's rules engine yet
# We have an HSTS rule that must be manually configured
# Ticket tracking rules engine in Terraform: https://github.com/terraform-providers/terraform-provider-azurerm/issues/7455

resource "azurerm_frontdoor" "front_door" {
  name                                         = local.name
  resource_group_name                          = var.resource_group
  enforce_backend_pools_certificate_name_check = true
  backend_pools_send_receive_timeout_seconds   = 90
  friendly_name                                = local.name

  /* General */

  frontend_endpoint {
    name      = "DefaultFrontendEndpoint"
    host_name = "${local.name}.azurefd.net"
  }

  dynamic "frontend_endpoint" {
    for_each = var.https_cert_names
    content {
      name      = frontend_endpoint.value
      host_name = replace(frontend_endpoint.value, "-", ".")
    }
  }

  /* Function app */

  backend_pool {
    name                = "functions"
    health_probe_name   = "functionsHealthProbeSettings"
    load_balancing_name = "functionsLoadBalancingSettings"

    backend {
      address     = local.functionapp_address
      host_header = local.functionapp_address
      http_port   = 80
      https_port  = 443
    }
  }

  backend_pool_load_balancing {
    name                            = "functionsLoadBalancingSettings"
    sample_size                     = 4
    successful_samples_required     = 2
    additional_latency_milliseconds = 0
  }

  backend_pool_health_probe {
    name                = "functionsHealthProbeSettings"
    path                = "/"
    interval_in_seconds = 30
    protocol            = "Https"
    probe_method        = "HEAD"
  }

  routing_rule {
    name               = "api"
    frontend_endpoints = local.frontend_endpoints
    accepted_protocols = ["Https"]
    patterns_to_match  = ["/*", "/api/*"]

    forwarding_configuration {
      backend_pool_name   = "functions"
      forwarding_protocol = "HttpsOnly"
    }
  }

  routing_rule {
    name               = "download"
    frontend_endpoints = local.frontend_endpoints
    accepted_protocols = ["Https"]
    patterns_to_match  = ["/", "/download"]

    # Redirect to the new download site in environments with it deployed
    dynamic "redirect_configuration" {
      for_each = local.static_env
      content {
        redirect_protocol = "MatchRequest"
        redirect_type     = "TemporaryRedirect"

        # Convert test-reportstream-gov to test.reportstream.gov
        custom_host = replace(local.static_endpoints[0], "-", ".")

        # Clear any existing URL paths
        custom_path         = "/"
        custom_query_string = ""
        custom_fragment     = ""
      }
    }

    # Use the old download site if it's not deployed
    # (this also applies to the dev environment, which does not have a cert)
    dynamic "forwarding_configuration" {
      for_each = local.dev_env
      content {
        backend_pool_name      = "functions"
        forwarding_protocol    = "HttpsOnly"
        custom_forwarding_path = "/api/download"
      }
    }
  }

  routing_rule {
    name               = "HttpToHttpsRedirect"
    frontend_endpoints = local.frontend_endpoints
    accepted_protocols = ["Http"]
    patterns_to_match  = ["/", "/*", "/api/*", "/download"]

    redirect_configuration {
      redirect_protocol = "HttpsOnly"
      redirect_type     = "Moved"
    }
  }

  /* Metabase */

  dynamic "backend_pool" {
    for_each = local.metabase_env
    content {
      name                = "metabase"
      health_probe_name   = "metabaseHealthProbeSettings"
      load_balancing_name = "metabaseLoadBalancingSettings"

      backend {
        address     = local.metabase_address
        host_header = local.metabase_address
        http_port   = 80
        https_port  = 443
      }
    }
  }

  dynamic "backend_pool_load_balancing" {
    for_each = local.metabase_env
    content {
      name                            = "metabaseLoadBalancingSettings"
      sample_size                     = 4
      successful_samples_required     = 2
      additional_latency_milliseconds = 0
    }
  }

  dynamic "backend_pool_health_probe" {
    for_each = local.metabase_env
    content {
      name                = "metabaseHealthProbeSettings"
      path                = "/"
      interval_in_seconds = 30
      protocol            = "Https"
      probe_method        = "HEAD"
    }
  }

  dynamic "routing_rule" {
    for_each = local.metabase_env
    content {
      name               = "metabase"
      frontend_endpoints = local.frontend_endpoints
      accepted_protocols = ["Https"]
      patterns_to_match  = ["/metabase", "/metabase/*"]

      forwarding_configuration {
        backend_pool_name      = "metabase"
        forwarding_protocol    = "HttpsOnly"
        custom_forwarding_path = "/"
      }
    }
  }

  dynamic "routing_rule" {
    for_each = local.metabase_env
    content {
      name               = "HttpToHttpsRedirectMetabase"
      frontend_endpoints = local.frontend_endpoints
      accepted_protocols = ["Http"]
      patterns_to_match  = ["/metabase", "/metabase/*"]

      redirect_configuration {
        redirect_protocol = "HttpsOnly"
        redirect_type     = "Moved"
      }
    }
  }

  /* Static site */

  backend_pool {
    name                = "static"
    health_probe_name   = "staticHealthProbeSettings"
    load_balancing_name = "staticLoadBalancingSettings"

    backend {
      address     = local.static_address
      host_header = local.static_address
      http_port   = 80
      https_port  = 443
    }
  }

  backend_pool_load_balancing {
    name                            = "staticLoadBalancingSettings"
    sample_size                     = 4
    successful_samples_required     = 2
    additional_latency_milliseconds = 0
  }

  backend_pool_health_probe {
    name                = "staticHealthProbeSettings"
    path                = "/"
    interval_in_seconds = 30
    protocol            = "Https"
    probe_method        = "HEAD"
  }

  dynamic "routing_rule" {
    for_each = local.static_env
    content {
      name               = "HttpToHttpsRedirectStatic"
      frontend_endpoints = local.static_endpoints
      accepted_protocols = ["Http"]
      patterns_to_match  = ["/", "/*"]

      redirect_configuration {
        redirect_protocol = "HttpsOnly"
        redirect_type     = "Moved"
      }
    }
  }

  dynamic "routing_rule" {
    for_each = local.static_env
    content {
      name               = "Static"
      frontend_endpoints = local.static_endpoints
      accepted_protocols = ["Https"]
      patterns_to_match  = ["/", "/*"]

      forwarding_configuration {
        backend_pool_name   = "static"
        forwarding_protocol = "HttpsOnly"
      }
    }
  }
}

resource "azurerm_frontdoor_custom_https_configuration" "frontend_default_https" {
  frontend_endpoint_id              = azurerm_frontdoor.front_door.frontend_endpoints["DefaultFrontendEndpoint"]
  custom_https_provisioning_enabled = false

  lifecycle {
    ignore_changes = [
      # Avoid cert updates blocking tf
      custom_https_configuration[0].azure_key_vault_certificate_secret_version
    ]
  }

  depends_on = [
    azurerm_frontdoor.front_door,
    azurerm_key_vault_access_policy.frontdoor_access_policy
  ]
}

resource "azurerm_frontdoor_custom_https_configuration" "frontend_custom_https" {
  for_each = toset(var.https_cert_names)

  frontend_endpoint_id              = azurerm_frontdoor.front_door.frontend_endpoints[each.value]
  custom_https_provisioning_enabled = true

  custom_https_configuration {
    certificate_source                      = "AzureKeyVault"
    azure_key_vault_certificate_secret_name = each.value
    azure_key_vault_certificate_vault_id    = var.application_key_vault_id
  }

  lifecycle {
    ignore_changes = [
      # Avoid cert updates blocking tf
      custom_https_configuration[0].azure_key_vault_certificate_secret_version
    ]
  }

  depends_on = [
    azurerm_frontdoor.front_door,
    azurerm_key_vault_access_policy.frontdoor_access_policy
  ]
}

resource "azurerm_key_vault_access_policy" "frontdoor_access_policy" {
  key_vault_id = var.application_key_vault_id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  # Microsoft.Azure.Frontdoor
  object_id = "270e4d1a-12bd-4564-8a4b-c9de1bbdbe95"

  secret_permissions = [
    "Get",
    "List",
  ]
  certificate_permissions = [
    "Get",
    "List",
  ]
}
