/* Public subnet */
data "azurerm_subnet" "public_subnet" {
  for_each             = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "public") }
  name                 = "public"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.resource_prefix}-${each.key}"
}


/* Container subnet */
data "azurerm_subnet" "container_subnet" {
  for_each             = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "container") }
  name                 = "container"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.resource_prefix}-${each.key}"

}

/* Private subnet */
data "azurerm_subnet" "private_subnet" {
  for_each             = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "private") }
  name                 = "private"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.resource_prefix}-${each.key}"
}

/* Endpoint subnet */
data "azurerm_subnet" "endpoint_subnet" {
  for_each = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "endpoint") }

  name                 = "endpoint"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.resource_prefix}-${each.key}"
}
