module "pdhprod_serviceplan" {
  source              = "./modules/app_service_plan"
  name                = "pdhprod-serviceplan"
  resource_group_name = "ddphss-prim-prd-moderate-app-vnet"
  location            = "East US"
  kind                = "Linux"
  sku_tier            = "PremiumV2"
  sku_size            = "P3v2"
  reserved            = true
  per_site_scaling    = false
  tags = {
    environment = "prod"
    managed-by  = "terraform"
  }
}

module "pdhstaging_serviceplan" {
  source              = "./modules/app_service_plan"
  name                = "pdhstaging-serviceplan"
  resource_group_name = "ddphss-prim-stg-moderate-app-vnet"
  location            = "East US"
  kind                = "Linux"
  sku_tier            = "PremiumV2"
  sku_size            = "P3v2"
  reserved            = true
  per_site_scaling    = false
  tags = {
    environment = "staging"
    managed-by  = "terraform"
  }
}

