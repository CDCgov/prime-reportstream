
module "application_insights" {
  source          = "../modules/application_insights"
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
  is_metabase_env = var.is_metabase_env
}

module "function_app" {
  source                      = "../modules/function_app"
  environment                 = var.environment
  resource_group              = var.resource_group
  resource_prefix             = var.resource_prefix
  location                    = var.location
  ai_instrumentation_key      = module.application_insights.instrumentation_key
  ai_connection_string        = module.application_insights.connection_string
  okta_redirect_url           = var.okta_redirect_url
  terraform_caller_ip_address = var.terraform_caller_ip_address
  use_cdc_managed_vnet        = var.use_cdc_managed_vnet
}

module "front_door" {
  source           = "../../modules/front_door"
  environment      = var.environment
  resource_group   = var.resource_group
  resource_prefix  = var.resource_prefix
  location         = var.location
  https_cert_names = var.https_cert_names
  is_metabase_env  = var.is_metabase_env
}

module "sftp_container" {
  count = var.environment != "prod" ? 1 : 0

  source               = "../../modules/sftp_container"
  environment          = var.environment
  resource_group       = var.resource_group
  resource_prefix      = var.resource_prefix
  location             = var.location
  use_cdc_managed_vnet = var.use_cdc_managed_vnet
}

module "metabase" {
  count = var.is_metabase_env ? 1 : 0

  source                 = "../../modules/metabase"
  environment            = var.environment
  resource_group         = var.resource_group
  resource_prefix        = var.resource_prefix
  location               = var.location
  ai_instrumentation_key = module.application_insights.metabase_instrumentation_key
  ai_connection_string   = module.application_insights.metabase_connection_string
  use_cdc_managed_vnet   = var.use_cdc_managed_vnet
}
