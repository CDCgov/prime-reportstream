environment     = "staging"
resource_group  = "prime-data-hub-staging"
resource_prefix = "pdhstaging"
location        = "eastus"
rsa_key_2048    = "pdhstaging-2048-key"
rsa_key_4096    = "pdhstaging-4096-key"
https_cert_names = [
  "staging-prime-cdc-gov",
  "staging-reportstream-cdc-gov",
]
okta_base_url             = "hhs-prime.oktapreview.com"
okta_redirect_url         = "https://staging.prime.cdc.gov/download"
aad_object_keyvault_admin = "b35a2a63-aeb2-438c-913b-bebeb821adfe" # CT-PRIMEDATAHUBSTG-DMZ-AZ-Contributor

use_cdc_managed_vnet = true