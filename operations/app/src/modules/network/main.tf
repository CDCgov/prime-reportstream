terraform {
    required_version = ">= 0.14"
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

resource "azurerm_subnet" "gateway" {
  name = "GatewaySubnet" # This subnet must be named this. Azure expects this name for VPN gateways.
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  address_prefixes = ["10.0.4.0/24"]
}

resource "azurerm_public_ip" "vpn_ip" {
  name = "${var.resource_prefix}-vpn-ip"
  location = var.location
  resource_group_name = var.resource_group
  allocation_method = "Dynamic"
}


## Outputs

output "public_subnet_id" {
  value = azurerm_subnet.public.id
}

output "container_subnet_id" {
  value = azurerm_subnet.container.id
}

output "private_subnet_id" {
  value = azurerm_subnet.private.id
}
