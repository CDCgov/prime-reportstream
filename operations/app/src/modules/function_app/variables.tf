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

variable "location" {
    type = string
    description = "Function App Location"
}

variable "storage_account_name" {
    type = string
    description = "Storage Account Name"
}

variable "storage_account_key" {
    type = string
    description = "Storage Account Key"
    sensitive = true
}

variable "public_subnet_id" {
    type = string
    description = "Public Subnet ID"
}

variable "postgres_user" {
    type = string
    description = "Database Server Username"
    sensitive = true
}

variable "postgres_password" {
    type = string
    description = "Database Server Password"
    sensitive = true
}

variable "postgres_url" {
    type = string
    description = "Database Server URL"
}

variable "az_phd_user" {
    type = string
    description = "AZ Public Health Department Username"
    sensitive = true
}

variable "az_phd_password" {
    type = string
    description = "AZ Public Health Department Password"
    sensitive = true
}

variable "redox_secret" {
    type = string
    description = "Redox Secret"
    sensitive = true
}

variable "okta_client_id" {
    type = string
    description = "Okta Client ID"
    sensitive = true
}

variable "login_server" {
    type = string
    description = "Container Registry Login Server"
}

variable "admin_user" {
    type = string
    description = "Container Registry Admin Username"
    sensitive = true
}

variable "admin_password" {
    type = string
    description = "Container Registry Admin Password"
    sensitive = true
}

variable "ai_instrumentation_key" {
    type = string
    description = "Application Insights Instrumentation Key"
    sensitive = true
}

variable "okta_redirect_url" {
    type = string
    description = "Okta Redirect URL"
}
