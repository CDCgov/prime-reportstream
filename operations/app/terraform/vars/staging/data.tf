## Let's get our secrets from the secrets key vault
## Note, this will need to be pre-populated

data "azurerm_key_vault" "app_config" {
  name                = var.app_config_kv_name
  resource_group_name = var.resource_group
}

data "azurerm_key_vault_secret" "postgres_user" {
  name         = "functionapp-postgres-user"
  key_vault_id = data.azurerm_key_vault.app_config.id
}

data "azurerm_key_vault_secret" "postgres_pass" {
  name         = "functionapp-postgres-pass"
  key_vault_id = data.azurerm_key_vault.app_config.id
}


data "azurerm_key_vault" "tf-secrets" {
  name                = var.tf_secrets_vault
  resource_group_name = var.resource_group
}

data "azurerm_key_vault_secret" "pagerduty_url" {
  name         = "pagerduty-integration-url"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}

data "azurerm_key_vault_secret" "pagerduty_businesshours_url" {
  name         = "pagerduty-businesshours-url"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}

data "azurerm_key_vault_key" "pdh-2048-key" {
  name         = "pdh${var.environment}-2048-key"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}
