# operations/app/terraform_np/migration/migration_copy_secrets.tf

# --- Configuration Variables for Source Vaults ---
# These defaults are derived from operations/app/terraform_np/environments/prod/locals.tf

variable "old_resource_group_name" {
  description = "The name of the resource group containing the source Key Vaults."
  type        = string
  default     = "prime-data-hub-prod" # From environments/prod/locals.tf: local.init.resource_group_name
}

variable "old_admin_app_vault_name" {
  description = "Name of the old admin/application Key Vault."
  type        = string
  default     = "pdhprod-keyvault" # From environments/prod/locals.tf: local.key_vault.application_kv_name
}

variable "old_appconfig_vault_name" {
  description = "Name of the old appconfig Key Vault."
  type        = string
  default     = "pdhprod-appconfig" # From environments/prod/locals.tf: local.key_vault.app_config_kv_name
}

variable "old_clientconfig_vault_name" {
  description = "Name of the old clientconfig Key Vault."
  type        = string
  default     = "pdhprod-clientconfig" # From environments/prod/locals.tf: local.key_vault.client_config_kv_name
}

# --- Helper Data Source (Ensure this is defined, e.g., in migration/data.tf) ---
# data "azurerm_client_config" "current" {} # Already present in migration/data.tf


# --- Copy Secrets for Admin/Application Vault ---
# Source: pdhprod-keyvault in prime-data-hub-prod
# Target: ddphss-prd-admin-functionapp (Resource: azurerm_key_vault.ddphss_prd_vault) in ddphss-prim-prd-moderate-app-rg

# 1. Reference the OLD admin/app vault
data "azurerm_key_vault" "old_admin_app_vault" {
  name                = var.old_admin_app_vault_name
  resource_group_name = var.old_resource_group_name
}

# 2. Use external data source to LIST secret names from the OLD vault
data "external" "old_admin_app_vault_secret_names" {
  program = ["bash", "-c", "az keyvault secret list --vault-name ${var.old_admin_app_vault_name} -g ${var.old_resource_group_name} --query '[?attributes.enabled].name' -o json"]

  query = {
    vault_id = data.azurerm_key_vault.old_admin_app_vault.id
  }
}

# 3. READ the value of each secret from the OLD vault
data "azurerm_key_vault_secret" "old_admin_app_secrets" {
  for_each = toset(jsondecode(data.external.old_admin_app_vault_secret_names.result))

  name         = each.value
  key_vault_id = data.azurerm_key_vault.old_admin_app_vault.id
}

# 4. WRITE each secret to the NEW vault (defined in migration/key_vaults.tf)
resource "azurerm_key_vault_secret" "new_admin_app_secrets" {
  for_each = data.azurerm_key_vault_secret.old_admin_app_secrets

  name         = each.key
  value        = each.value.value
  key_vault_id = azurerm_key_vault.ddphss_prd_vault.id # Target NEW vault resource

  lifecycle {
    ignore_changes = [value, tags, content_type]
  }
}


# --- Copy Secrets for AppConfig Vault ---
# Source: pdhprod-appconfig in prime-data-hub-prod
# Target: ddphss-prd-appconfig (Resource: azurerm_key_vault.ddphss_prd_appconfig) in ddphss-prim-prd-moderate-app-rg

# 1. Reference the OLD appconfig vault
data "azurerm_key_vault" "old_appconfig_vault" {
  name                = var.old_appconfig_vault_name
  resource_group_name = var.old_resource_group_name
}

# 2. LIST secret names from the OLD vault
data "external" "old_appconfig_vault_secret_names" {
  program = ["bash", "-c", "az keyvault secret list --vault-name ${var.old_appconfig_vault_name} -g ${var.old_resource_group_name} --query '[?attributes.enabled].name' -o json"]
  query = {
    vault_id = data.azurerm_key_vault.old_appconfig_vault.id
  }
}

# 3. READ the value of each secret from the OLD vault
data "azurerm_key_vault_secret" "old_appconfig_secrets" {
  for_each = toset(jsondecode(data.external.old_appconfig_vault_secret_names.result))
  name         = each.value
  key_vault_id = data.azurerm_key_vault.old_appconfig_vault.id
}

# 4. WRITE each secret to the NEW vault
resource "azurerm_key_vault_secret" "new_appconfig_secrets" {
  for_each = data.azurerm_key_vault_secret.old_appconfig_secrets
  name         = each.key
  value        = each.value.value
  key_vault_id = azurerm_key_vault.ddphss_prd_appconfig.id # Target NEW vault resource
  lifecycle {
    ignore_changes = [value, tags, content_type]
  }
}


# --- Copy Secrets for ClientConfig Vault ---
# Source: pdhprod-clientconfig in prime-data-hub-prod
# Target: ddphss-prd-clientconfig (Resource: azurerm_key_vault.ddphss_prd_clientconfig) in ddphss-prim-prd-moderate-app-rg

# 1. Reference the OLD clientconfig vault
data "azurerm_key_vault" "old_clientconfig_vault" {
  name                = var.old_clientconfig_vault_name
  resource_group_name = var.old_resource_group_name
}

# 2. LIST secret names from the OLD vault
data "external" "old_clientconfig_vault_secret_names" {
  program = ["bash", "-c", "az keyvault secret list --vault-name ${var.old_clientconfig_vault_name} -g ${var.old_resource_group_name} --query '[?attributes.enabled].name' -o json"]
  query = {
    vault_id = data.azurerm_key_vault.old_clientconfig_vault.id
  }
}

# 3. READ the value of each secret from the OLD vault
data "azurerm_key_vault_secret" "old_clientconfig_secrets" {
  for_each = toset(jsondecode(data.external.old_clientconfig_vault_secret_names.result))
  name         = each.value
  key_vault_id = data.azurerm_key_vault.old_clientconfig_vault.id
}

# 4. WRITE each secret to the NEW vault
resource "azurerm_key_vault_secret" "new_clientconfig_secrets" {
  for_each = data.azurerm_key_vault_secret.old_clientconfig_secrets
  name         = each.key
  value        = each.value.value
  key_vault_id = azurerm_key_vault.ddphss_prd_clientconfig.id # Target NEW vault resource
  lifecycle {
    ignore_changes = [value, tags, content_type]
  }
}

# --- NOTE on ddphss-prd-tfsecrets ---
# The fourth vault defined in migration/key_vaults.tf is 'ddphss-prd-tfsecrets'.
# Copying secrets automatically from an old 'tfsecrets' vault (likely 'pdhprod-keyvault' based on prod locals)
# is generally NOT recommended. We'll need to review and manually add required secrets to the new vault.
 
