ARG JAVA_VERSION=21
# This Docker file was generated from a `mvn archetype:generate -Ddocker` command. It creates 
# a container that is deployable to the Azure cloud
#
# This image additionally contains function core tools – useful when using custom extensions
#FROM mcr.microsoft.com/azure-functions/java:4-java$JAVA_VERSION-core-tools AS installer-env
FROM mcr.microsoft.com/azure-functions/java:4-java$JAVA_VERSION-build AS installer-env

COPY . /src/java-function-app
RUN cd /src/java-function-app && \
    mkdir -p /home/site/wwwroot && \
    cd ./build/azure-functions/ && \
    cd $(ls -d */|head -n 1) && \
    cp -a . /home/site/wwwroot

# This image is ssh enabled
FROM mcr.microsoft.com/azure-functions/java:4-java$JAVA_VERSION-appservice
# This image isn't ssh enabled
#FROM mcr.microsoft.com/azure-functions/java:4-java$JAVA_VERSION

ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

COPY --from=installer-env ["/home/site/wwwroot", "/home/site/wwwroot"]


COPY ./certs/CDC-G2.crt $JAVA_HOME/conf/security
RUN cd $JAVA_HOME/conf/security \
    && $JAVA_HOME/bin/keytool -cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias G1 -file CDC-G2.crt

COPY ./certs/CDC-G2-S1.crt $JAVA_HOME/conf/security
RUN cd $JAVA_HOME/conf/security \
    && $JAVA_HOME/bin/keytool -cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias G2 -file CDC-G2-S1.crt

COPY ./certs/CA-CA-S1.crt $JAVA_HOME/conf/security
RUN cd $JAVA_HOME/conf/security \
    && $JAVA_HOME/bin/keytool -cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias S1 -file CA-CA-S1.crt
    
COPY ./certs/CA-CA-S2.crt $JAVA_HOME/conf/security
RUN cd $JAVA_HOME/conf/security \
    && $JAVA_HOME/bin/keytool -cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias S2 -file CA-CA-S2.crt

