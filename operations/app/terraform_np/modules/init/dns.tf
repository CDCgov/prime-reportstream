resource "azurerm_network_profile" "init" {
  name                = "${var.resource_prefix}-dns-profile"
  location            = var.location
  resource_group_name = var.resource_group

  container_network_interface {
    name = "${var.resource_prefix}-dns-network-interface"

    ip_configuration {
      name      = "${var.resource_prefix}-dns-ip-config"
      subnet_id = module.subnets["vnet"].dns_container_subnet_id
    }
  }

  depends_on = [
    azurerm_virtual_network.init
  ]
}

resource "azurerm_container_group" "init" {
  name                = "${var.resource_prefix}-dns"
  location            = var.location
  resource_group_name = var.resource_group
  ip_address_type     = "Private"
  os_type             = "Linux"
  network_profile_id  = azurerm_network_profile.init.id

  container {
    name   = "dnsmasq"
    image  = "ghcr.io/cdcgov/prime-reportstream_dnsmasq:${var.environment}"
    cpu    = "0.5"
    memory = "1.5"

    ports {
      port     = 53
      protocol = "UDP"
    }
  }
}
