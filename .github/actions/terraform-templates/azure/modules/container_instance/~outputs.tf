output "exec" {
  value = "az container exec --name ${azurerm_container_group.default.name} --resource-group ${var.common.resource_group.name} --container-name ${azurerm_container_group.default.name} ${var.exec != "" ? "--exec-command '${var.exec}'" : ""}"
}
