set -euo pipefail

# Get developer name
read -r -p "Enter a deverloper name (eg. cglodosky, test, prod): " prime_dev_name

# Check that a name was entered
if [ -z $prime_dev_name ]
then
    echo "A developer name was not entered. Exiting."
    exit 1
fi

# Set resource group
if [[ "$prime_dev_name" =~ (test|prod) ]]
then
    resource_group="prime-data-hub-$prime_dev_name"
else
    resource_group="prime-dev-$prime_dev_name"
fi

# Set variables
PREFIX="pdh"
vnet_name=$PREFIX"_vnet"
nsg_bastion=$PREFIX"_nsg.bastion"
nsg_public=$PREFIX"_nsg.public"
nsg_private=$PREFIX"_nsg.private"
bastion_ip=$PREFIX"_ip.bastion"
bastion_name=$PREFIX"_bastion"
storage_account=$PREFIX"storageaccount"
container_registry=$PREFIX"containerregistry"

# Check that resource group exists
if [ ! `az group exists --resource-group "$resource_group"` ]
then
    echo "$resource_group is not an existing resource group."
else
    echo "Building the environment within the $resource_group resource group..."
fi

# Create network security groups
echo "--- Creating network security groups..."
az network nsg create --resource-group $resource_group \
                      --name $nsg_bastion \
                      --output none \
                      --only-show-errors

az network nsg rule create --resource-group $resource_group \
                           --nsg-name $nsg_bastion \
                           --name AllowHttpsInbound \
                           --priority 100 \
                           --access Allow \
                           --direction Inbound \
                           --protocol TCP \
                           --source-address-prefixes Internet \
                           --destination-port-ranges 443 \
                           --output none \
                           --only-show-errors

az network nsg rule create --resource-group $resource_group \
                           --nsg-name $nsg_bastion \
                           --name AllowGatewayManagerInbound \
                           --priority 110 \
                           --access Allow \
                           --direction Inbound \
                           --protocol TCP \
                           --source-address-prefixes GatewayManager \
                           --destination-port-ranges 443 \
                           --output none \
                           --only-show-errors

az network nsg rule create --resource-group $resource_group \
                           --nsg-name $nsg_bastion \
                           --name AllowAzureLoadBalancerInbound \
                           --priority 120 \
                           --access Allow \
                           --direction Inbound \
                           --protocol TCP \
                           --source-address-prefixes AzureLoadBalancer \
                           --destination-port-ranges 443 \
                           --output none \
                           --only-show-errors

az network nsg rule create --resource-group $resource_group \
                           --nsg-name $nsg_bastion \
                           --name AllowSshRdpOutbound \
                           --priority 130 \
                           --access Allow \
                           --direction Outbound \
                           --protocol "*" \
                           --destination-address-prefixes VirtualNetwork \
                           --destination-port-ranges 22 3389 \
                           --output none \
                           --only-show-errors

az network nsg rule create --resource-group $resource_group \
                           --nsg-name $nsg_bastion \
                           --name AllowAzureCloudOutbound \
                           --priority 140 \
                           --access Allow \
                           --direction Outbound \
                           --protocol TCP \
                           --destination-address-prefixes AzureCloud \
                           --destination-port-ranges 443 \
                           --output none \
                           --only-show-errors

az network nsg create --resource-group $resource_group \
                      --name $nsg_public \
                      --output none \
                      --only-show-errors

az network nsg create --resource-group $resource_group \
                      --name $nsg_private \
                      --output none \
                      --only-show-errors

# Create virtual network
echo "--- Creating virtual network..."
az network vnet create --resource-group $resource_group \
                       --name $vnet_name \
                       --address-prefixes 10.0.0.0/16 \
                       --subnet-name AzureBastionSubnet \
                       --subnet-prefix 10.0.0.0/27 \
                       --network-security-group $nsg_bastion \
                       --output none \
                       --only-show-errors

az network vnet subnet create --resource-group $resource_group \
                              --vnet-name $vnet_name \
                              --name public \
                              --address-prefix 10.0.1.0/27 \
                              --network-security-group $nsg_public \
                              --output none \
                              --only-show-errors

az network vnet subnet create --resource-group $resource_group \
                              --vnet-name $vnet_name \
                              --name private \
                              --address-prefix 10.0.2.0/24 \
                              --network-security-group $nsg_private \
                              --service-endpoints Microsoft.Storage \
                              --output none \
                              --only-show-errors

# Create public IP address for bastion host
echo "--- Creating public IP for bastion host..."
az network public-ip create --resource-group $resource_group \
                            --name $bastion_ip \
                            --sku Standard \
                            --output none \
                            --only-show-errors

# Create bastion host
echo "--- Creating bastion host..."
az network bastion create --resource-group $resource_group \
                          --vnet-name $vnet_name \
                          --name $bastion_name \
                          --public-ip-address $bastion_ip \
                          --output none \
                          --only-show-errors

# Create storage account
# echo "--- Creating storage account..."
# az storage account create --resource-group $resource_group \
#                           --name $storage_account \
#                           --sku Standard_LRS \
#                           --output none \
#                           --only-show-errors

# az storage account network-rule add --resource-group $resource_group \
#                                     --account-name $storage_account \
#                                     --vnet-name $vnet_name \
#                                     --subnet private \
#                                     --output none \
#                                     --only-show-errors

# Create container registry
# echo "--- Creating container registry..."
# az acr create --resource-group $resource_group \
#               --name $container_registry \
#               --sku Basic \
#               --admin-enabled true \
#               --output none \
#               --only-show-errors
