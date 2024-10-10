#================    Existing Resources    ================
data "azurerm_resource_group" "existing-rg" {
  name = var.existing-vnet-resource-group
}

data "azurerm_virtual_network" "existing-vnet" {
  name                = var.existing-vnet-name
  resource_group_name = data.azurerm_resource_group.existing-rg.name
}

#================    Subnets    ================
resource "azurerm_subnet" "container-subnet" {
  name                 = var.container-subnet-name
  address_prefixes     = var.container-subnet-prefix
  resource_group_name  = data.azurerm_resource_group.existing-rg.name
  virtual_network_name = data.azurerm_virtual_network.existing-vnet.name
  service_endpoints    = ["Microsoft.Storage"]
  delegation {
    name = "delegation"
    service_delegation {
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
      name    = "Microsoft.ContainerInstance/containerGroups"
    }
  }
}

resource "azurerm_subnet" "relay-subnet" {
  name                                           = var.relay-subnet-name
  address_prefixes                               = var.relay-subnet-prefix
  resource_group_name                            = data.azurerm_resource_group.existing-rg.name
  virtual_network_name                           = data.azurerm_virtual_network.existing-vnet.name
  enforce_private_link_endpoint_network_policies = true  #true = Disable; false = Enable
  enforce_private_link_service_network_policies  = false #true = Disable; false = Enable
  depends_on = [
    azurerm_subnet.container-subnet
  ]
}

#================    Network Profile    ================
resource "azurerm_network_profile" "network-profile" {
  name                = "aci-networkProfile-${data.azurerm_resource_group.existing-rg.location}"
  resource_group_name = data.azurerm_resource_group.existing-rg.name
  location            = data.azurerm_resource_group.existing-rg.location
  container_network_interface {
    name = "eth-${azurerm_subnet.container-subnet.name}"
    ip_configuration {
      name      = "ipconfig-${azurerm_subnet.container-subnet.name}"
      subnet_id = azurerm_subnet.container-subnet.id
    }
  }
  tags = var.tags
  lifecycle { ignore_changes = [tags] }
  depends_on = [
    azurerm_subnet.container-subnet
  ]
}

#================    Relay Namespace   ================
resource "azurerm_relay_namespace" "relay-namespace" {
  name                = var.relay-namespace-name # must be unique
  resource_group_name = data.azurerm_resource_group.existing-rg.name
  location            = data.azurerm_resource_group.existing-rg.location
  sku_name            = "Standard"
  tags                = var.tags
  lifecycle { ignore_changes = [tags] }
}

#================    Role Assignments    ================
resource "random_uuid" "network" {
}

resource "random_uuid" "contributor" {
}

data "azurerm_role_definition" "networkRoleDefinition" {
  role_definition_id = "4d97b98b-1d4f-4787-a291-c67834d212e7"
}

data "azurerm_role_definition" "contributorRoleDefinition" {
  role_definition_id = "b24988ac-6180-42a0-ab88-20f7382dd24c"
}

resource "azurerm_role_assignment" "role-assignment-network" {
  scope                = azurerm_network_profile.network-profile.id
  role_definition_name = data.azurerm_role_definition.networkRoleDefinition.name
  principal_id         = var.ACI-OID
  depends_on = [
    azurerm_network_profile.network-profile
  ]
}

resource "azurerm_role_assignment" "role-assignment-contributor" {
  scope                = azurerm_relay_namespace.relay-namespace.id
  role_definition_name = data.azurerm_role_definition.contributorRoleDefinition.name
  principal_id         = var.ACI-OID
  depends_on = [
    azurerm_relay_namespace.relay-namespace
  ]
}

#================    Private Endpoints    ================
resource "azurerm_private_endpoint" "private-endpoint" {
  name                = var.private-endpoint-name
  resource_group_name = data.azurerm_resource_group.existing-rg.name
  location            = data.azurerm_resource_group.existing-rg.location
  subnet_id           = azurerm_subnet.relay-subnet.id
  private_service_connection {
    name                           = "${data.azurerm_virtual_network.existing-vnet.location}-privsvc" # Max Length 80 characters
    private_connection_resource_id = azurerm_relay_namespace.relay-namespace.id
    is_manual_connection           = false
    subresource_names              = ["namespace"]
  }
  tags = var.tags
  lifecycle { ignore_changes = [tags] }
  depends_on = [
    azurerm_relay_namespace.relay-namespace,
    azurerm_subnet.relay-subnet
  ]
}

#================    Private DNS    ================
resource "azurerm_private_dns_zone" "global-private-dns-zone" {
  name                = "privatelink.servicebus.windows.net"
  resource_group_name = data.azurerm_resource_group.existing-rg.name
  tags                = var.tags
  lifecycle { ignore_changes = [tags] }
}

resource "azurerm_private_dns_zone_virtual_network_link" "dns-zone-link" {
  name                  = azurerm_relay_namespace.relay-namespace.name
  resource_group_name   = data.azurerm_resource_group.existing-rg.name
  private_dns_zone_name = "privatelink.servicebus.windows.net"
  virtual_network_id    = data.azurerm_virtual_network.existing-vnet.id
  depends_on            = [azurerm_private_dns_zone.global-private-dns-zone]
}

resource "azurerm_private_dns_a_record" "ussc-dns-a-record" {
  name                = azurerm_relay_namespace.relay-namespace.name
  zone_name           = azurerm_private_dns_zone.global-private-dns-zone.name
  resource_group_name = data.azurerm_resource_group.existing-rg.name
  ttl                 = 3600
  records             = [cidrhost(var.relay-subnet-prefix[0], 4)]
  depends_on          = [azurerm_private_dns_zone.global-private-dns-zone]
}

#================    Storage    ================
resource "azurerm_storage_account" "storageaccount" {
  name                            = var.storageaccount-name
  resource_group_name             = data.azurerm_resource_group.existing-rg.name
  location                        = data.azurerm_resource_group.existing-rg.location
  account_tier                    = "Standard"
  account_replication_type        = "LRS"
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false
  network_rules {
    default_action             = "Deny"
    virtual_network_subnet_ids = [azurerm_subnet.container-subnet.id]
  }

  tags = merge(var.tags, { ms-resource-usage = "azure-cloud-shell" })
  lifecycle { ignore_changes = [tags] }
}