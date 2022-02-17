environment     = "test"
resource_group  = "prime-data-hub-test"
resource_prefix = "pdhtest"
location        = "eastus"
rsa_key_2048    = "pdhtest-2048-key"
rsa_key_4096    = "pdhtest-key"
https_cert_names = [
  "test-prime-cdc-gov",
  "test-reportstream-cdc-gov",
]
okta_base_url             = "hhs-prime.oktapreview.com"
okta_redirect_url         = "https://test.prime.cdc.gov/download"
aad_object_keyvault_admin = "3c17896c-ff94-4298-a719-aaac248aa2c8" # CT-PRIMEDATAHUBTST-DMZ-AZ-Contributor
use_cdc_managed_vnet      = false
