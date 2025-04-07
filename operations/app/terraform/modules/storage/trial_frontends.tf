// Storage accounts for trial/test frontend deployments
resource "azurerm_storage_account" "storage_trials" {
  for_each                        = local.trial_env ? toset(local.trial_accounts) : []
  resource_group_name             = var.resource_group
  name                            = "${var.resource_prefix}publictrial${each.value}"
  location                        = var.location
  account_tier                    = "Standard"
  account_kind                    = "StorageV2"
  account_replication_type        = "LRS"
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false
  https_traffic_only_enabled      = true
  local_user_enabled              = false

  static_website {
    index_document     = "index.html"
    error_404_document = "404.html"
  }

  network_rules {
    default_action = "Allow"
  }

  lifecycle {
    prevent_destroy = false
  }

  tags = {
    environment = var.environment
  }

  timeouts {
    create = var.timeout_create
    read   = var.timeout_read
    delete = var.timeout_delete
    update = var.timeout_update
  }
}
