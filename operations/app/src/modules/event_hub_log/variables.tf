variable "resource_type" {
  type = string
  description = "Type of resource to log, ex. front_door"
}

variable "log_type" {
  type = string
  description = "Type of log, ex. access"
}

variable "eventhub_namespace_name" {
  type = string
  description = "Namespace of the event hub"
}

variable "resource_group" {
  type = string
  description = "Resource Group Name"
}

variable "resource_prefix" {
  type = string
  description = "Resource Prefix"
}
