 module "pdhprod_serviceplan" {
  source              = "./modules/app_service_plan"
  name                = "pdhprod-serviceplan"
  resource_group_name = "prime-data-hub-prod"
  location            = "East US"
  kind                = "Linux"
  sku_tier            = "PremiumV2"
  sku_size            = "P3v2"
  reserved            = true
  per_site_scaling    = false
  tags = {
    environment = "prod"
  }
}

module "pdhstaging_serviceplan" {
  source              = "./modules/app_service_plan"
  name                = "pdhstaging-serviceplan"
  resource_group_name = "prime-data-hub-staging"
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

module "pdhtest_serviceplan" {
  source              = "./modules/app_service_plan"
  name                = "pdhtest-serviceplan"
  resource_group_name = "prime-data-hub-test"
  location            = "East US"
  kind                = "Linux"
  sku_tier            = "PremiumV2"
  sku_size            = "P3v2"
  reserved            = true
  per_site_scaling    = false
  tags = {
    environment = "test"
    managed-by  = "terraform"
  }
}

