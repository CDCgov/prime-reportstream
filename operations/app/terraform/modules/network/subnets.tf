
/* Postgres subnet */
data "azurerm_subnet" "postgres_subnet" {
  for_each             = toset(data.azurerm_virtual_network.vnet["db-vnet"].subnets)
  name                 = each.value
  resource_group_name  = var.resource_group
  virtual_network_name = var.azure_vns["db-vnet"].name
}

/* App subnet */
data "azurerm_subnet" "app_subnet" {
    for_each             = toset([for s in data.azurerm_virtual_network.vnet["app-vnet"].subnets : s if strcontains(s, "rest-app")])
    name                 = each.value
    virtual_network_name = var.azure_vns["app-vnet"].name
    resource_group_name  = var.resource_group
}

data "azurerm_subnet" "container_subnet" {
  for_each             = toset([for s in data.azurerm_virtual_network.vnet["app-vnet"].subnets : s if strcontains(s, "container")])
  name                 = each.value
  virtual_network_name = var.azure_vns["app-vnet"].name
  resource_group_name  = var.resource_group
}


