variable "environment" {}
variable "resource_group" {}
variable "resource_prefix" {}
variable "location" {}
variable "aad_object_keyvault_admin" {}
variable "terraform_caller_ip_address" {}
variable "use_cdc_managed_vnet" {
  type = bool
}
variable "terraform_object_id" {}
variable "app_config_kv_name" {}
variable "application_kv_name" {}
variable "dns_vnet" {}
variable "client_config_kv_name" {}
variable "network" {}
variable "subnets" {
  default     = ""
  description = "A set of all available subnet combinations"
}
variable "random_id" {}
