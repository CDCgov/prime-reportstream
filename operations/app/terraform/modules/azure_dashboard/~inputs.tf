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
  description = "Function App Location"
}

# variable "dashboards" {
#   description = "List of Dashboard definitions."
#   type = list(object({
#     name           = string
#     json_file_path = string
#     variables      = map(string)
#   }))
#   default = []
# }

# variable "tags" {
#   description = "A map of tags to add to all resources"
#   type        = map(string)
#   default     = {}
# }