resource "azurerm_public_ip" "nat_gateway_ip" {
  name                = "${var.resource_prefix}-publicip"
  location            = var.location
  resource_group_name = var.resource_group
  allocation_method   = "Static"
  sku                 = "Standard"

  lifecycle {
    prevent_destroy = false
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_nat_gateway" "nat_gateway" {
  name                    = "${var.resource_prefix}-natgateway"
  location                = var.location
  resource_group_name     = var.resource_group
  sku_name                = "Standard"
  idle_timeout_in_minutes = 10

  tags = {
    environment = var.environment
  }
}

resource "azurerm_nat_gateway_public_ip_association" "nat_gateway_ip_association" {
  nat_gateway_id       = azurerm_nat_gateway.nat_gateway.id
  public_ip_address_id = azurerm_public_ip.nat_gateway_ip.id
}

resource "azurerm_subnet_nat_gateway_association" "nat_gateway_public_subnet_association" {
  subnet_id      = var.subnets.public_subnets[2]
  nat_gateway_id = azurerm_nat_gateway.nat_gateway.id
}
