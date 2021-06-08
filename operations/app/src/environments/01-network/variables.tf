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
  description = "Azure Data Center"
}

variable "rsa_key_2048" {
  type        = string
  description = "Key Vault certificate name for encrypting Azure resources at RSA-2048"
}

variable "rsa_key_4096" {
  type        = string
  description = "Key Vault certificate name for encrypting Azure resources at RSA-4096"
}

variable "aad_group_postgres_admin" {
  type        = string
  description = "Azure Active Directory group id containing postgres db admins"
  default     = "c4031f1f-229c-4a8a-b3b9-23bae9dbf197"
}

variable "is_metabase_env" {
  type        = bool
  description = "If Metabase will be deployed in the environment"
  default     = true
}

variable "https_cert_names" {
  type = list(string)
  description = "List of SSL certs to associate with the Front Door"
}

variable "okta_redirect_url" {
  type = string
  description = "URL to redirect to after Okta login"
}