resource "azurerm_public_ip" "init" {
  name                = "${var.resource_prefix}-vpn-ip"
  location            = var.location
  resource_group_name = var.resource_group

  sku                     = "Basic"
  ip_version              = "IPv4"
  idle_timeout_in_minutes = 4
  allocation_method       = "Dynamic"
}

resource "azurerm_virtual_network_gateway" "init" {
  name                = "${var.resource_prefix}-vpn"
  location            = var.location
  resource_group_name = var.resource_group

  type     = "Vpn"
  vpn_type = "RouteBased"

  active_active              = false
  enable_bgp                 = false
  private_ip_address_enabled = false
  sku                        = "VpnGw1"

  ip_configuration {
    name                          = "vnetGatewayConfig"
    public_ip_address_id          = azurerm_public_ip.init.id
    private_ip_address_allocation = "Dynamic"
    subnet_id                     = module.subnets["vnet"].dns_gateway_subnet_id
  }

  vpn_client_configuration {
    address_space        = ["192.168.10.0/24"]
    vpn_client_protocols = ["OpenVPN"]

    root_certificate {
      name = "ReportStream-VPN-Root"

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
}
