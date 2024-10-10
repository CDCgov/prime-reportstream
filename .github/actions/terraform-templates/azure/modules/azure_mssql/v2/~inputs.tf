variable "common" {}
variable "key" {}
variable "sqladmins" {}
variable "databases" {
  type = list(string)
}
variable "epool" {}
variable "admin_username" {}
variable "admin_password" {
  sensitive = true
}
