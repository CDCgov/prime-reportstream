## Set basic variables
variable "terraform_object_id" {
  type        = string
  description = "Object id of user running TF"
  default     = "4d81288c-27a3-4df8-b776-c9da8e688bc7"
}
variable "tf_secrets_vault" {
  default = "pdhstaging-keyvault"
}
variable "environment" {
  default = "staging"
}
variable "resource_group" {
  default = "prime-data-hub-staging"
}
variable "resource_prefix" {
  default = "pdhstaging"
}
variable "location" {
  default = "eastus"
}
variable "rsa_key_2048" {
  default = null
}
variable "rsa_key_4096" {
  default = null
}
variable "is_metabase_env" {
  default = true
}
variable "https_cert_names" {
  default = ["staging-prime-cdc-gov", "staging-reportstream-cdc-gov"]
}
variable "okta_base_url" {
  default = "hhs-prime.oktapreview.com"
}
variable "okta_redirect_url" {
  default = "https://staging.prime.cdc.gov/download"
}
variable "aad_object_keyvault_admin" {
  default = "b35a2a63-aeb2-438c-913b-bebeb821adfe"
} # Group or individual user id

###################
## Network Variables
###################

variable "network" {
  description = "The map that describes all of our networking. Orders are important for subnets."
  default = {
    "East-vnet" = {
      "address_space"           = "172.17.6.0/25"
      "dns_servers"             = ["172.17.0.135"]
      "location"                = "East Us"
      "nsg_prefix"              = "eastus-"
      "network_security_groups" = ["private", "public", "container", "endpoint"]
      "subnets"                 = ["public", "private", "container", "endpoint"]
      "subnet_cidrs" = [
        {
          name     = "public"
          new_bits = 3
        },
        {
          name     = "container"
          new_bits = 3
        },
        {
          name     = "private"
          new_bits = 3
        },
        {
          name     = "endpoint"
          new_bits = 2
        },
      ]
    },
    "West-vnet" = {
      "address_space"           = "172.17.6.128/25"
      "dns_servers"             = ["172.17.0.135"]
      "location"                = "West Us"
      "subnets"                 = ["public", "private", "container"]
      "nsg_prefix"              = "westus-"
      "network_security_groups" = ["private", "public", "container"]
      "subnet_cidrs" = [
        {
          name     = "public"
          new_bits = 3
        },
        {
          name     = "container"
          new_bits = 3
        },
        {
          name     = "private"
          new_bits = 3
        },
        {
          name     = "endpoint"
          new_bits = 2
        },
      ]
    },
    "vnet" = {
      "address_space"           = "10.0.0.0/16"
      "dns_server"              = [""]
      "location"                = "East Us"
      "subnets"                 = ["public", "private", "container", "endpoint", "GatewaySubnet"]
      "nsg_prefix"              = ""
      "network_security_groups" = ["public", "private", "container"]
      "subnet_cidrs" = [
        {
          name     = "GatewaySubnet"
          new_bits = 8
        },
        {
          name     = "public"
          new_bits = 8
        },
        {
          name     = "container"
          new_bits = 8
        },
        {
          name     = "private"
          new_bits = 8
        },
        {
          name     = "unused"
          new_bits = 8
        },
        {
          name     = "endpoint"
          new_bits = 8
        }
      ]
    },
    "vnet-peer" = {
      "address_space"           = "10.1.0.0/16"
      "dns_servers"             = [""]
      "location"                = "West Us"
      "subnets"                 = ["private", "endpoint"]
      "nsg_prefix"              = ""
      "network_security_groups" = [""]
      "subnet_cidrs" = [
        {
          name     = "public"
          new_bits = 3
        },
        {
          name     = "container"
          new_bits = 3
        },
        {
          name     = "private"
          new_bits = 3
        },
        {
          name     = "endpoint"
          new_bits = 2
        },
      ]
    }
  }
}
variable "dns_vnet" {
  description = "VNET to use for DNS entries"
  default     = "East-vnet"
}

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
  default = true
}
variable "app_config_kv_name" {
  default     = "pdhstaging-appconfig"
  description = "The keyvault used for application specific secrets."
}
variable "application_kv_name" {
  default     = "pdhstaging-keyvault"
  description = "The keyvault used for the entire application as a whole."
}
variable "client_config_kv_name" {
  default = "pdhstaging-clientconfig"
}
variable "terraform_caller_ip_address" {
  type    = list(string)
  default = ["162.224.209.174", "24.163.118.70", "75.191.122.59"]
}

##########
## Function App Vars
##########

variable "dns_ip" {
  type        = string
  description = "IP address for function app WEBSITE_DNS_SERVER setting"
  default     = "172.17.0.135"
}

##########
## DB Vars
##########

variable "db_sku_name" {
  default = "GP_Gen5_16"
}
variable "db_version" {
  default = "11"
}
variable "db_storage_mb" {
  default = 566272
}
variable "db_auto_grow" {
  default = true
}
variable "db_prevent_destroy" {
  default = false
}
variable "db_threat_detection" {
  default = true
}
variable "db_replica" {
  default = true
}
variable "aad_group_postgres_admin" {
  type        = string
  description = "Azure Active Directory group id containing postgres db admins"
  default     = "c4031f1f-229c-4a8a-b3b9-23bae9dbf197"
}
