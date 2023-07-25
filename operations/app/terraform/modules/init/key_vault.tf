locals {
  secrets = {
    functionapp-postgres-user = {
      secret = "prime",
      vault  = azurerm_key_vault.init["appconfig"]
    },
    functionapp-postgres-pass = {
      secret = "changeIT!",
      vault  = azurerm_key_vault.init["appconfig"]
    },
    pagerduty-integration-url = {
      secret = "https://foo.local",
      vault  = azurerm_key_vault.init["keyvault"]
    },
    hhsprotect-ip-ingress = {
      secret = "54.211.46.158,54.145.176.199",
      vault  = azurerm_key_vault.init["keyvault"]
    },
    cyberark-ip-ingress = {
      secret = "158.111.21.0/24,158.111.123.0/25",
      vault  = azurerm_key_vault.init["keyvault"]
    }
  }
  keys = {
    "${var.resource_prefix}-2048-key" = {
      vault = azurerm_key_vault.init["keyvault"]
    }
  }
}

resource "azurerm_key_vault" "init" {
  for_each                        = toset(["appconfig", "keyvault"])
  name                            = "${var.resource_prefix}-${each.value}${var.random_id}"
  location                        = var.location
  resource_group_name             = var.resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = false
  soft_delete_retention_days      = 7

  network_acls {
    bypass         = "AzureServices"
    default_action = "Allow"

    ip_rules = var.terraform_caller_ip_address

    //virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
  }

  depends_on = [
    azurerm_virtual_network.init,
    module.subnets
  ]

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "init" {
  for_each = toset(["appconfig", "keyvault"])

  key_vault_id = azurerm_key_vault.init[each.value].id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  // CT-PRIMEReportStream-AZ-Owners
  object_id = "52fe06f4-9717-4beb-9b7a-e05b6bdd2f0f"

  key_permissions = [
    "Create",
    "Get",
    "List",
    "Delete",
    "Purge"
  ]

  secret_permissions = [
    "Set",
    "List",
    "Get",
    "Delete",
    "Purge",
    "Recover"
  ]

  certificate_permissions = []

  depends_on = [
    azurerm_key_vault.init
  ]
}

resource "azurerm_key_vault_access_policy" "init_tf" {
  for_each = toset(["appconfig", "keyvault"])

  key_vault_id = azurerm_key_vault.init[each.value].id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  // terraform-automation 5ab367bf-df15-45af-a027-47f95f2c75d8
  object_id = "4d81288c-27a3-4df8-b776-c9da8e688bc7"

  key_permissions = [
    "Create",
    "Get",
    "List",
    "Delete",
    "Purge"
  ]

  secret_permissions = [
    "Set",
    "List",
    "Get",
    "Delete",
    "Purge",
    "Recover"
  ]

  certificate_permissions = []

  depends_on = [
    azurerm_key_vault.init
  ]
}

resource "azurerm_key_vault_secret" "init" {
  for_each = local.secrets

  name            = each.key
  value           = each.value.secret
  key_vault_id    = each.value.vault.id
  content_type    = "text/plain"
  expiration_date = "2028-01-01T00:00:00Z"

  depends_on = [
    azurerm_key_vault_access_policy.init
  ]
}

resource "azurerm_key_vault_key" "init" {
  for_each        = local.keys
  name            = each.key
  key_vault_id    = each.value.vault.id
  key_type        = "RSA-HSM"
  key_size        = 2048
  expiration_date = "2028-01-01T00:00:00Z"

  key_opts = [
    "decrypt",
    "encrypt",
    "sign",
    "unwrapKey",
    "verify",
    "wrapKey"
  ]

  depends_on = [
    azurerm_key_vault_access_policy.init
  ]
}
