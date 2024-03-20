#!/usr/bin/env bash

# requirements:
# openssl
# az

cd "$(dirname "$0")"

vpn_user="$1"
envs="$2"

cert_data=$(cat profiles/${vpn_user}/${vpn_user}Cert.pem)

for env in $envs
do
  gateway_name=pdh${env}-vpn
  resource_group=prime-data-hub-${env}

  thumb=$(echo "$cert_data" | openssl x509 -fingerprint -noout \
  | sed "s/SHA1 Fingerprint=//g" | sed "s/://g")

  az network vnet-gateway revoked-cert create -g "$resource_group" \
  -n "$vpn_user" --gateway-name "$gateway_name" --thumbprint "$thumb"
done
