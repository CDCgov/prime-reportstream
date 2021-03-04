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
    description = "App Service Plan Name"
}

variable "location" {
    type = string
    description = "Function App Location"
}
