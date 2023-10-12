## Let's get our secrets from the secrets key vault
## Note, this will need to be pre-populated

data "azurerm_key_vault" "app_config" {
  name                = local.key_vault.app_config_kv_name
  resource_group_name = local.init.resource_group_name
}

data "azurerm_key_vault_secret" "sendgrid_password" {
  name         = "sendgrid-password"
  key_vault_id = data.azurerm_key_vault.app_config.id
}

data "azurerm_key_vault_secret" "postgres_user" {
  name         = "functionapp-postgres-user"
  key_vault_id = data.azurerm_key_vault.app_config.id
}

data "azurerm_key_vault_secret" "postgres_pass" {
  name         = "functionapp-postgres-pass"
  key_vault_id = data.azurerm_key_vault.app_config.id
}

data "azurerm_key_vault_secret" "postgres_readonly_user" {
  name         = "functionapp-postgres-readonly-user"
  key_vault_id = data.azurerm_key_vault.app_config.id
}

data "azurerm_key_vault_secret" "postgres_readonly_pass" {
  name         = "functionapp-postgres-readonly-pass"
  key_vault_id = data.azurerm_key_vault.app_config.id
}

data "azurerm_key_vault" "tf-secrets" {
  name                = local.key_vault.tf_secrets_vault
  resource_group_name = local.init.resource_group_name
}

data "azurerm_key_vault_secret" "pagerduty_url" {
  name         = "pagerduty-integration-url"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}

data "azurerm_key_vault_key" "pdhtest-2048-key" {
  name         = "pdh${local.init.environment}-2048-key"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}


data "azurerm_key_vault_secret" "slack_email_address" {
  name         = "slack-email"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}

data "azurerm_key_vault_secret" "terraform_caller_ip_address" {
  name         = "terraform-caller-ip-address"
  key_vault_id = "/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/prime-data-hub-test/providers/Microsoft.KeyVault/vaults/pdhtest-keyvault"
}