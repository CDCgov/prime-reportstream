## Let's get our secrets from the secrets key vault
## Note, this will need to be pre-populated

data "azurerm_key_vault" "tf-secrets" {
  name                = var.tf_secrets_vault
  resource_group_name = var.resource_group
}

data "azurerm_key_vault_secret" "postgres_user" {
  name         = "postgres-user"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}

data "azurerm_key_vault_secret" "postgres_pass" {
  name         = "postgres-pass"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}

data "azurerm_key_vault_secret" "pagerduty_url" {
  name         = "pagerduty-url"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id
}