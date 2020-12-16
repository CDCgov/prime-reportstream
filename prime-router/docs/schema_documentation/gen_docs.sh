#!/usr/local/bin/zsh

# set up our variables
PRIME_DIR="../../../prime-router/"
DOC_DIR=$(pwd)
SCHEMA_REG="/"
CSV_REG="\-csv"

# change to working directory
cd $PRIME_DIR || exit

# loop each file name in the schemas dir, and take just its
# immediate parent dir and the file name itself
for FILE_NAME in $(find metadata/schemas -type f | cut -f 3,4 -d /)
do
    # lc the file name
    FILE_NAME=$FILE_NAME:l
    # get the schema name from the file name
    SCHEMA_NAME=${FILE_NAME/.schema/}
    # set the doc name to the schema name as a default
    DOC_NAME=$SCHEMA_NAME

    # if the schema name has the parent dir and slash in it, remove it
    if echo "$DOC_NAME" | grep -Eq $SCHEMA_REG; then
        DOC_NAME=$(echo "$DOC_NAME" | sed -e 's|[a-z]*/||g')
    fi

    # some schemas include a -csv in the file name but that's
    # not present in the actual schema name when generating docs
    if echo "$SCHEMA_NAME" | grep -Eq -- "$CSV_REG"; then
        SCHEMA_NAME=$(echo "$SCHEMA_NAME" | sed -e 's|-csv$||g')
    fi

    # generate docs using prime
    . target/prime --generate-docs --input_schema "$SCHEMA_NAME" --output "$DOC_NAME-doc" --output_dir "$DOC_DIR"
done;

# return to original dir
cd - || exit