// terraform -chdir=operations/app/terraform/vars/demo init -target=module.init -var-file=demo1/env.tfvars.json -backend-config=demo1/env.tfbackend
// terraform -chdir=operations/app/terraform/vars/demo plan -target=module.init -var-file=demo1/env.tfvars.json
// terraform -chdir=operations/app/terraform/vars/demo apply -target=module.init -var-file=demo1/env.tfvars.json
// X2
// terraform -chdir=operations/app/terraform/vars/demo apply -var-file=demo1/env.tfvars.json

data "azurerm_client_config" "current" {}
