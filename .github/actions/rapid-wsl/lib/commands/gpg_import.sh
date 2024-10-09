#!/bin/bash

echo ""
echo "***"
echo "Setting up gpg"

PGP_FILES=(./etc/*.pgp)
GIT_CONFIG=(./etc/.gitconfig)
if [[ -e "${PGP_FILES[0]}" ]]; then
    if [[ -f "${GIT_CONFIG}" ]]; then
        echo "Importing etc/*.pgp files:"
        echo "***"
        echo ""
        wsl -d $1 -u $3 -e bash -c \
        ' \
        path=$(wslpath -u '"$PWD"')/etc/; \
        path=$(echo $path | sed "s3/c/c/3/c/3g"); \
        gpg --pinentry-mode loopback --import $path/*.pgp; \
        cp $path/.gitconfig ~/.gitconfig; \
        cp $path/.gitconfig /home/'"$3"'/.gitconfig; \
        sudo chown -R '"$3"':'"$3"' /home/'"$3"'/.gitconfig; \
        echo "export GPG_TTY=\$(tty)" >>~/.bashrc; \
        '
        echo ""
        echo "***"
        echo "Import complete"
        echo "***"
        echo ""
    else
        echo "No etc/.gitconfig file found"
        echo "***"
        cat "etc/sample.gitconfig"
        echo "***"
        echo ""
    fi
else
    echo "No etc/*.pgp files found"
    echo "keybase pgp export -q xxx > etc/public.pgp"
    echo "keybase pgp export -q xxx --secret > etc/private.pgp"
    echo "***"
    echo ""
fi
