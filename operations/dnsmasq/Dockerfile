FROM alpine:latest

RUN apk --no-cache add dnsmasq
EXPOSE 53/tcp 53/udp

ARG AZ_ENV
COPY dnsmasq-*.conf /tmp/
RUN cp /tmp/dnsmasq-${AZ_ENV}.conf /etc/dnsmasq.conf

CMD ["dnsmasq", "-k"]