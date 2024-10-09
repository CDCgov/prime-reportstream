#!/bin/bash

echo -e "
=======================

# GENERIC TIPS

=======================

## Resolve merge conflicts

### Find conflict files:
grep -lr '<<<<<<<' .

### Accept local/ours version:
git checkout --ours PATH/FILE

### For multiple files:
grep -lr '<<<<<<<' . | xargs git checkout --ours

### Accept remote/theirs version:
git checkout --theirs PATH/FILE

### For multiple files:
grep -lr '<<<<<<<' . | xargs git checkout --theirs

## Resolve failing git fetch:
rm -f .git/FETCH_HEAD
=======================
"

FILE=./modules/$2/tips_distro.sh
if [[ -f $FILE ]]; then
    echo -e "# MODULE SPECIFIC TIPS\n"
    echo -e "======================="
    $FILE $1 $2 $3 $4
fi
