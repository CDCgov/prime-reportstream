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

variable "app_service_plan_id" {
    type = string
    description = "App Service Plan ID"
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

variable "endpoint_subnet_id" {
    type = string
    description = "Private Endpoint Subnet ID"
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

variable "app_config_key_vault_id" {
    type = string
    description = "Key Vault used for function app configuration"
}

variable "client_config_key_vault_id" {
    type = string
    description = "Key Vault used for client credential secrets"
}

variable "storage_partner_connection_string" {
    type = string
    description = "Storage account to export data with HHS Protect"
}