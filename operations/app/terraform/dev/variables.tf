## Set basic variables
variable "environment"{
    default = "dev"
}
variable "resource_group"{
    default = "matt-test-rg"
}

variable "resource_prefix"{
    default = "mla"
}
variable "location"{
    default = "eastus"
}
variable "rsa_key_2048"{
    default = null 
}              
variable "rsa_key_4096"{
    default = null
}            
variable "is_metabase_env"{
    default = false 
}         
variable "https_cert_names"{
    default = []
}         
variable "okta_redirect_url"{
    default = "https://prime-data-hub-rkh5012.azurefd.net/download"
}         
variable "aad_object_keyvault_admin"{
    default = "f94409a9-12b1-4820-a1b6-e3e0a4fa282d"
}  # Group or individual user id

##################
## App Service Plan Vars
##################

variable "app_tier" {
  default = "Free"
}

variable "app_size" {
  default = "F1"
}

##################
## KeyVault Vars
##################

variable "use_cdc_managed_vnet" {
  default = false
}

variable "terraform_caller_ip_address" {
  default = "162.224.209.174"
}