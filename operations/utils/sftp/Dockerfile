FROM cgr.dev/chainguard/wolfi-base@sha256:0e09bcd548cf2dfb9a3fd40af1a7389aa8c16b428de4e8f72b085f015694ce3d
# Based on work of Adrian Dvergsdal [atmoz.net]

RUN apk update && apk upgrade && \
apk add bash shadow openssh-server openssh-sftp-server && \
groupadd users && \
rm -rf /var/cache/apk/* && \
mkdir -p /var/run/sshd && \
rm -f /etc/ssh/ssh_host_*key*

COPY files/sshd_config /etc/ssh/sshd_config
COPY files/create-sftp-user /usr/local/bin/
COPY files/entrypoint / 

EXPOSE 22

ENTRYPOINT ["/entrypoint"]

