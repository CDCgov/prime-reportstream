environment       = "prod"
resource_group    = "prime-data-hub-prod"
resource_prefix   = "pdhprod"
location          = "eastus"
rsa_key_2048      = "pdhprod-2048-key"
rsa_key_4096      = "pdhprod-4096-key"
https_cert_names  = [
  "prime-cdc-gov",
  "reportstream-cdc-gov"]
okta_redirect_url = "https://prime.cdc.gov/download"