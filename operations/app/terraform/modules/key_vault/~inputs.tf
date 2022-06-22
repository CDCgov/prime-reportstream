variable "environment" {}
variable "resource_group" {}
variable "resource_prefix" {}
variable "location" {}
variable "aad_object_keyvault_admin" {}
variable "terraform_caller_ip_address" {
  type = list(string)
}
variable "use_cdc_managed_vnet" {
  type = bool
}
variable "cyberark_ip_ingress" {}
variable "terraform_object_id" {}
variable "app_config_kv_name" {}
variable "application_kv_name" {}
variable "dns_vnet" {}
variable "client_config_kv_name" {}

variable "subnets" {
  description = "A set of all available subnet combinations"
}

variable "dns_zones" {
  description = "A set of all available dns zones"
}

variable "admin_function_app" {
  description = "Admin function app"
}

variable "is_temp_env" {
  default     = false
  description = "Is a temporary environment. true or false"
}
