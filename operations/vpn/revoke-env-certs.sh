#!/usr/bin/env bash

# requirements:
# openssl
# az

cd "$(dirname "$0")"

vpn_user="$1"
envs="$2"
cert_data=$3

for env in $envs
do
  gateway_name=pdh${env}-vpn
  resource_group=prime-data-hub-${env}

  ./revoke-cert.sh $gateway_name $resource_group $vpn_user "$cert_data"
done
