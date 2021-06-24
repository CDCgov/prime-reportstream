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
    description = "App Service Location"
}

variable "ai_instrumentation_key" {
    type = string
    description = "Application Insights Instrumentation Key"
    sensitive = true
}
