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
