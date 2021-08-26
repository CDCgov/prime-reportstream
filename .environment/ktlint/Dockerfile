FROM alpine:3 as downloader

ARG KTLINT_VERSION=0.42.1
RUN apk update \
    && apk add curl gnupg
WORKDIR /tmp
RUN curl -sS https://keybase.io/ktlint/pgp_keys.asc | gpg --import
RUN curl --output /tmp/ktlint -sSLO https://github.com/pinterest/ktlint/releases/download/${KTLINT_VERSION}/ktlint
RUN curl --output /tmp/ktlint.asc -sSLO https://github.com/pinterest/ktlint/releases/download/${KTLINT_VERSION}/ktlint.asc
RUN gpg --verify ktlint.asc
RUN chmod a+x /tmp/ktlint

FROM java:alpine
COPY --from=downloader "/tmp/ktlint" "/usr/bin/"
RUN mkdir "/src"
WORKDIR "/src"