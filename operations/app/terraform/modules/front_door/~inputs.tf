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
  description = "Function App Location"
}

variable "https_cert_names" {
  type        = list(string)
  description = "The HTTPS cert to associate with the front door. Omitting will not associate a domain to the front door."
}

variable "is_metabase_env" {
  type        = bool
  description = "Should Metabase be deployed in this environment"
}

variable "public_primary_web_endpoint" {}
variable "application_key_vault_id" {}
