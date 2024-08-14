resource "azurerm_monitor_autoscale_setting" "app_autoscale" {
  count               = 1
  name                = "${var.resource_prefix}-appautoscale"
  resource_group_name = var.resource_group
  location            = var.location
  target_resource_id  = local.app_service_plan

  profile {
    name = "Autoscaling event - ScaleOnHighLoad"

    capacity {
      default = 6
      minimum = 6
      maximum = 20
    }

    rule {
      metric_trigger {
        metric_name        = "CpuPercentage"
        metric_resource_id = local.app_service_plan
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT5M"
        time_aggregation   = "Average"
        operator           = "GreaterThan"
        threshold          = 70
      }

      scale_action {
        direction = "Increase"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT1M"
      }
    }

    rule {
      metric_trigger {
        metric_name        = "CpuPercentage"
        metric_resource_id = local.app_service_plan
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT5M"
        time_aggregation   = "Average"
        operator           = "LessThan"
        threshold          = 25
      }

      scale_action {
        direction = "Decrease"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT5M"
      }
    }

    rule {
      metric_trigger {
        metric_name        = "MemoryPercentage"
        metric_resource_id = local.app_service_plan
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT5M"
        time_aggregation   = "Average"
        operator           = "GreaterThan"
        threshold          = 85
      }

      scale_action {
        direction = "Increase"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT5M"
      }
    }

    rule {
      metric_trigger {
        metric_name        = "MemoryPercentage"
        metric_resource_id = local.app_service_plan
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT5M"
        time_aggregation   = "Average"
        operator           = "LessThan"
        threshold          = 50
      }

      scale_action {
        direction = "Decrease"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT5M"
      }
    }

    rule {
      metric_trigger {
        metric_name        = "ApproximateMessageCount"
        metric_resource_id = join("/", ["${var.storage_account}", "services/queue/queues", "process"])
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT10M"
        time_aggregation   = "Average"
        operator           = "GreaterThan"
        threshold          = 5
      }

      scale_action {
        direction = "Increase"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT5M"
      }
    }
    rule {
      metric_trigger {
        metric_name        = "ApproximateMessageCount"
        metric_resource_id = join("/", ["${var.storage_account}", "services/queue/queues", "process"])
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT10M"
        time_aggregation   = "Average"
        operator           = "LessThan"
        threshold          = 5
      }

      scale_action {
        direction = "Decrease"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT5M"
      }
    }
  }

  notification {
    # Updated to match production
    email {
      custom_emails                         = []
      send_to_subscription_administrator    = false
      send_to_subscription_co_administrator = false
    }
    webhook {
      service_uri = var.pagerduty_url
    }
  }

  depends_on = [
    var.app_service_plan
  ]
}

locals {
  app_service_plan = replace(var.app_service_plan, "serverFarms", "serverfarms")
}
