variable "environment" {
  type        = string
  description = "Target Environment"
}

variable "resource_group" {
  type        = string
  description = "Resource Group Name"
}

variable "resource_prefix" {
  type        = string
  description = "Resource Prefix"
}

variable "location" {
  type        = string
  description = "Container Registry Location"
}

variable "enable_content_trust" {
  type        = bool
  description = "Boolean value indicating enabledness of Content Trust"
}

variable "subnets" {
  description = "A set of all available subnet combinations"
}
