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

If traffic to a given code path must be completely stopped, [the backend source can be removed from the load balancer](https://docs.microsoft.com/en-us/cli/azure/network/front-door/backend-pool?view=azure-cli-latest#az_network_front_door_backend_pool_delete).

### From the CLI

```
az network front-door backend-pool delete --front-door-name <FD_NAME>
                                          --name <BE_POOL_NAME>
                                          --resource-group <RESX_GROUP>
```

### From the Azure Portal

1. Login to Azure Portal
2. Navigate to `prime-data-hub-*` > `Front Door Designer` within the Portal
3. Click on the backend pool(s) to be removed
4. From the bottom menu, select the `Delete` option
5. Repeat for each affected backend pool

## Delete Application Resources

If the application must completely be removed from Azure, Terraform can destroy all application resources. To do this:

1. [Follow the operations/README.md for environment tear down directions](https://github.com/CDCgov/prime-reportstream/blob/master/operations/README.md#tear-down-a-environment)