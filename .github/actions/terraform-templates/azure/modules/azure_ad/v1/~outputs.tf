output "dns_ips" {
  value = azurerm_active_directory_domain_service.aadds.initial_replica_set.0.domain_controller_ip_addresses
}
