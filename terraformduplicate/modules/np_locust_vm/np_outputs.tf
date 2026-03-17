output "vm_id" {
  description = "ID of the virtual machine"
  value       = azurerm_linux_virtual_machine.locust_vm.id
}

output "public_ip" {
  description = "Public IP address of the virtual machine"
  value       = azurerm_public_ip.locust_pip.ip_address
}

output "locust_web_url" {
  description = "URL to access the Locust web interface"
  value       = "http://${azurerm_public_ip.locust_pip.ip_address}:8089"
} 