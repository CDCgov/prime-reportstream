resource "azurerm_api_management_api" "default" {
  name                = "${var.common.env}-api"
  resource_group_name = var.common.resource_group.name
  api_management_name = azurerm_api_management.default.name
  revision            = "1"
  display_name        = "${var.common.env} API"
  path                = var.common.env
  protocols           = ["https"]
}

resource "azurerm_api_management_product" "default" {
  product_id            = "${var.common.env}-product"
  api_management_name   = azurerm_api_management.default.name
  resource_group_name   = var.common.resource_group.name
  display_name          = "${var.common.env} Product"
  subscription_required = true
  subscriptions_limit   = 10
  approval_required     = true
  published             = true
}

resource "azurerm_api_management_product_api" "default" {
  api_name            = azurerm_api_management_api.default.name
  product_id          = azurerm_api_management_product.default.product_id
  api_management_name = azurerm_api_management.default.name
  resource_group_name = var.common.resource_group.name
}

resource "azurerm_api_management_api_operation" "default_fetch" {
  operation_id        = "${var.common.env}-fetch"
  api_name            = azurerm_api_management_api.default.name
  api_management_name = azurerm_api_management.default.name
  resource_group_name = var.common.resource_group.name
  display_name        = "${var.common.env} fetch db"
  method              = "GET"
  url_template        = "/db"
  description         = "This can only be done by the logged in user."

  response {
    status_code = 200
  }
}

resource "azurerm_api_management_api_operation" "default_insert" {
  operation_id        = "${var.common.env}-insert"
  api_name            = azurerm_api_management_api.default.name
  api_management_name = azurerm_api_management.default.name
  resource_group_name = var.common.resource_group.name
  display_name        = "${var.common.env} insert db"
  method              = "POST"
  url_template        = "/db"
  description         = "This can only be done by the logged in user."

  response {
    status_code = 200
  }
}

resource "azurerm_api_management_backend" "default" {
  name                = "${var.common.env}-la-db-backend"
  resource_group_name = var.common.resource_group.name
  api_management_name = azurerm_api_management.default.name
  protocol            = "http"
  url                 = var.logic_app_endpoint
}

resource "azurerm_api_management_api_operation_policy" "default" {
  api_name            = azurerm_api_management_api.default.name
  operation_id        = azurerm_api_management_api_operation.default_fetch.operation_id
  api_management_name = azurerm_api_management.default.name
  resource_group_name = var.common.resource_group.name
  xml_content         = <<XML
<policies>
  <inbound>
     <base />
     <set-backend-service backend-id="${azurerm_api_management_backend.default.name}" />
     <set-method>POST</set-method>
     <rewrite-uri template="?" />
     <set-header name="Ocp-Apim-Subscription-Key" exists-action="delete" />
  </inbound>
</policies>
XML
}
