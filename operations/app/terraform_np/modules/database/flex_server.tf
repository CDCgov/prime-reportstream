resource "azurerm_postgresql_flexible_server" "flex_server" {
  for_each = toset(var.flex_instances)

  name                         = "${var.resource_prefix}-pgsql-flex"
  location                     = var.location
  resource_group_name          = var.resource_group
  version                      = "13"
  delegated_subnet_id          = var.subnets.postgres_subnets[0]
  private_dns_zone_id          = var.dns_zones["postgres"].id
  administrator_login          = var.postgres_user
  administrator_password       = var.postgres_pass
  zone                         = "1"
  geo_redundant_backup_enabled = true

  storage_mb = 32768

  sku_name = var.flex_sku_name
}
