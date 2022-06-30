variable "environment" {
  description = "Name of environment. (demo1, demo2, etc.)"
}
variable "address_id" {
  description = "Address space partial unique to environment"
}
variable "terraform_caller_ip_address" {
  type        = list(string)
  description = "Public IP address of users and runners that need access"
  default     = ["162.224.209.174", "24.163.118.70", "75.191.122.59", "108.48.23.191"]
}
