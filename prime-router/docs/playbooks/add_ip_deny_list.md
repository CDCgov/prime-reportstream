# Add IP to deny list in Azure

## Introduction
There may be a time where we need to deny a specific IP address due to a bad actor or misconfiguration from a sender/reciever. After checking into the various options, it seems that blocking the user at the Azure WAF/Front Door will be easier to deploy and remove than through Akamai. Please refer to the following steps.


## Add IP to deny list steps
### Console
1. Log into the Azure Portal.
2. Click the resource group you want to add the deny list to.
3. In the main resource group window, find the Front Door WAF Policy and click on it.
4. Inside the WAF Policy Screen, click on Custom Rule under the Settings section of the menu.
5. Next click on Add Custom Rule.
6. Provide the followiing in the opened menu
    - Name: Rule of the name you want to create.
    - Priority: Priority number based on other Rules.
    - Conditions
        - Match Type: IP Address
        - Match Variable: RemoteAddr
        - Operation: Does Contain
        - IP Address or Range: *IP YOU WISH TO BLOCK*
    - Then
      - Deny Traffic

### Azure CLI
1. Add a custom WAF Rule with the following changes: 
- **IPAllowPolicyExampleCLI**: The Policy name for the WAF Policy you wish to create.
- **IPAllowListRule**: Can be used multiple times, but should be a description of the specific rule you are creating.
- Priority: Based on other rules
- resource-group: The Resource Group you are adding the rule in
- ip-address-range-1: IP address you want to block
```sh
az network front-door waf-policy rule create \
  --name IPAllowListRule \
  --priority 1 \
  --rule-type MatchRule \
  --action Block \
  --resource-group <resource-group-name> \
  --policy-name IPAllowPolicyExampleCLI --defer

```
```sh
az network front-door waf-policy rule match-condition add \
--match-variable SocketAddr \
--operator IPMatch \
--values "ip-address-range-1" "ip-address-range-2" \
--negate true \
--name IPAllowListRule \
  --resource-group <resource-group-name> \
  --policy-name IPAllowPolicyExampleCLI
```

## Remove IP from deny list steps
### Console
1. Log into the Azure Portal.
2. Click the resource group you created the WAF policy in above.
3. Click on the WAF policy you created in the list of resources.
4. Click the delete icon on overview page.

### Azure CLI
1. Delete the WAF policy rule
- - **IPAllowPolicyExampleCLI**: The Policy name for the WAF Policy you wish to delete.
```sh
az network front-door waf-policy delete --name IPAllowPolicyExampleCLI --resource-group <resource-group-name>
```


## Notes
*  Please refer to this document for further informaiton in necessary: https://docs.microsoft.com/en-us/azure/web-application-firewall/afds/waf-front-door-configure-ip-restriction