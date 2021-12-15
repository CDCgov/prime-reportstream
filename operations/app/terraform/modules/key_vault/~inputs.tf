variable "environment" {
  type        = string
  description = "Target Environment"
}

variable "resource_group" {
  type        = string
  description = "Resource Group Name"
}

variable "resource_prefix" {
  type        = string
  description = "Resource Prefix"
}

variable "location" {
  type        = string
  description = "Network Location"
}

variable "aad_object_keyvault_admin" {
  type        = string
  description = "Azure Active Directory ID for a user or group who will be given write access to Key Vaults"
}

variable "terraform_caller_ip_address" {
  type        = string
  description = "The IP address of the Terraform script caller. This IP will have already been whitelisted; it's inclusion is to prevent its removal during terraform apply calls."
  sensitive   = true
}

variable "use_cdc_managed_vnet" {
  type        = bool
  description = "If the environment should be deployed to the CDC managed VNET"
}


variable "public_subnet" {}

variable "container_subnet" {}

variable "endpoint_subnet" {}

variable "cyberark_ip_ingress" {}
variable "terraform_object_id" {
  
}