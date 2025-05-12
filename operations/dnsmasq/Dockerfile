FROM cgr.dev/chainguard/wolfi-base:latest


RUN apk update && \
    apk add --no-cache dnsmasq

# Expose the DNS port
EXPOSE 53/udp

ARG AZ_ENV
COPY config /tmp/config
RUN cp -r /tmp/config/${AZ_ENV}/. /etc/dnsmasq.d/ && rm -r /tmp/config

ENTRYPOINT ["dnsmasq", "-k"]
