#!/bin/bash
#
# This script runs the Azure function of the project with a debug port open to allow debugger to attach.
# It is meant to used on developer machines, but not in production.
#
# It handles three different configurations in this order of preference
#  1. The dev container case where '/prime-data-hub-router/azure-functions/prime-data-hub-router' contains the function
#  2. The local case where 'build/azure-functions/prime-data-hub-router'1' contains the function
#  2. The local case where 'azure-functions/prime-data-hub-router' contains the function
#

set -e
base_name=azure-functions/prime-data-hub-router

# find the function folder
if [ -d /prime-data-hub-router/$base_name ]; then
  function_folder=/prime-data-hub-router/$base_name
elif [ -d build/$base_name ]; then
  function_folder=target/$base_name
elif [ -d $base_name ]; then
  function_folder=$base_name
fi

cd $function_folder

func host start --cors http://localhost:8090,http://localhost:3000 --language-worker -- "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"