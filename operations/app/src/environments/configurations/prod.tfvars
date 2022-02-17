environment     = "prod"
resource_group  = "prime-data-hub-prod"
resource_prefix = "pdhprod"
location        = "eastus"
rsa_key_2048    = "pdhprod-2048-key"
rsa_key_4096    = "pdhprod-4096-key"
https_cert_names = [
  "prime-cdc-gov",
  "reportstream-cdc-gov",
]
okta_base_url             = "hhs-prime.okta.com"
okta_redirect_url         = "https://prime.cdc.gov/download"
aad_object_keyvault_admin = "5c6a951e-a4c2-4890-b62c-0ed8179501bb" # CT-PRIMEDATAHUBPRD-DMZ-AZ-Contributor

use_cdc_managed_vnet = true