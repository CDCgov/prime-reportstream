variable "environment" {
    type = string
    description = "Target Environment"
}

variable "resource_group" {
    type = string
    description = "Resource Group Name"
}

variable "resource_prefix" {
    type = string
    description = "Resource Prefix"
}

variable "https_cert_name" {
    type = string
    description = "The HTTPS cert to associate with the front door. Omitting will not associate a domain to the front door."
}

variable "rsa_key_2048" {
    type = string
    description = "Name of the 2048 length RSA key in the Key Vault. Omitting will use Azure-managed key instead of a customer-key."
}

variable "rsa_key_4096" {
    type = string
    description = "Name of the 2048 length RSA key in the Key Vault. Omitting will use Azure-managed key instead of a customer-key."
}

variable "okta_redirect_url" {
    type = string
    description = "Okta Redirect URL"
}
