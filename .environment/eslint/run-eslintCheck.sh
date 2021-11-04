#!/usr/bin/env bash

function note() {
    echo "eslint-check> info: ${*}"
}

function js_fmt_check() {
    note "Checking JavaScript formatting."
    MODIFIED_TSX_FILES_COUNT=$(git status --porcelain | grep "\.tsx$" | wc -l)
    MODIFIED_JS_FILES_COUNT=$(git status --porcelain | grep "\.js$" | wc -l)
    if [[ ${MODIFIED_TSX_FILES_COUNT?} != 0 ]] || [[ ${MODIFIED_JS_FILES_COUNT?} != 0 ]]; then
        cd frontend-react
        npm run lint
        cd ..
    else
        note "Skipping this check, you made no changes to JavaScript or TypeScript files..."
    fi
}

js_fmt_check