FROM ubuntu:22.04

# Install dnsmasq
RUN apt-get update && apt-get install -y dnsmasq

# Expose the DNS port
EXPOSE 53/udp

# Place the environment's config in the config directory
ARG AZ_ENV
COPY config /tmp/config
RUN cp -r /tmp/config/${AZ_ENV}/. /etc/dnsmasq.d/

# Remove the unused configs
RUN rm -r /tmp/config

ENTRYPOINT ["dnsmasq", "-k"]
