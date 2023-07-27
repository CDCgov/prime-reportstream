# Shutdown Options

In the event the project needs to shutdown quickly in the response to an incident, the following options are available:

## Disable a Function

If the scope of the shutdown can be limited to one or more Azure Functions, [Functions can individually be disabled](https://docs.microsoft.com/en-us/azure/azure-functions/disable-function?tabs=portal).

### From the CLI

```
az functionapp config appsettings set --name <FUNCTION_APP_NAME>
                                      --resource-group <RESOURCE_GROUP_NAME>
                                      --settings AzureWebJobs.<FUNCTION_NAME>.Disabled=true
```

### From the Azure Portal

1. Login to Azure Portal
2. Navigate to `*-functionapp` > `Functions` within the Portal
3. Click on the affected function(s)
4. From the top menu, select the `Disable` option
5. Repeat for each affected function

## Disable HTTP Traffic

If traffic to a given code path must be completely stopped, [the backend route can be disabled in the load balancer](https://docs.microsoft.com/en-us/cli/azure/network/front-door/routing-rule?view=azure-cli-latest#az_network_front_door_routing_rule_update).

### From the CLI

```
az network front-door routing-rule update --front-door-name <FD_NAME>
                                          --name <RULE_NAME>
                                          --resource-group <RESX_GROUP>
                                          --enabled Disabled
```

### From the Azure Portal

1. Login to Azure Portal
2. Navigate to `prime-data-hub-*` > `Front Door Designer` within the Portal
3. Click on the routing rule(s) to be disabled
4. From the top of the page, select the `Disabled` option under `Status`
5. Repeat for each affected routing rule
6. Save the Front Door configuration with the `Save` button at the top

## Delete Application Resources

If the application must completely be removed from Azure, Terraform can destroy all application resources. To do this:

1. [Follow the operations/README.md for environment tear down directions](https://github.com/CDCgov/prime-reportstream/blob/master/operations/README.md#tear-down-a-environment)
2. Start with the app stage (`04-app`) and work backwards until the resources needed are destroyed
3. Make note of the warning when tearing down resources that are not ephemeral
