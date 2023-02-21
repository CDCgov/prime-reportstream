# Steps to modify the dashboard template

1- Download the json file from Azure for the corresponding dashboard
2- Replacee the content of this file az_dashboard_json.tpl with the one you just downloaded
3- Edit the content of the new version of your file
      a- Replace all subscription value with ${subscription_id}
      b- Replace all resource group value with ${resource_group_name}
      c- Replace all appinsights name value with ${appinsights_name}
      e.g: "/subscriptions/${subscription_id}/resourceGroups/${resource_group_name}/providers/microsoft.insights/components/${appinsights_name}"
4- Commit your changes and create a PR to master branch
5- Once the PR is approved and merged, the changes will be apply during the next deployment in production env


Starting this file way too late, but wanted to recognize contributions made by people who helped this repo. There are many more than this, but I should have started this file years ago.

* Lucas Dze