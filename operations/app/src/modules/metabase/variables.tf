variable "environment" {
    type = string
    description = "Target Environment"
}

variable "resource_group" {
    type = string
    description = "Resource Group Name"
}

variable "name" {
    type = string
    description = "App Service Name"
}

variable "location" {
    type = string
    description = "App Service Location"
}

variable "app_service_plan_id" {
    type = string
    description = "App Service Plan ID"
}

variable "public_subnet_id" {
    type = string
    description = "Public Subnet ID"
}

variable "postgres_url" {
    type = string
    description = "PostgreSQL Connection URL"
    sensitive = true
}

variable "ai_instrumentation_key" {
    type = string
    description = "Application Insights Instrumentation Key"
    sensitive = true
}
