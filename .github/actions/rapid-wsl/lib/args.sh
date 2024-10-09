#!/bin/bash

source ./lib/controller.sh

#Declare the number of mandatory args
margs=2

# Common functions - BEGIN
function example {
  echo -e "example: ./rwsl [COMMAND] -d VAL -m VAL -u VAL -r VAL"
  echo -e "example: ./rwsl init -d 'Ubuntu-18.04' -m 'generic' -u 'user3' -r 'https://github.com/JosiahSiegel/rapid-wsl.git'"
}

function usage {
  echo -e "usage: ./rwsl [COMMAND] MANDATORY [OPTION]\n"
}

function help {
  usage
  echo -e "MANDATORY:"
  echo -e "  -d, --distro  VAL  Distro name (*reference: wsl --list --online*)"
  echo -e "  -m, --module  VAL  Application name\n"
  echo -e "OPTION:"
  echo -e "  -u, --user    VAL  Username"
  echo -e "  -r, --repo    VAL  Repository url"
  echo -e "  -h, --help        Prints this help\n"
  example
}

# Ensures that the number of passed args are at least equals
# to the declared number of mandatory args.
# It also handles the special case of the -h or --help arg.
function margs_precheck {
  if [ $2 ] && [ $1 -lt $margs ]; then
    if [ $2 == "--help" ] || [ $2 == "-h" ]; then
      help
      exit
    else
      usage
      example
      exit 1 # error
    fi
  fi
}

# Ensures that all the mandatory args are not empty
function margs_check {
  if [ $# -lt $margs ]; then
    usage
    example
    exit 1 # error
  fi
}
# Common functions - END

# Main
margs_precheck $# $1

distro=
module=
user="user"
repo=

i=0
command=""
# Args while-loop
while [ "$1" != "" ]; do
  ((i++))
  case $1 in
  -d | --distro)
    shift
    distro=$1
    ;;
  -m | --module)
    shift
    module=$1
    ;;
  -u | --user)
    shift
    user=$1
    ;;
  -r | --repo)
    shift
    repo=$1
    ;;
  -h | --help)
    help
    exit
    ;;
  *)
    if [ $i -eq 1 ]; then
      case $1 in
      -h | --help)
        help
        exit
        ;;
      init)
        command="init"
        ;;
      destroy)
        command="destroy"
        ;;
      install)
        command="install"
        ;;
      docker)
        command="docker"
        ;;
      setup)
        command="setup"
        ;;
      test)
        command="test"
        ;;
      test-pipe)
        command="test-pipe"
        ;;
      enter)
        command="enter"
        ;;
      backup)
        command="backup"
        ;;
      down)
        command="down"
        ;;
      fix)
        command="fix"
        ;;
      restore)
        command="restore"
        ;;
      status)
        command="status"
        ;;
      up)
        command="up"
        ;;
      tips)
        command="tips"
        ;;
      *)
        echo "[COMMAND]: illegal option $1"
        usage
        example
        exit 1 # error
        ;;
      esac
    else
      echo "[COMMAND]: illegal option $1"
      usage
      example
      exit 1 # error
    fi
    ;;
  esac
  shift
done

# Pass here your mandatory args for check
margs_check $distro $module
echo "$distro $module $user $repo"
eval $command "$distro" "$module" "$user" "$repo"
