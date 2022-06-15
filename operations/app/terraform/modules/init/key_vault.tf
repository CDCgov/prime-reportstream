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
    pagerduty-businesshours-url = {
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
  for_each = toset(["appconfig", "keyvault"])

  name                            = "${var.resource_prefix}-${each.value}"
  location                        = var.location
  resource_group_name             = var.resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = false

  network_acls {
    bypass         = "AzureServices"
    default_action = "Deny"

    ip_rules = var.terraform_caller_ip_address

    //virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      # Temp ignore ip_rules during tf development
      network_acls[0].ip_rules
    ]
  }

  access_policy {
    tenant_id = data.azurerm_client_config.current.tenant_id
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
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_secret" "init" {
  for_each = local.secrets

  name         = each.key
  value        = each.value.secret
  key_vault_id = each.value.vault.id
}

resource "azurerm_key_vault_key" "init" {
  for_each = local.keys

  name         = each.key
  key_vault_id = each.value.vault.id
  key_type     = "RSA"
  key_size     = 2048

  key_opts = [
    "decrypt",
    "encrypt",
    "sign",
    "unwrapKey",
    "verify",
    "wrapKey"
  ]
}
