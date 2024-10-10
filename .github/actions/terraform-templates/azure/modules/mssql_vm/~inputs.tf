variable "common" {}
variable "key" {}
variable "ad_dns_ips" {
    default = []
}
variable "domain_name" {}
variable "ad_username" {
  sensitive = true
}
variable "ad_password" {
  sensitive = true
}