# Database module for external database lookups
# This module uses data sources to look up existing PostgreSQL servers
# managed outside of this Terraform configuration

locals {
  # This module looks up existing external PostgreSQL resources
  module_initialized = true
}