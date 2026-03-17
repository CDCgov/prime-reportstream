module "pdhprod_serviceplan" {
  source              = "../modules/app_service_plan"
  name                = "ddphss-prd-serviceplan"
  resource_group_name = "ddphss-prim-prd-moderate-app-rg"
  location            = "East US"
  kind                = "Linux"
  sku_tier            = "PremiumV2"
  sku_size            = "P3v2"
  reserved            = true
  per_site_scaling    = false
  tags = {
    environment = "prd"
    managed-by  = "terraform"
  }
}
