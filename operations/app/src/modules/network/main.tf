locals {
  dns_zones_private = [
    "privatelink.vaultcore.azure.net",
    "privatelink.postgres.database.azure.com",
    "privatelink.blob.core.windows.net",
    "privatelink.file.core.windows.net",
    "privatelink.queue.core.windows.net",
    #"privatelink.azurecr.io",
    "privatelink.servicebus.windows.net",
    "privatelink.azurewebsites.net"
  ]
}

resource "azurerm_network_security_group" "nsg_public" {
  name = "${var.resource_prefix}-nsg.public"
  location = var.location
  resource_group_name = var.resource_group
}

resource "azurerm_network_security_group" "nsg_private" {
  name = "${var.resource_prefix}-nsg.private"
  location = var.location
  resource_group_name = var.resource_group
}

resource "azurerm_virtual_network" "virtual_network" {
  name = "${var.resource_prefix}-vnet"
  location = var.location
  resource_group_name = var.resource_group
  address_space = ["10.0.0.0/16"]

  tags = {
    environment = var.environment
  }
}

resource "azurerm_subnet" "public" {
  name = "public"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  address_prefixes = ["10.0.1.0/24"]
  service_endpoints = ["Microsoft.ContainerRegistry", 
                       "Microsoft.Storage",
                       "Microsoft.Sql",
                       "Microsoft.Web",
                       "Microsoft.KeyVault"]
  delegation {
    name = "server_farms"
    service_delegation {
      name = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

resource "azurerm_subnet_network_security_group_association" "public_public" {
  subnet_id = azurerm_subnet.public.id
  network_security_group_id = azurerm_network_security_group.nsg_public.id
}

resource "azurerm_subnet" "container" {
  name = "container"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  address_prefixes = ["10.0.2.0/24"]
  service_endpoints = ["Microsoft.Storage", "Microsoft.KeyVault"]
  delegation {
      name = "container_groups"
      service_delegation {
        name = "Microsoft.ContainerInstance/containerGroups"
        actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
      }
  }
}

resource "azurerm_subnet_network_security_group_association" "container_public" {
  subnet_id = azurerm_subnet.container.id
  network_security_group_id = azurerm_network_security_group.nsg_public.id
}

resource "azurerm_subnet" "private" {
  name = "private"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  address_prefixes = ["10.0.3.0/24"]
  service_endpoints = ["Microsoft.Storage", "Microsoft.Sql", "Microsoft.KeyVault"]

  delegation {
    name = "server_farms"
    service_delegation {
      name = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

resource "azurerm_subnet_network_security_group_association" "private_private" {
  subnet_id = azurerm_subnet.private.id
  network_security_group_id = azurerm_network_security_group.nsg_private.id
}


## Private endpoints and DNS (used for the VNET and VPN)

# This IP address of this container will be needed for the VPN configuration profile you download from the Azure console
resource "azurerm_container_group" "dns" {
  name = "${var.resource_prefix}-dns"
  location = var.location
  resource_group_name = var.resource_group
  ip_address_type = "Private"
  network_profile_id = azurerm_network_profile.dns_network_profile.id
  os_type = "Linux"
  restart_policy = "Always"

  container {
    name = "dnsmasq"
    image = "andyshinn/dnsmasq:2.83"
    cpu = "0.5"
    memory = "1.0"

    ports {
      port = 53
      protocol = "UDP" # Both TCP and UDP can not be configured at the same time
    }
  }

  tags = {
    environment = var.environment
  }

  lifecycle {
    ignore_changes = [network_profile_id] // Workaround. TF thinks this is a new resource after import
  }
}

resource "azurerm_network_profile" "dns_network_profile" {
  name = "${var.resource_prefix}-dns-profile"
  location = var.location
  resource_group_name = var.resource_group

  container_network_interface {
    name = "${var.resource_prefix}-dns-network-interface"

    ip_configuration {
      name = "${var.resource_prefix}-dns-ip-config"
      subnet_id = azurerm_subnet.container.id
    }
  }
}

# This subnet is where the private endpoints will exist
# They need a dedicated subnet, since Azure requires full automatic management capabilities of the ACL
resource "azurerm_subnet" "endpoint" {
  name = "endpoint"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  address_prefixes = ["10.0.5.0/24"]
  enforce_private_link_endpoint_network_policies = true
  service_endpoints = ["Microsoft.Storage"]
}

resource "azurerm_private_dns_zone" "dns_zone_private" {
  for_each = toset(local.dns_zones_private)
  name = each.value
  resource_group_name = var.resource_group
}

# Associate the DNS zone with our VNET, so the VNET will resolve these addresses
resource "azurerm_private_dns_zone_virtual_network_link" "dns_zone_private_link" {
  for_each = azurerm_private_dns_zone.dns_zone_private
  name = each.value.name
  private_dns_zone_name = each.value.name
  resource_group_name = var.resource_group
  virtual_network_id = azurerm_virtual_network.virtual_network.id
}


## VPN Access

resource "azurerm_virtual_network_gateway" "vpn_gateway" {
  name =  "${var.resource_prefix}-vpn"
  location = var.location
  resource_group_name = var.resource_group
  sku = "VpnGw1"
  type = "Vpn"

  ip_configuration {
    public_ip_address_id = azurerm_public_ip.vpn_ip.id
    subnet_id = azurerm_subnet.gateway.id
  }

  vpn_client_configuration {
    # Clients connected to the VPN will receive an IP address in this space
    address_space = ["192.168.10.0/24"]
    vpn_client_protocols = ["OpenVPN"]

    root_certificate {
      name = "ReportStream-VPN-Root"

      # This is a public key. Private keys are stored elsewhere,
      # so there is no security risk to storing unencrypted in a public repo.
      public_cert_data = <<EOF
MIIC5jCCAc6gAwIBAgIII4Y+H046XeswDQYJKoZIhvcNAQELBQAwETEPMA0GA1UE
AxMGVlBOIENBMB4XDTIxMDMwOTE2MDY0M1oXDTI0MDMwODE2MDY0M1owETEPMA0G
A1UEAxMGVlBOIENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq8y1
FIQ2UsiUDZL3uOmcpS9H0Kgo3/IkcUvm61+EhICqCp+4ZcYkXyKvZiFLVcPdgACT
g6Lun/DewvHYRZsHcIS/7P+58BbbJLobBviGZrQOME5DwoaTgAZLY/21RoEif/+3
kKFNy3VVClb27VTD+ak656UXeqIxCvOIHhD2OyaMUUewYFwPBymSG9VYtkXtQSi0
838ewbYVt5lWwgChA+1z+NPt9JLzB0rW1e+3H5vpJA8O5JhkpwmYN1/IaBXtZ63Z
l8fPXOhZNQMSub+3QonREYz931OZ0LNoE/gCMsy1uZ7Mk8M3TpFgF9yq2sYFmjQY
jNAl2QF1PubAc0ULqQIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB
/wQEAwIBBjAdBgNVHQ4EFgQUasPU+9+fgel7L6tECx5tPJDZ+iEwDQYJKoZIhvcN
AQELBQADggEBAARHM2oTIE8aFLpulufusQGekEkGvmuXA4yxs7gn2SNv2eg8deMi
+DRErc8yAhZn+0HwjW6UhxzHBJ0ovx2EiWCasiCez699nx+f18EmejAgSkXb8cOn
4OFTMls9BaNSbBFI6yCXNmpIqstSb/Z6RHHSgARjQqvUZElpkzYfuC6L0El70q+b
ArS+Qwkq8JJ93hPXXxUIcgaSC6KHNik0ik44nS1czYmwIyvdTeo/In2lZiqTL299
GhdGksT8b4Wz3chHvgNJoFZmxm3YpiDKyWwNMLe/T7RLu8gY66b5GvB3s0YHjq9G
axJToXMg3T9oImHz8yIk6X7j1n+UMHE9528=
EOF
    }
  }

  tags = {
    environment = var.environment
  }
}

# VPN gateways will receive an IP address in this subnet
resource "azurerm_subnet" "gateway" {
  name = "GatewaySubnet" # This subnet must be named this. Azure expects this name for VPN gateways.
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  address_prefixes = ["10.0.4.0/24"]
}

# A public IP is needed so we can access the VPN over the internet
resource "azurerm_public_ip" "vpn_ip" {
  name = "${var.resource_prefix}-vpn-ip"
  location = var.location
  resource_group_name = var.resource_group
  allocation_method = "Dynamic"
}


## Redundancy VNET

resource "azurerm_virtual_network" "virtual_network_2" {
  name = "${var.resource_prefix}-vnet-peer"
  location = "westus"
  resource_group_name = var.resource_group
  address_space = ["10.1.0.0/16"]

  tags = {
    environment = var.environment
  }
}

resource "azurerm_subnet" "private2" {
  name = "private"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network_2.name
  address_prefixes = ["10.1.3.0/24"]
  service_endpoints = ["Microsoft.Sql"]
}

resource "azurerm_virtual_network_peering" "virtual_network_peer" {
  name = "${var.resource_prefix}-peering-001"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  remote_virtual_network_id = azurerm_virtual_network.virtual_network_2.id
}

# This subnet is where the private endpoints will exist
# They need a dedicated subnet, since Azure requires full automatic management capabilities of the ACL
resource "azurerm_subnet" "endpoint2" {
  name = "endpoint"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network_2.name
  address_prefixes = ["10.1.5.0/24"]
  enforce_private_link_endpoint_network_policies = true
}
