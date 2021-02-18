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
    description = "Storage Account Name"
}

variable "location" {
    type = string
    description = "Storage Account Location"
}

variable "subnet_ids" {
    type = list(string)
    description = "List of VNet Subnet IDs"
}
