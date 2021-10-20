FROM alpine:latest

# Install dnsmasq
RUN apk --no-cache add dnsmasq

# Expose the DNS port
EXPOSE 53/udp

# Place the environment's config in the config directory
ARG AZ_ENV
COPY config /tmp/config
RUN cp -r /tmp/config/${AZ_ENV}/. /etc/dnsmasq.d/

ENTRYPOINT ["dnsmasq", "-k"]