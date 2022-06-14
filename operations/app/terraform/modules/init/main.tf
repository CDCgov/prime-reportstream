// terraform -chdir=operations/app/terraform/vars/demo init -target=module.init
// terraform -chdir=operations/app/terraform/vars/demo plan -target=module.init

data "azurerm_client_config" "current" {}
