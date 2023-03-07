## Let's get our secrets from the secrets key vault
## Note, this will need to be pre-populated

data "azurerm_key_vault" "app_config" {
  name                = local.key_vault.app_config_kv_name
  resource_group_name = local.init.resource_group_name

  depends_on = [
    module.init
  ]
}

data "azurerm_key_vault_secret" "postgres_user" {
  name         = "functionapp-postgres-user"
  key_vault_id = data.azurerm_key_vault.app_config.id

  depends_on = [
    module.init
  ]
}

data "azurerm_key_vault_secret" "postgres_pass" {
  name         = "functionapp-postgres-pass"
  key_vault_id = data.azurerm_key_vault.app_config.id

  depends_on = [
    module.init
  ]
}

data "azurerm_key_vault_secret" "postgres_readonly_user" {
  name         = "functionapp-postgres-readonly-user"
  key_vault_id = data.azurerm_key_vault.app_config.id

  depends_on = [
    module.init
  ]
}

data "azurerm_key_vault_secret" "postgres_readonly_pass" {
  name         = "functionapp-postgres-readonly-pass"
  key_vault_id = data.azurerm_key_vault.app_config.id

  depends_on = [
    module.init
  ]
}

data "azurerm_key_vault" "tf-secrets" {
  name                = local.key_vault.application_kv_name
  resource_group_name = local.init.resource_group_name

  depends_on = [
    module.init
  ]
}

data "azurerm_key_vault_secret" "pagerduty_url" {
  name         = "pagerduty-integration-url"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id

  depends_on = [
    module.init
  ]
}

data "azurerm_key_vault_secret" "pagerduty_businesshours_url" {
  name         = "pagerduty-businesshours-url"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id

  depends_on = [
    module.init
  ]
}

data "azurerm_key_vault_key" "pdh-2048-key" {
  name         = "pdh${local.init.environment}-2048-key"
  key_vault_id = data.azurerm_key_vault.tf-secrets.id

  depends_on = [
    module.init
  ]
}

resource "random_id" "init" {
  byte_length = 2
}
