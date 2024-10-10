output "dns_ips" {
  value = ["10.0.1.4", "8.8.8.8"]//azurerm_active_directory_domain_service.aadds.initial_replica_set.0.domain_controller_ip_addresses
}

output "domain_admin_username" {
  value = "${var.prefix}admin"
}

output "domain_admin_password" {
  value = random_password.domain_password.result
}

