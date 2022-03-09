## Set basic variables
variable "terraform_object_id" {
  type        = string
  description = "Object id of user running TF"
  default     = "4d81288c-27a3-4df8-b776-c9da8e688bc7"
}

variable "tf_secrets_vault" {
  default = "pdhtest-keyvault"
}

variable "environment" {
  default = "test"
}
variable "resource_group" {
  default = "prime-data-hub-test"
}

variable "resource_prefix" {
  default = "pdhtest"
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
  default = false
}
variable "https_cert_names" {
  default = []
}
variable "okta_redirect_url" {
  default = "https://prime-data-hub-rkh5012.azurefd.net/download"
}
variable "aad_object_keyvault_admin" {
  default = "3c17896c-ff94-4298-a719-aaac248aa2c8"
} # Group or individual user id

###################
## Netowrk Variables
###################

variable "network" {
  description = "The map that describes all of our networking. Orders are important for subnets."
  default = {
    "East-vnet" = {
      "address_space"           = "172.17.5.0/25"
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
      "address_space"           = "172.17.5.128/25"
      "dns_servers"             = ["172.17.0.135"]
      "location"                = "West Us"
      "subnets"                 = ["public", "private", "container", "endpoint"]
      "nsg_prefix"              = "westus-"
      "network_security_groups" = ["private", "public", "container", "endpoint"]
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
      "subnets"                 = ["public", "private", "container", "endpoint"]
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
    }
  }
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
  default = false
}

variable "terraform_caller_ip_address" {
  type    = list(string)
  default = ["162.224.209.174","24.163.118.70","75.191.122.59"]
}


##########
## Function App Vars
##########

variable "dns_ip" {
  type = string
  description = "IP address for function app dns"
  default = "168.63.129.16"
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
  default = 5120
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
  default = false
}

variable "aad_group_postgres_admin" {
  type        = string
  description = "Azure Active Directory group id containing postgres db admins"
  default     = "f94409a9-12b1-4820-a1b6-e3e0a4fa282d"
}
