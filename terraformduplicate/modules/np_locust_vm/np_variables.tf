variable "vm_name" {
  description = "Name of the virtual machine"
  type        = string
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
  default     = "ddphss-prim-prd-moderate-rg"
}

variable "location" {
  description = "Azure region where resources will be created"
  type        = string
}

variable "vm_size" {
  description = "Size of the virtual machine"
  type        = string
  default     = "Standard_B2s"
}

variable "admin_username" {
  description = "Username for the virtual machine"
  type        = string
  default     = "azureuser"
}

variable "ssh_public_key" {
  description = "SSH public key for the virtual machine"
  type        = string
}

variable "subnet_id" {
  description = "ID of the subnet where the VM will be deployed"
  type        = string
}

variable "locust_config" {
  description = "Configuration for Locust test scenarios"
  type = object({
    target_host = string
    users       = number
    spawn_rate  = number
    run_time    = string
  })
  default = {
    target_host = "http://localhost:8080"
    users       = 100
    spawn_rate  = 10
    run_time    = "5m"
  }
} 