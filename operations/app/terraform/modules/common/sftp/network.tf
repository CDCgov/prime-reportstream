/*
"reason":"For use please contact OCIO Azure Admins."},
"policyAssignmentDisplayName":"Not allowed - Application Gateway or Front Door",
"policyAssignmentName":"7c6ce932d91a422bab364d68",
"policyAssignmentScope":"/providers/Microsoft.Management/managementGroups/CDC-Centers-MG",
"policyDefinitionDisplayName":"Not allowed resource types"

resource "azurerm_public_ip" "sftp" {
  name                = "${var.resource_prefix}-publicip"
  resource_group_name = var.resource_group
  location            = var.location
  allocation_method   = "Static"
  sku                 = "Standard"
  domain_name_label   = var.resource_prefix
}

locals {
  backend_address_pool_name      = "${var.resource_prefix}-beap"
  frontend_port_name             = "${var.resource_prefix}-feport"
  frontend_ip_configuration_name = "${var.resource_prefix}-feip"
  http_setting_name              = "${var.resource_prefix}-be-htst"
  listener_name                  = "${var.resource_prefix}-httplstn"
  request_routing_rule_name      = "${var.resource_prefix}-rqrt"
  redirect_configuration_name    = "${var.resource_prefix}-rdrcfg"
}

resource "azurerm_application_gateway" "sftp" {
  name                = "${var.resource_prefix}-appgateway"
  location            = var.location
  resource_group_name = var.resource_group

  sku {
    name     = "Standard_v2"
    tier     = "Standard_v2"
    capacity = 2
  }

  gateway_ip_configuration {
    name      = "${var.resource_prefix}-ip-configuration"
    subnet_id = var.subnet_id
  }

  frontend_port {
    name = local.frontend_port_name
    port = 80
  }

  frontend_ip_configuration {
    name                 = local.frontend_ip_configuration_name
    public_ip_address_id = azurerm_public_ip.sftp.id
  }

  backend_address_pool {
    name         = local.backend_address_pool_name
    ip_addresses = [azurerm_container_group.sftp.ip_address]
  }

  probe {
    interval            = 60
    timeout             = 60
    name                = "status"
    protocol            = "Http"
    path                = "/api/status/"
    unhealthy_threshold = 3
    host                = "127.0.0.1"
  }

  backend_http_settings {
    name                  = local.http_setting_name
    cookie_based_affinity = "Disabled"
    path                  = "/"
    port                  = 80
    protocol              = "Http"
    request_timeout       = 60
    probe_name            = "status"
  }

  http_listener {
    name                           = local.listener_name
    frontend_ip_configuration_name = local.frontend_ip_configuration_name
    frontend_port_name             = local.frontend_port_name
    protocol                       = "Http"
  }

  request_routing_rule {
    name                       = local.request_routing_rule_name
    rule_type                  = "Basic"
    http_listener_name         = local.listener_name
    backend_address_pool_name  = local.backend_address_pool_name
    backend_http_settings_name = local.http_setting_name
  }

  depends_on = [azurerm_container_group.sftp]
}
*/