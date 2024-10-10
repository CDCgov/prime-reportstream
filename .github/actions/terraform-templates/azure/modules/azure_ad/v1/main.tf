# https://schnerring.net/blog/set-up-azure-active-directory-domain-services-aadds-with-terraform-updated/
# service principal for Domain Controller Services
resource "azuread_service_principal" "aadds" {
  client_id = "2565bd9d-da50-47d4-8b85-4c97f669dc36"
}

# Microsoft.AAD Resource Provider Registration
resource "azurerm_resource_provider_registration" "aadds" {
  name = "Microsoft.AAD"
}

# DC Admin Group and User
resource "azuread_group" "dc_admins" {
  display_name     = "AAD DC Administrators"
  description      = "AADDS Administrators"
  members          = [azuread_user.dc_admin.object_id]
  security_enabled = true
}

resource "random_password" "dc_admin" {
  length = 64
}

resource "azuread_user" "dc_admin" {
  user_principal_name = "dc-admin@${var.domain_name}"
  display_name        = "AADDS DC Administrator"
  password            = random_password.dc_admin.result
}

# Resource Group for Azure Active Directory Domain Services (AADDS)
resource "azurerm_resource_group" "aadds" {
  name     = "aadds-${var.key}-rg"
  location = "East US"
}

# Network Resources
resource "azurerm_virtual_network" "aadds" {
  name                = "aadds-vnet"
  location            = azurerm_resource_group.aadds.location
  resource_group_name = azurerm_resource_group.aadds.name
  address_space       = ["10.0.0.0/16"]
}

resource "azurerm_subnet" "aadds" {
  name                 = "aadds-snet"
  resource_group_name  = azurerm_resource_group.aadds.name
  virtual_network_name = azurerm_virtual_network.aadds.name
  address_prefixes     = ["10.0.0.0/24"]
}

resource "azurerm_network_security_group" "aadds" {
  name                = "aadds-nsg"
  location            = azurerm_resource_group.aadds.location
  resource_group_name = azurerm_resource_group.aadds.name

  security_rule {
    name                       = "AllowRD"
    priority                   = 201
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "3389"
    source_address_prefix      = "CorpNetSaw"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "AllowPSRemoting"
    priority                   = 301
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "5986"
    source_address_prefix      = "AzureActiveDirectoryDomainServices"
    destination_address_prefix = "*"
  }

  /*
  security_rule {
    name                       = "AllowLDAPS"
    priority                   = 401
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "636"
    source_address_prefix      = "<Authorized LDAPS IPs>"
    destination_address_prefix = "*"
  }
  */
}

resource azurerm_subnet_network_security_group_association "aadds" {
  subnet_id                 = azurerm_subnet.aadds.id
  network_security_group_id = azurerm_network_security_group.aadds.id
}

# AADDS Managed Domain
resource "azurerm_active_directory_domain_service" "aadds" {
  name                = "aadds"
  location            = azurerm_resource_group.aadds.location
  resource_group_name = azurerm_resource_group.aadds.name
  domain_configuration_type = "FullySynced"

  domain_name           = var.domain_name
  sku                   = "Standard"

  initial_replica_set {
    subnet_id = azurerm_subnet.aadds.id
  }

  notifications {
    additional_recipients = ["josiah0601@gmail.com"]
    notify_dc_admins      = true
    notify_global_admins  = true
  }

  security {
    sync_kerberos_passwords = true
    sync_ntlm_passwords     = true
    sync_on_prem_passwords  = true
  }

  depends_on = [
    azuread_service_principal.aadds,
    azurerm_resource_provider_registration.aadds,
    azurerm_subnet_network_security_group_association.aadds,
  ]
}
