environment       = "test"
resource_group    = "prime-data-hub-test"
resource_prefix   = "pdhtest"
location          = "eastus"
rsa_key_2048      = "pdhtest-2048-key"
rsa_key_4096      = "pdhtest-key"
https_cert_names  = [
  "test-prime-cdc-gov",
  "test-reportstream-cdc-gov"]
okta_redirect_url = "https://test.prime.cdc.gov/download"
