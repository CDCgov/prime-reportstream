FROM cgr.dev/chainguard/wolfi-base:latest
RUN apk add git gh make parallel jq

COPY git-secrets /git-secrets
RUN make -C /git-secrets install
COPY lib/* /

ENTRYPOINT ["bash", "/scan.sh"]
