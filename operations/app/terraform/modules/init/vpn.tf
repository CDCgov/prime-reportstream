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
MIIC7jCCAdagAwIBAgIIQIiZV08pG70wDQYJKoZIhvcNAQELBQAwFTETMBEGA1UE
AxMKTmV3IFZQTiBDQTAeFw0yMjA5MjAxODA0NTlaFw0yNTA5MTkxODA0NTlaMBUx
EzARBgNVBAMTCk5ldyBWUE4gQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK
AoIBAQDAuz3lq1HDr8bOKdZ2dPsMtss2wzbR9jsMs4Fd05f2uNSrklHSTKjghEuw
qVYTcqy9INFYwL7sApW4iM+SwigtG+OJL0N4DrVwCGv+nHx1dhE1d/uTexO/p9gP
6bjHns87VJmG6vLp57z8fHrmjhFjUPBUFOXsaxhxPJFI5wNzCUW3ZAzlAGq8WIgR
7i9NSlus+ELiM4vWLrov+DtqALmzoeSVToxNuy9mU0EF+14iAKxkXdpX+Cs++Wf5
rNYWAjV3JjQ7AlFZL2BU5NzTlyCTN2CRJ8lGyMvl7TIv13er6ORWi4HMvG5DojXT
S0D3HG877yhJXhyTMTTMXm1PAIC/AgMBAAGjQjBAMA8GA1UdEwEB/wQFMAMBAf8w
DgYDVR0PAQH/BAQDAgEGMB0GA1UdDgQWBBT+6HfqStyvA3+NEXrAoKu6qQ2kvzAN
BgkqhkiG9w0BAQsFAAOCAQEABMO3J8W83CxWbdBM7lNpmF1b56OMYtDBbuRF99BE
8jt1CSNXknnGFrtSveHWDISvmZzpT89l6QB9kuFzzbcqA3zqGFCLlxBlQ0HYUbQs
k+syJlEOYbzTTWO30cx5n6FcB9jyK8ERLy9a6+zAomj1fDroOLwY6d1s4s2e6+YG
E/QgfZEGDrNkMXifDkvmNayM6HzC/Q7N944xGj5p8gPie6kT+ZYyBmP7IemBQoYn
8/lKFGIlV5wX+K6ZmkQ9WLUaxeyRfF+ZkUgRL+pS1h5sE1ic0tVxTfoG0lfw0JYP
2/Ey8Bzdc511B0imemrqqSDgDlacsHhIjTiVoe+OAINOWQ==
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
