variable "common" {}
variable "key" {}
variable "storage_account" {
  default = null
}
variable "image" {}
variable "cpu_cores" {}
variable "mem_gb" {}
variable "commands" {
  default = []
}
variable "exec" {
  default = ""
}
variable "repos" {
  default = {}
}
variable "shares" {
  default = {}
}
variable "os_type" {
  default = "Linux"
}
variable "user_password" {
  sensitive = true
  default   = null
}
