[![CAPI-LB](https://github.com/surisoft-io/capi-consul-lb/actions/workflows/main.yml/badge.svg)](https://github.com/surisoft-io/capi-consul-lb/actions/workflows/main.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/surisoft/capi-consul-lb)


# CAPI LB for Consul 
## _Light Apache Camel Load Balancer with Consul integration_

## Supports:
* Light API Gateway / Load Balancer powered by Apache Camel dynamics routes.
* Deployments supported: Hashicorp Consul
* Optional Spring Security OIDC protected CAPI Manager API.
* Distributed tracing system (Zipkin)
* Metrics (Prometheus)
* Hawtio (JVM management console)
* Load Balancer (Round robin)
* Failover (With and without Round Robin)
* Stick Session (Cookies and Headers)
* Certificate Manager (using the CAPI Manager API)

### CAPI Manager API
CAPI Manager is available on http://localhost:8380/swagger-ui.html
Security to this API is disabled by default, if you need to enable security you need to provide the following properties at run time:
```
    -Dcapi.manager.security.enabled=true \ 
    -Dcapi.manager.security.issuer=https://localhost:8443/auth/realms/master/protocol/openid-connect/certs \
```
If security (OpenID Connect/oauth2) is enabled CAPI needs the endpoint of your identity provider JWK.
###### With the API you can:
* Get all configured API's
* Get all running API's
* Add/Remove a node to an API

Certificate management is disabled by default, to enable it you need to provide a valid path to a truststore. 
CAPI will not change JVM default certificate.
To enable start CAPI with the following attributes:
```
    -Dcapi.trust.store.enabled=true
    -Dcapi.trust.store.path=/your/path/cacerts \ 
    -Dcapi.trust.store.password=changeit \
```
With the Certificate Management enabled you can:
* Get all the certificates in the trust store
* Add a certificate to the trust store providing the certificate
* Add a certificate to the trust store providing the HTTPS endpoint.
* Remove a certificate from the trust store.

### Install CAPI fat jar on a VM
* You need a consul instance running
* You need Open JDK 17
```
$ mkdir logs
$ git clone this repo
$ mvn clean package
$ java \
     -XX:InitialHeapSize=2g \
     -XX:MaxHeapSize=2g \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath="$PWD/logs/heap-dump.hprof" \
     -Dhazelcast.diagnostics.enabled=true \
     -Dhazelcast.diagnostics.directory="$CAPI_HOME/logs" \
     --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
     --add-opens java.base/java.lang=ALL-UNNAMED \
     --add-opens java.base/java.nio=ALL-UNNAMED \
     --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
     --add-opens java.management/sun.management=ALL-UNNAMED \
     --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
     -Dcapi.consul.discovery.enabled=true \
     -Dcapi.consul.discovery.timer.interval=20 \
     -Dcapi.consul.host=http://localhost:8500 \
     -Dcapi.trust.store.path=/your/path/cacerts \ 
     -Dcapi.trust.store.password=changeit \
     -Dcapi.manager.security.enabled=true \ 
     -Dcapi.manager.security.issuer=https://localhost:8443/auth/realms/master/protocol/openid-connect/certs \
     -jar <CAPI_JAR> > $PWD/logs/capi.log 2>&1 & echo $! > capi.pid

```
In the example above CAPI will be available with CAPI Manager secured and certificate management enabled.
### Install CAPI on Docker (with docker-compose)
```
version: "3"
services:
  capi:
    container_name: capi
    image: surisoft/capi-consul-lb:0.0.1
    ports:
      - "8380:8380"
    environment:
      - hazelcast.diagnostics.enabled=true
      - hazelcast.diagnostics.directory=/capi/logs
      - capi.consul.discovery.enabled=true
      - capi.consul.discovery.timer.interval=20
      - capi.consul.host=http://localhost:8500
      - capi.manager.security.enabled=false
    volumes:
      - ./logs:/capi/logs
    networks:
      capi-network:
networks:
  capi-network:
```


 