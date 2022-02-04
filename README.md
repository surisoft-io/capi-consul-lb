[![CAPI-LB](https://github.com/rodrigoserracoelho/capi-lb/actions/workflows/main.yml/badge.svg)](https://github.com/rodrigoserracoelho/capi-lb/actions/workflows/main.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/surisoft/capi-lb)


# CAPI Load Balancer
## _Light Apache Camel Load Balancer_

## Supports:
* Light API Gateway / Load Balancer powered by Apache Camel dynamics routes.
* Deployments supported: Spring Boot Microservices auto discovery, CAPI Manager Endpoints and Hashicorp Consul
* Optional Spring Security OIDC protected CAPI Manager API.
* Distributed tracing system (Zipkin)
* Metrics (Prometheus)
* Hawtio (JVM management console)
* Load Balancer (Round robin)
* Failover (With and without Round Robin)
* Stick Session (Cookies and Headers)
* Certificate Manager (using the CAPI Manager API)

## Example of an API definition

    {
        "name": "api-name",
    	"context": "your-api-context",
    	"mappingList": [
    		{
    		    "hostname": "your.app.node1",
    		    "port": 8080,
    		    "rootContext": "your-app-context",
                        "ingress": false
    		},
    		{
    		    "hostname": "your.app.node2",
    		    "port": 8080,
    		    "rootContext": "your-app-context",
                        "ingress": true
    		}
    	],
    	"roundRobinEnabled": true,
    	"failoverEnabled": true,
    	"matchOnUriPrefix": true,
    	"httpMethod": ALL,
    	"httpProtocol": "HTTP",
    	"stickySession": true,
    	"stickySessionParam": "SESSION_ID",
    	"stickySessionParamInCookie": true,
    	"removeMe": false,
    	"connectTimeout": 0,
    	"socketTimeout": 0,
    	"swaggerEndpoint": null
    }
### Field Description

* ```context``` (Mandatory) The context wehre users will access your api. (Example: https://domain.com/capi/your-api-context).
* ```mappingList``` (1-N Array) - If you specify more than one mapping then: If failover and round robin are enabled, CAPI will round robin between all healthy endpoints. If only failover is enabled, then one node will be used only as backup.
* ```httpProtocol``` (Mandatory) (HTTP, HTTPS) - If you are exposing on HTTPS it is important to add your certificate to CAPI trust store. CAPI Manager exposes an API for managing your certificates.
* ```httpMethod``` (Default ALL) - If no http method is specified, CAPI will expose all standard methods for your API (GET,POST,PUT,DELETE). If you specify POST, only post calls to your API will be load balanced.
* ```matchOnUriPrefix``` (Default true), if true, you don't need to specify a definition (Swagger) for your API. CAPI will allow all paths. (Example: /your-api-context/clients /your-api-context/customer/foo/bar?action=example).
* ```stickySession``` (Default false) - If you enable sticky sessions then you also need to provide ```stickySessionParam``` and ```stickySessionParamInCookie``` (Example: ```stickySession=true```, ```stickySessionParam=X_KEY```,```stickySessionParamInCookie=true```: CAPI will look for a cookie named X_KEY, and associate the value with a random node, subsequent calls with the same cookie value will be forwarded to the same node. If that node becames unavailable CAPI returns a 503 to the client and starts all over again.)
* ```connectTimeout``` (default 2 minutes) - You can specify the timeout for CAPI to try to connect to your endpoint.
* ```socketTimeout``` (default 2 minutes) - You can specify the timeout for CAPI to wait for a response from your endpoint.
* ```removeMe``` (default true) - If false, CAPI will not only remove the node requesting to be removed, but the entire API. (Example: Node 1 joins the _API-X_, Node 2 joins _API-X_, with ```removeMe=false```, if Node 2 exits _API-X_, the entire _API-X_ will be deleted)


* ```ingress``` (default false) - If one of your mapping is pointing to a Kubernetes ingress, ```ingress``` should be true. This is because Ingress Controller needs to evaluate the Host header to determine to which service to forward the request. Check the documentation here: https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-rules
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
* You need a valid MySQL running instance, with CAPI db created.
* You need Open JDK 14
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
     -Dspring.datasource.url=jdbc:mysql://localhost:3306/capi \
     -Dspring.datasource.username=root \
     -Dspring.datasource.password=root \
     -Dcapi.trust.store.path=/your/path/cacerts \ 
     -Dcapi.trust.store.password=changeit \
     -Dcapi.manager.security.enabled=true \ 
     -Dcapi.manager.security.issuer=https://localhost:8443/auth/realms/master/protocol/openid-connect/certs \
     -jar <CAPI_JAR> > $PWD/logs/capi.log 2>&1 & echo $! > capi.pid

```
In the example above CAPI will be available with CAPI Manager secured and certificate management enabled.
### Install CAPI on Docker (with docker-compose)
Create an _init.sql_ file, for CAPI database to be created on start, with the following script:
```
CREATE DATABASE IF NOT EXISTS capi;
```
You will see this file mapped in the docker-compose.yml below.
```
version: "3"
services:
  capi:
    container_name: capi
    image: surisoft/capi-lb:0.0.1
    ports:
      - "8380:8380"
    environment:
      - hazelcast.diagnostics.enabled=true
      - hazelcast.diagnostics.directory=/capi/logs
      - spring.datasource.url=jdbc:mysql://capi-db:3306/capi
      - spring.datasource.username=root
      - spring.datasource.password=secret
      - capi.manager.security.enabled=false
    volumes:
      - ./logs:/capi/logs
    depends_on:
      - capi-db
    networks:
      capi-network:
  capi-db:
    container_name: capi-db
    image: mysql:latest
    ports:
      - "3306:3306"
    command: --init-file /data/application/init.sql
    volumes:
      - ./init.sql:/data/application/init.sql
    environment:
      - MYSQL_ROOT_USER=root
      - MYSQL_ROOT_PASSWORD=secret
    networks:
      capi-network:
networks:
  capi-network:
```
### Install CAPI on AWS EKS
Create the cluster
```
$ eksctl create cluster -f capi-eks-cluster.yaml 
```
Grab a cup of coffee, it will take a while...
```
$ eksctl utils associate-iam-oidc-provider --region eu-west-1 --cluster capi-demo-1 --approve
```
Apply IAM Policies for Ingress Controller
```
$ kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/aws-alb-ingress-controller/v1.1.8/docs/examples/rbac-role.yaml
```
Apply ALB Ingress Controller
```
$ eksctl create iamserviceaccount \
    --region eu-west-1 \
    --name alb-ingress-controller \
    --namespace kube-system \
    --cluster capi-demo-1 \
    --attach-policy-arn arn:aws:iam::610447901435:policy/ALBIngressControllerIAMPolicy \
    --override-existing-serviceaccounts \
    --approve

$ kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/aws-alb-ingress-controller/v1.1.8/docs/examples/alb-ingress-controller.yaml
```
Install Capi Load Balancer Helm Charts
```
$ helm install "capi" ./capi-lb-charts
```

## Delete
```
$ helm delete capi
$ eksctl delete cluster --name capi-demo-1
```

 