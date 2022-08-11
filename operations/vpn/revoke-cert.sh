#!/usr/bin/env bash

# requirements:
# openssl
# az

gateway_name=$1
resource_group=$2
cert_name=$3
cert_data=$4

thumb=$(echo "$cert_data" | openssl x509 -fingerprint -noout \
| sed "s/SHA1 Fingerprint=//g" | sed "s/://g")

az network vnet-gateway revoked-cert create -g "$resource_group" \
-n "$cert_name" --gateway-name "$gateway_name" --thumbprint "$thumb"
