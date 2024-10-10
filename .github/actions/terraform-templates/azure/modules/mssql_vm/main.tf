data "http" "icanhazip" {
  url = "http://icanhazip.com"
}

locals {
  public_ip = chomp(data.http.icanhazip.response_body)
}

resource "azurerm_virtual_network" "example" {
  name                = "${var.key}-VN"
  address_space       = ["10.1.0.0/16"]
  location            = var.common.location
  resource_group_name = var.common.resource_group.name

  dns_servers = var.ad_dns_ips
}

resource "azurerm_network_security_group" "example" {
  name                = "${var.key}-NSG"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name
}

resource "azurerm_subnet" "example" {
  name                 = "${var.key}-SN"
  resource_group_name  = var.common.resource_group.name
  virtual_network_name = azurerm_virtual_network.example.name
  address_prefixes     = ["10.1.0.0/24"]
}

/*resource "azurerm_subnet_network_security_group_association" "example" {
  subnet_id                 = azurerm_subnet.example.id
  network_security_group_id = azurerm_network_security_group.example.id
}*/

resource "azurerm_public_ip" "vm" {
  for_each = toset([ "vm1", "vm2" ])

  name                = "${var.key}-${each.key}-PIP"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name
  allocation_method   = "Dynamic"
}

resource "azurerm_public_ip" "lb" {
  name                = "${var.key}-lb-PIP"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name
  allocation_method   = "Dynamic"
}

resource "azurerm_lb" "example" {
  name                = "TestLoadBalancer"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name

  frontend_ip_configuration {
    name                 = "PublicIPAddress"
    public_ip_address_id = azurerm_public_ip.lb.id
  }
}

resource "azurerm_network_security_rule" "RDPRule" {
  name                        = "RDPRule"
  resource_group_name         = var.common.resource_group.name
  priority                    = 1000
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = 3389
  source_address_prefix       = "${local.public_ip}"
  destination_address_prefix  = "*"
  network_security_group_name = azurerm_network_security_group.example.name
}

resource "azurerm_network_security_rule" "MSSQLRule" {
  name                        = "MSSQLRule"
  resource_group_name         = var.common.resource_group.name
  priority                    = 1001
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = 1433
  source_address_prefix       = "${local.public_ip}"
  destination_address_prefix  = "*"
  network_security_group_name = azurerm_network_security_group.example.name
}

resource "azurerm_network_interface" "example" {
  for_each = toset([ "vm1", "vm2" ])

  name                = "${var.key}-${each.key}-NIC"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name

  ip_configuration {
    name                          = "exampleconfiguration1"
    subnet_id                     = azurerm_subnet.example.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.vm[each.key].id
  }
}

resource "azurerm_network_interface_security_group_association" "example" {
  for_each = toset([ "vm1", "vm2" ])

  network_interface_id      = azurerm_network_interface.example[each.key].id
  network_security_group_id = azurerm_network_security_group.example.id
}

resource "azurerm_windows_virtual_machine" "example" {
  for_each = toset([ "vm1", "vm2" ])

  name                  = "${var.key}-${each.key}-VM"
  location              = var.common.location
  resource_group_name   = var.common.resource_group.name
  network_interface_ids = [azurerm_network_interface.example[each.key].id]
  size               = "Standard_B2s"
  provision_vm_agent       = true
  enable_automatic_updates = true

  admin_username = "exampleadmin"
  admin_password = "Password1234!"
  
  source_image_reference {
    publisher = "MicrosoftSQLServer"
    offer     = "SQL2022-WS2022" #"SQL2017-WS2016"
    sku       = "sqldev-gen2" #"SQLDEV"
    version   = "latest"
  }

  os_disk {
    name              = "${var.key}-${each.key}-OSDisk"
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
  }

}

// Waits for up to 1 hour for the Domain to become available. Will return an error 1 if unsuccessful preventing the member attempting to join.
// todo - find out why this is so variable? (approx 40min during testing)

resource "azurerm_virtual_machine_extension" "wait-for-domain-to-provision" {
  for_each = toset([ "vm1", "vm2" ])

  name                 = "TestConnectionDomain"
  publisher            = "Microsoft.Compute"
  type                 = "CustomScriptExtension"
  type_handler_version = "1.9"
  virtual_machine_id   = azurerm_windows_virtual_machine.example[each.key].id
  settings             = <<SETTINGS
  {
    "commandToExecute": "powershell.exe -Command \"while (!(Test-Connection -ComputerName ${var.domain_name} -Count 1 -Quiet) -and ($retryCount++ -le 360)) { Start-Sleep 10 } \""
  }
SETTINGS
}

resource "azurerm_virtual_machine_extension" "join-domain" {
  for_each = toset([ "vm1", "vm2" ])

  name                 = azurerm_windows_virtual_machine.example[each.key].name
  publisher            = "Microsoft.Compute"
  type                 = "JsonADDomainExtension"
  type_handler_version = "1.3"
  virtual_machine_id   = azurerm_windows_virtual_machine.example[each.key].id

  settings = <<SETTINGS
    {
        "Name": "${var.domain_name}",
        "OUPath": "",
        "User": "${var.ad_username}@${var.domain_name}",
        "Restart": "true",
        "Options": "3"
    }
SETTINGS

  protected_settings = <<SETTINGS
    {
        "Password": "${var.ad_password}"
    }
SETTINGS

  depends_on = [azurerm_virtual_machine_extension.wait-for-domain-to-provision]
}

resource "azurerm_mssql_virtual_machine_group" "example" {
  name                = "examplegroup"
  resource_group_name = var.common.resource_group.name
  location            = var.common.location

  sql_image_offer = "SQL2022-WS2022" #"SQL2017-WS2016"
  sql_image_sku   = "Developer"

  wsfc_domain_profile {
    fqdn                = var.domain_name
    cluster_bootstrap_account_name = "install@${var.domain_name}"
    cluster_operator_account_name  = "install@${var.domain_name}"
    sql_service_account_name       = "sqlsvc@${var.domain_name}"
    cluster_subnet_type = "SingleSubnet"
  }
}

resource "azurerm_mssql_virtual_machine" "example" {
  for_each = toset([ "vm1", "vm2" ])

  virtual_machine_id           = azurerm_windows_virtual_machine.example[each.key].id
  sql_license_type             = "PAYG"
  sql_virtual_machine_group_id = azurerm_mssql_virtual_machine_group.example.id

  wsfc_domain_credential {
    cluster_bootstrap_account_password = "P@ssw0rd1234!"
    cluster_operator_account_password  = "P@ssw0rd1234!"
    sql_service_account_password       = "P@ssw0rd1234!"
  }

  depends_on = [ azurerm_virtual_machine_extension.join-domain ]
}

resource "azurerm_mssql_virtual_machine_availability_group_listener" "example" {
  name                         = "listener1"
  availability_group_name      = "availabilitygroup1"
  port                         = 1433
  sql_virtual_machine_group_id = azurerm_mssql_virtual_machine_group.example.id

  load_balancer_configuration {
    load_balancer_id   = azurerm_lb.example.id
    private_ip_address = "10.1.0.11"
    probe_port         = 51572
    subnet_id          = azurerm_subnet.example.id

    sql_virtual_machine_ids = [
      azurerm_mssql_virtual_machine.example["vm1"].id,
      azurerm_mssql_virtual_machine.example["vm2"].id
    ]
  }

  replica {
    sql_virtual_machine_id = azurerm_mssql_virtual_machine.example["vm1"].id
    role                   = "Primary"
    commit                 = "Synchronous_Commit"
    failover_mode          = "Automatic"
    readable_secondary     = "All"
  }

  replica {
    sql_virtual_machine_id = azurerm_mssql_virtual_machine.example["vm2"].id
    role                   = "Secondary"
    commit                 = "Asynchronous_Commit"
    failover_mode          = "Manual"
    readable_secondary     = "No"
  }
}