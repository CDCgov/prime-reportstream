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

variable "eventhub_namespace_name" {
    type = string
    description = "Event hub to stream logs to"
}

variable "eventhub_manage_auth_rule_id" {
    type = string
    description = "Event Hub Manage Authorization Rule ID"
}