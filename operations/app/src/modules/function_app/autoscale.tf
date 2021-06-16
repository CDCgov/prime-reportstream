resource "azurerm_monitor_autoscale_setting" "app_autoscale" {
  count = (var.environment == "prod" ? 1 : 0)
  name = "${var.resource_prefix}-appautoscale"
  resource_group_name = var.resource_group
  location = var.location
  target_resource_id = data.azurerm_app_service_plan.service_plan.id

  profile {
    name = "ScaleOnHighLoad"

    capacity {
      default = 1
      minimum = 1
      maximum = 20
    }

    rule {
      metric_trigger {
        metric_name = "CpuPercentage"
        metric_resource_id = data.azurerm_app_service_plan.service_plan.id
        time_grain = "PT1M"
        statistic = "Average"
        time_window = "PT5M"
        time_aggregation = "Average"
        operator = "GreaterThan"
        threshold = 75
      }

      scale_action {
        direction = "Increase"
        type = "ChangeCount"
        value = "1"
        cooldown = "PT5M"
      }
    }

    rule {
      metric_trigger {
        metric_name = "CpuPercentage"
        metric_resource_id = data.azurerm_app_service_plan.service_plan.id
        time_grain = "PT1M"
        statistic = "Average"
        time_window = "PT5M"
        time_aggregation = "Average"
        operator = "LessThan"
        threshold = 25
      }

      scale_action {
        direction = "Decrease"
        type = "ChangeCount"
        value = "1"
        cooldown = "PT5M"
      }
    }

    rule {
      metric_trigger {
        metric_name = "MemoryPercentage"
        metric_resource_id = data.azurerm_app_service_plan.service_plan.id
        time_grain = "PT1M"
        statistic = "Average"
        time_window = "PT5M"
        time_aggregation = "Average"
        operator = "GreaterThan"
        threshold = 90
      }

      scale_action {
        direction = "Increase"
        type = "ChangeCount"
        value = "1"
        cooldown = "PT5M"
      }
    }

    rule {
      metric_trigger {
        metric_name = "MemoryPercentage"
        metric_resource_id = data.azurerm_app_service_plan.service_plan.id
        time_grain = "PT1M"
        statistic = "Average"
        time_window = "PT5M"
        time_aggregation = "Average"
        operator = "LessThan"
        threshold = 50
      }

      scale_action {
        direction = "Decrease"
        type = "ChangeCount"
        value = "1"
        cooldown = "PT5M"
      }
    }
  }

  notification {
    webhook {
      service_uri = data.azurerm_key_vault_secret.pagerduty_url.value
    }
  }
}