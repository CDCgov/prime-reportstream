/* DNS server for VPN */
resource "azurerm_network_profile" "vpn_dns_network_profile" {
  name                = "${var.resource_prefix}-vpn-dns-profile"
  location            = var.location
  resource_group_name = var.resource_group

  container_network_interface {
    name = "${var.resource_prefix}-vpn-dns-network-interface"

    ip_configuration {
      name      = "${var.resource_prefix}-vpn-dns-ip-config"
      subnet_id = azurerm_subnet.container_subnet[local.vnet_primary.name].id
    }
  }
}

resource "azurerm_container_group" "vpn_dns" {
  name                = "${var.resource_prefix}-vpn-dns"
  location            = var.location
  resource_group_name = var.resource_group
  ip_address_type     = "Private"
  network_profile_id  = azurerm_network_profile.vpn_dns_network_profile.id
  os_type             = "Linux"
  restart_policy      = "Always"

  container {
    name   = "dnsmasq"
    image  = "andyshinn/dnsmasq:2.83"
    cpu    = "0.5"
    memory = "1.0"

    ports {
      # Both TCP and UDP can not be configured at the same time
      port     = 53
      protocol = "UDP"
    }
  }

  tags = {
    environment = var.environment
  }

  lifecycle {
    // Workaround. TF thinks this is a new resource after import
    ignore_changes = [
      network_profile_id,
    ]
  }
}

/* VPN Gateway */
resource "azurerm_virtual_network_gateway" "vpn_network_gateway" {
  name                = "${var.resource_prefix}-vpn-gateway"
  location            = var.location
  resource_group_name = var.resource_group
  sku                 = "VpnGw1"
  type                = "Vpn"

  ip_configuration {
    public_ip_address_id = azurerm_public_ip.vpn_public_ip.id
    subnet_id            = azurerm_subnet.gateway_subnet.id
  }

  vpn_client_configuration {
    # Clients connected to the VPN will receive an IP address in this space
    address_space = [
      "192.168.10.0/24",
    ]
    vpn_client_protocols = [
      "OpenVPN",
    ]

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

resource "azurerm_subnet" "gateway_subnet" {
  # This subnet must be named this. Azure expects this name for VPN gateways.
  name = "GatewaySubnet"

  resource_group_name  = var.resource_group
  virtual_network_name = local.vnet_primary.name

  address_prefixes = [
    local.vnet_subnets_cidrs[local.vnet_primary.name][4],
  ]
}

resource "azurerm_public_ip" "vpn_public_ip" {
  name                = "${var.resource_prefix}-vpn-public-ip"
  location            = var.location
  resource_group_name = var.resource_group
  allocation_method   = "Dynamic"
}