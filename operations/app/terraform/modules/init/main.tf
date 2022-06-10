// terraform -chdir=operations/app/terraform/vars/demo init -target=module.init
// terraform -chdir=operations/app/terraform/vars/demo plan -target=module.init

data "azurerm_client_config" "current" {}
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
  purge_protection_enabled        = true

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
    object_id = var.aad_object_keyvault_admin

    key_permissions = [
      "Create",
      "Get",
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
  name         = "functionapp-postgres-user"
  value        = "prime"
  key_vault_id = azurerm_key_vault.init["appconfig"].id

  depends_on = [
    azurerm_key_vault.init["appconfig"]
  ]
}
