# https://github.com/hashicorp/terraform-provider-azurerm/tree/main/examples/virtual-machines/windows/vm-joined-to-active-directory

resource "random_password" "domain_password" {
  length  = 16
  special = false
  upper   = true
  lower   = true
  numeric = true
}

resource "azurerm_virtual_network" "example" {
  name                = join("-", [var.prefix, "network"])
  location            = var.common.location
  address_space       = ["10.0.0.0/16"]
  resource_group_name = var.common.resource_group.name
  dns_servers         = ["10.0.1.4", "8.8.8.8"]
}

resource "azurerm_subnet" "domain-controllers" {
  name                 = "domain-controllers"
  address_prefixes     = ["10.0.1.0/24"]
  resource_group_name  = var.common.resource_group.name
  virtual_network_name = azurerm_virtual_network.example.name
}

resource "azurerm_subnet" "domain-members" {
  name                 = "domain-members"
  address_prefixes     = ["10.0.2.0/24"]
  resource_group_name  = var.common.resource_group.name
  virtual_network_name = azurerm_virtual_network.example.name
}


resource "azurerm_network_interface" "dc_nic" {
  name                = join("-", [var.prefix, "dc-primary"])
  location            = var.common.location
  resource_group_name = var.common.resource_group.name
  ip_configuration {
    name                          = "primary"
    private_ip_address_allocation = "Static"
    private_ip_address            = "10.0.1.4"
    subnet_id                     = azurerm_subnet.domain-controllers.id
  }
}

resource "azurerm_windows_virtual_machine" "domain-controller" {
  name                = local.virtual_machine_name
  resource_group_name = var.common.resource_group.name
  location            = var.common.location
  size                = "Standard_B2s"
  admin_username      = "${var.prefix}admin"
  admin_password      = random_password.domain_password.result
  custom_data         = local.custom_data

  network_interface_ids = [
    azurerm_network_interface.dc_nic.id,
  ]

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
  }

  source_image_reference {
    publisher = "MicrosoftWindowsServer"
    offer     = "WindowsServer"
    sku       = "2016-Datacenter"
    version   = "latest"
  }

  additional_unattend_content {
    content = local.auto_logon_data
    setting = "AutoLogon"
  }

  additional_unattend_content {
    content = local.first_logon_data
    setting = "FirstLogonCommands"
  }
}

resource "azurerm_virtual_machine_extension" "create-ad-forest" {
  name                 = "create-active-directory-forest"
  virtual_machine_id   = azurerm_windows_virtual_machine.domain-controller.id
  publisher            = "Microsoft.Compute"
  type                 = "CustomScriptExtension"
  type_handler_version = "1.9"
  settings             = <<SETTINGS
  {
    "commandToExecute": "powershell.exe -Command \"${local.powershell_command}\""
  }
SETTINGS
}