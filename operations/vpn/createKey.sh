#!/usr/bin/env bash

declare -a ENVIRONMENTS=("prod" "staging" "demo3" "demo2" "demo1")

# Pretty colors
COLOR_AZURE='\033[1;44m'
COLOR_GREEN='\033[0;32m'
COLOR_NONE='\033[0m'

VPN_USERNAME=""

function prompt_user() {
  echo "What user would you like to create?"
  read -p "> " VPN_USERNAME
}

function create_user_keys() {
  if [ ! -d "${VPN_USERNAME}" ]
  then
    echo "Creating keys in \"profiles/${VPN_USERNAME}/\"..."
    mkdir profiles/${VPN_USERNAME}
    ipsec pki --gen --outform pem > "profiles/${VPN_USERNAME}/${VPN_USERNAME}Key.pem"
    ipsec pki --pub --in "profiles/${VPN_USERNAME}/${VPN_USERNAME}Key.pem" | ipsec pki --issue --cacert ./gen/caCert.pem --cakey ./gen/caKey.pem --dn "CN=${VPN_USERNAME}" --san "${VPN_USERNAME}" --flag clientAuth --outform pem > "profiles/${VPN_USERNAME}/${VPN_USERNAME}Cert.pem"
    openssl pkcs12 -in "profiles/${VPN_USERNAME}/${VPN_USERNAME}Cert.pem" -inkey "profiles/${VPN_USERNAME}/${VPN_USERNAME}Key.pem" -certfile ./gen/caCert.pem -export -out "profiles/${VPN_USERNAME}/${VPN_USERNAME}.p12"
  fi
}

function create_user_vpn_profiles() {
  echo "Creating VPN profiles in \"profiles/${VPN_USERNAME}/\"..."
  CLIENT_CERTIFICATE=$(<profiles/${VPN_USERNAME}/${VPN_USERNAME}Cert.pem)
  PRIVATE_KEY=$(<profiles/${VPN_USERNAME}/${VPN_USERNAME}Key.pem)

  for env in "${ENVIRONMENTS[@]}"
  do
    CLIENT_CERTIFICATE=${CLIENT_CERTIFICATE} PRIVATE_KEY=${PRIVATE_KEY} envsubst < gen/prime-data-hub-${env}.ovpn > "profiles/${VPN_USERNAME}/prime-data-hub-${env}.ovpn"
  done
}

echo -e "${COLOR_AZURE}=== CREATE VPN KEYS ===${COLOR_NONE}"
echo ""
prompt_user
echo ""
create_user_keys
echo ""
create_user_vpn_profiles
echo ""
echo -e "${COLOR_GREEN}Done making keys and VPN profiles! Send them via CDC Teams.${COLOR_NONE}"
