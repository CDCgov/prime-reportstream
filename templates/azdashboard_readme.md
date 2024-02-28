# Steps to modify the dashboard template

    ***Do these changes from a feature branch***

1- Download the json file from Azure for the corresponding dashboard
2- Replace the content of  template/az_dashboard_json.tpl file with the one you just downloaded
3- Edit the content of the tpl file
      a- Replace all subscription value with ${subscription_id}
      b- Replace all resource group value with ${resource_group_name}
      c- Replace all appinsights name value with ${appinsights_name}
         e.g: "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
      d- Remove "properties {" at the begining of your file and the corresponding "}" at the end.
      e- Remove the extra code at the end of the file after the metadata portion
4- Commit your changes and create a PR to master branch
5- Once the PR is approved and merged, the changes will be applied during the production deployment.


* Lucas Dze
