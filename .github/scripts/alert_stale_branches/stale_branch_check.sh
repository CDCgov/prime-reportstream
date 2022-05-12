#!/bin/bash
_setArgs(){
  while [ "${1:-}" != "" ]; do
    case "$1" in
      "--notmerged")
        not_merged=true
        ;;
      "--notmergedcount")
        not_merged_count=true
        ;;
      "--merged")
        merged=true
        ;;
      "--mergedcount")
        merged_count=true
        ;;
    esac
    shift
  done
}

get_branches () {
count=0
for k in $branches
do
  if [[ "$k" != *"HEAD"* ]] && [[ "$k" != *"->"* ]] && [[ "$k" != *"master"* ]]
    then
      if [ -z "$(git log -1 --since='6 months ago' -s $k)" ]
        then
          ((count=count+1))
          if [ $echo_branches ]
            then
            echo `git show --format="%ci %cr %an" $k | head -n 1` \\t$k
          fi
      fi
  fi
done
if [ $echo_count ]
  then
    echo Total: $count
fi
}

_setArgs $*

if [ $merged ] || [ $merged_count ]
  then
    branches=$(git branch -r --merged | sed /\*/d)
    echo_branches=$merged
    echo_count=$merged_count
    get_branches
fi

if [ $not_merged ] || [ $not_merged_count ]
  then
    branches=$(git branch -r --no-merged | sed /\*/d)
    echo_branches=$not_merged
    echo_count=$not_merged_count
    get_branches
fi