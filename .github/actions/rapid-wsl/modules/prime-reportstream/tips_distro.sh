#!/bin/bash

echo -e "
## Run local terraform options:

1.
(cd operations/app/src/environments/01-network/ && \ 
sed -i -e 's/backend \"azurerm\"/backend \"local\"/g' main.tf && \ 
terraform init && ../tf --verbose -c \"validate\")

2.
(cd operations/ make tf-cmd TF_STAGE=\"01-network\" TF_CMD=\"tf validate\")

## Run local frontend:

sudo apt-get install node -y
sudo apt-get install npm -y
sudo npm install -g n
n 14

sudo apt remove cmdtest
sudo apt remove yarn
curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -
echo \"deb https://dl.yarnpkg.com/debian/ stable main\" | sudo tee /etc/apt/sources.list.d/yarn.list
sudo apt-get update
sudo apt-get install yarn -y

sudo npm install pm2 -g
pm2 --name HelloWorld start npm -- start

#pm2 ps
#pm2 delete 0
#pm2 logs
"
