resource "azurerm_cosmosdb_postgresql_cluster" "default" {
  name                            = "cluster-${var.common.uid}-${var.common.env}-${var.key}"
  location                        = var.common.location
  resource_group_name             = var.common.resource_group.name
  administrator_login_password    = var.admin_password
  coordinator_storage_quota_in_mb = 262144
  coordinator_vcore_count         = 4
  node_count                      = 0
}
