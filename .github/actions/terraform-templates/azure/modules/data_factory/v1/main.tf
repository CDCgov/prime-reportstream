resource "azurerm_data_factory" "default" {
  name                = "df-${var.common.uid}-${var.common.env}"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name

  identity {
    type = "SystemAssigned"
  }
}

resource "azurerm_role_assignment" "df_blob" {
  scope                = var.common.resource_group.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_data_factory.default.identity[0].principal_id
}

resource "azurerm_role_assignment" "df_kv" {
  scope                = var.common.resource_group.id
  role_definition_name = "Key Vault Reader"
  principal_id         = azurerm_data_factory.default.identity[0].principal_id
}

resource "azurerm_role_assignment" "df_kv_secrets" {
  scope                = var.common.resource_group.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_data_factory.default.identity[0].principal_id
}

resource "azurerm_data_factory_linked_service_azure_blob_storage" "default" {
  name                 = var.storage_account.name
  data_factory_id      = azurerm_data_factory.default.id
  service_endpoint     = var.storage_account.primary_blob_endpoint
  use_managed_identity = true
}

resource "azurerm_data_factory_linked_custom_service" "rest_default" {
  name            = "${var.common.env}-rest"
  data_factory_id = azurerm_data_factory.default.id
  description     = "Managed by Terraform"

  type                 = "RestService"
  type_properties_json = <<JSON
{
    "url": "${var.secrets["${var.common.env}-API-ENDPOINT"].value}",
    "enableServerCertificateValidation": true,
    "authenticationType": "Basic",
    "userName": "${var.secrets["${var.common.env}-KEY"].value}",
    "password": {
        "type": "AzureKeyVaultSecret",
        "store": {
            "referenceName": "${azurerm_data_factory_linked_service_key_vault.default.name}",
            "type": "LinkedServiceReference"
        },
        "secretName": "${var.common.env}-SECRET"
    }
}
JSON

  depends_on = [azurerm_role_assignment.df_kv_secrets]
}

resource "azurerm_data_factory_linked_service_key_vault" "default" {
  name            = "${var.common.env}-kv"
  data_factory_id = azurerm_data_factory.default.id
  key_vault_id    = var.key_vault.id
}


resource "azurerm_data_factory_custom_dataset" "rest_default" {
  name            = "${var.common.env}-rest"
  data_factory_id = azurerm_data_factory.default.id
  type            = "RestResource"

  linked_service {
    name = azurerm_data_factory_linked_custom_service.rest_default.name
  }

  type_properties_json = <<JSON
{
  "relativeUrl": "${var.secrets["${var.common.env}-API-URL"].value}"
}
JSON

  description = "test description"
}