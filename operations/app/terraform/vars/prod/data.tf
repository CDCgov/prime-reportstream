## Let's get our secrets from the secrets key vault
## Note, this will need to be pre-populated

data "azurerm_key_vault" "app_config" {
  name                = local.key_vault.app_config_kv_name
  resource_group_name = local.init.resource_group_name
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

data "azurerm_key_vault_key" "pdh-2048-key" {
  name         = "pdh${local.init.environment}-2048-key"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}

data "azurerm_key_vault_secret" "slack_email_address" {
  name         = "slack-email"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}

data "azurerm_key_vault_secret" "chatops_slack_bot_token" {
  name         = "chatops-slack-bot-token"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}

data "azurerm_key_vault_secret" "chatops_slack_app_token" {
  name         = "chatops-slack-app-token"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}

data "azurerm_key_vault_secret" "chatops_github_token" {
  name         = "chatops-github-token"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}
