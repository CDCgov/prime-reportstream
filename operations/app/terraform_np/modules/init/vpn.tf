resource "azurerm_public_ip" "init" {
  name                = "${var.resource_prefix}-vpn-ip"
  location            = var.location
  resource_group_name = var.resource_group

  sku                     = "Basic"
  ip_version              = "IPv4"
  idle_timeout_in_minutes = 4
  allocation_method       = "Dynamic"

  lifecycle {
    prevent_destroy = true
  }
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
      name = "New-ReportStream-VPN-Root"

      public_cert_data = <<EOF
MIIC5jCCAc6gAwIBAgIIN1F5NdWmALIwDQYJKoZIhvcNAQELBQAwETEPMA0GA1UE
AxMGVlBOIENBMB4XDTI0MDMxMTE4MzE0M1oXDTI3MDMxMTE4MzE0M1owETEPMA0G
A1UEAxMGVlBOIENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxO27
Z/wmQKM/zdEzKWgoiFEXQxiz/UWWLRDYnwyROPhbVsJOwu4VIWyQ1u3c4x6JUWpG
DcZdmiBe2Korj9g6Yj0wCm2hsozpdUh2Nux+xyvNfmjFIIT2Q1PvQ/aZx3yVenrT
9vQCkz1OtGe9HjnuA4KlhLCTLO9AxPgHlbD/KH42fl7LgA/ptGcFJlrRzDYsNjWs
GWYbdgQXPUqoUjVU6xAkvhRtqGMC7DkBfjOf/WbP+P+2Usgwoy80mZ/QFxkzDJJr
H5/cdzv6YUy3fNBp0pU5wlSiE2Fa+CmEINGCzt5mN3jnWmP2dM2Myrp5fLlk3M0J
d7gESPdaDfMBagNZ3QIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB
/wQEAwIBBjAdBgNVHQ4EFgQU4UUN3VM5iEzilTKPjZ5aAIb6txQwDQYJKoZIhvcN
AQELBQADggEBADi22wecaYDEdj+xiMlsfw9kP/8btVmRLj5JGoy7Cxn8zT2rVjm1
aZfOgbqiz/kjhrBJbCly+z85vVLwIt82K3BQohyqTi1q8FTBskmrAPJMB7q59wyt
gTPTSzJcjx5g8ayF3B37WBnQazhyH4H2UyD6jIb+alTxfeDYhd+4Z98BSISBUIVc
jQcxJyozeZerzeSpb2jIhsHLGGi3Jge4Kg3mQYLj2b5sjDi5pmBd5LAxMPiMEc3s
JYawVvWptatqBT7vEu7zsc5chqMToTuOd9AKe9boG3b3yAyirh0TPqlx9s36i513
ylNsH14cy0vHXxpwn/u/ORiQ3blCw+u5S2I=
EOF
    }
  }

  depends_on = [
    azurerm_virtual_network.init
  ]

  lifecycle {
    prevent_destroy = true
  }
}
