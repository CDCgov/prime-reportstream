FROM ubuntu:24.04

# Latest patches
RUN apt-get update && apt-get upgrade -y

# Install dnsmasq and explicitly update perl to the patched version
RUN apt-get install -y dnsmasq perl=5.38.2-3.2ubuntu0.1

# Expose the DNS port
EXPOSE 53/udp

# Place the environment's config in the config directory
ARG AZ_ENV
COPY config /tmp/config
RUN cp -r /tmp/config/${AZ_ENV}/. /etc/dnsmasq.d/

# Remove the unused configs
RUN rm -r /tmp/config

ENTRYPOINT ["dnsmasq", "-k"]
