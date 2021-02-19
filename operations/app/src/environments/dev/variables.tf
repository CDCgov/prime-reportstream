variable "dev_name" {
  type = string
  description = "Name of developer (eg. cglodosky, rheft, jduff, etc.)"
}

variable "postgres_user" {
  type = string
  description = "Database Server Username"
  sensitive = true
}

variable "postgres_password" {
  type = string
  description = "Database Server Password"
  sensitive = true
}

variable "az_phd_user" {
    type = string
    description = "AZ Public Health Department Username"
    sensitive = true
}

variable "az_phd_password" {
    type = string
    description = "AZ Public Health Department Password"
    sensitive = true
}

variable "redox_secret" {
    type = string
    description = "Redox Secret"
    sensitive = true
}

variable "okta_client_id" {
    type = string
    description = "Okta Client ID"
    sensitive = true
}
