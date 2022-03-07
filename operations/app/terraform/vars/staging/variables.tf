## Set basic variables
variable "terraform_object_id" {
  type = string
  description = "Object id of user running TF"
  default = ""
}

variable "tf_secrets_vault" {
  default = "pdhstaging-keyvault"
}

###################
## VNET Variables
###################

variable "east_address_space" {
  type = list
  description = "East VNET Address Space CIDR"
  default = ["172.17.6.0/25"]
}

variable "east_dns_servers" {
  type = list
  description = "List of DNS Servers"
  default = ["172.17.0.135"]
}

variable "west_address_space" {
  type = list
  description = "West VNET Address Space CIDR"
  default = ["172.17.6.128/25"]
}

variable "west_dns_servers" {
  type = list
  description = "List of DNS Servers"
  default = ["172.17.0.135"]
}

variable "vnet_address_space" {
  type = list
  description = "VNET Address Space CIDR"
  default = ["10.0.0.0/16"]
}

variable "vnet_peer_address_space" {
  type = list
  description = "VNET Address Space CIDR"
  default = ["10.1.0.0/16"]
}




variable "environment"{
    default = "staging"
}
variable "resource_group"{
    default = "prime-data-hub-staging"
}

variable "resource_prefix"{
    default = "pdhstaging"
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
  default = "PremiumV2"
}

variable "app_size" {
  default = "P3v2"
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

#############################



variable "aad_group_postgres_admin" {
  type        = string
  description = "Azure Active Directory group id containing postgres db admins"
  default     = "f94409a9-12b1-4820-a1b6-e3e0a4fa282d"
}


##########
## DB Vars
##########

variable "db_sku_name" {
  default = "GP_Gen5_2"
}
variable "db_version" {
  default = "11"
}
variable "db_storage_mb" {
  default = 5120
}
variable "db_auto_grow" {
  default = false
}
variable "db_prevent_destroy" {
  default = false
}

variable "db_threat_detection" {
  default = false
}

variable "db_replica" {
  default = true
}
