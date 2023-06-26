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
  description = "Storage Account Location"
}

variable "rsa_key_4096" {
  type        = string
  description = "Name of the 2048 length RSA key in the Key Vault. Omitting will use Azure-managed key instead of a customer-key."
}

variable "terraform_caller_ip_address" {
  type        = list(string)
  description = "The IP address of the Terraform script caller. This IP will have already been whitelisted; it's inclusion is to prevent its removal during terraform apply calls."
  sensitive   = true
}

variable "use_cdc_managed_vnet" {
  type        = bool
  description = "If the environment should be deployed to the CDC managed VNET"
}

variable "application_key_vault_id" {}
variable "dns_vnet" {}

variable "subnets" {
  description = "A set of all available subnet combinations"
}

variable "dns_zones" {
  description = "A set of all available dns zones"
}

variable "delete_pii_storage_after_days" {
  description = "Number of days after which we'll delete PII-related blobs from the storage account"
}

variable "is_temp_env" {
  default     = false
  description = "Is a temporary environment. true or false"
}

variable "storage_queue_name" {
  description = "Default storage queue names that will be created in the storage account."
  type        = list(string)
  default     = ["process", "batch", "batch-poison", "process-poison", "send", "send-poison"]

}