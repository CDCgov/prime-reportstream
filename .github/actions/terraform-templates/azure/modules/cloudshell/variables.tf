variable "existing-vnet-name" {
  type        = string
  description = "the name of the existing virtual network"
}

variable "existing-vnet-resource-group" {
  type        = string
  description = "the name of the resource containing the existing virtual network"
}

variable "relay-namespace-name" {
  type        = string
  description = "the name to be assigned to the relay namespace"
  default     = "cshrelay"
}

variable "ACI-OID" {
  type        = string
  description = "Azure Container Instance OID"
}

variable "container-subnet-name" {
  type        = string
  description = "the name to be assigned to the cloudshell container subnet"
  default     = "cloudshellsubnet"
}

variable "container-subnet-prefix" {
  type        = list(string)
  description = "the list of address prefix(es) to be assigned to the cloudshell container subnet"
}

variable "relay-subnet-name" {
  type        = string
  description = "the name to be assigned to the relay subnet"
  default     = "relaysubnet"
}

variable "relay-subnet-prefix" {
  type        = list(string)
  description = "the list of address prefix(es) to be assigned to the relay subnet"
}

variable "storage-subnet-name" {
  type        = string
  description = "the name to be assigned to the storage subnet"
  default     = "storagesubnet"
}

variable "storageaccount-name" {
  type        = string
  description = "the name of the storage account to create"
}

variable "private-endpoint-name" {
  type        = string
  description = "the name to be assigned to the private endpoint"
  default     = "cloudshellRelayEndpoint"
}

variable "tags" {
  type        = map(string)
  description = "the list of tags to be assigned"
  default     = {}
}