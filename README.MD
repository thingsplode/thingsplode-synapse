*Thingsplode Synapse - blazingly fast, super-light communication library for anti-fragile cloud architecture.*

*_Work in progress_* - version 1.0.0-snapshot

# Use cases
* shines in embedded environment where CPU cycles are precious;
* scales in the cloud, when millions of concurrent connections need to be served;

# Packed with features, yet easy to use
## Containerless communication
Supported protocols:
- [x] REST over HTTP (simplicity);
- [x] File streaming (download) over HTTP
- [ ] File streaming (upload) over HTTP - [#2](../../issues/2)
- [ ] REST over HTTPS (simplicity and security);
- [ ] Websocket (bidirectional communication for mobile and web);
- [ ] MQTT (small size for IoT);
- [ ] Unix domain sockets for microservices within the same Docker container (or other host-only communication) (speed) - [#3](../../issues/3);

## Simplified microservice communication
Hides the complexity of microservice communication for you. It provides:
- [x] generated Swagger definition for your published endpoints;
- [x] built-in Swagger UI for browsing and calling services;
- [x] HTTP file download support;
- [x] message introspectors on the Endpoint (server) and Endpoint Proxy (client);
- [ ] load balancer with partitioning / fail-over;
- [ ] circuit breaker (fast failing of connections);
- [ ] replicated service registry - [#4](../../issues/4);
- [ ] reliable messaging;
- [ ] error handling and message resend mechanism;
- [ ] built-in performance monitoring
- [ ] api keys - [#5](../../issues/5);
- [ ] Endpoint Proxy (Client) authentication and authorization - [#6](../../issues/6);
- [ ] message authentication - [#7](../../issues/7);

### Configurable message dispatching modes
- [x] sequential blocking client
- [x] correlated asynchronous message dispatching
- [ ] message pipelining

### Pluggable serialization:
- [x] json with Jackson
- [ ] json with Gson
- [ ] message pack
- [ ] google protocol buffer - [#8](../../issues/8);

## Synapse Endpoint (Server)
- [x] request/response pattern
- [x] rpc pattern
- [ ] publish/subscribe patterns
- [ ] publish one service over multiple channels (Websocket / Rest / MqTT)

## Synapse Endpoint Proxy (Client)
- [x] client proxy with sequential/blocking dispatching;
- [x] client proxy with asynchronous message dispatching;
- [ ] message pipelining on the Endpoint (Server) side (see HTTP pipelining) - [#9](../../issues/9);

## Non functional features
Some architectural choices are supporting the performance of the library:
- [x] non-blocking message processing (speed)
- [ ] reactive programming in the user space
- [ ] memory mapped files for persistence (speed)

# Supported Message patterns

## Request/Response

- [x] The Endpoint Proxy (Client) dispatches a Request to the Endpoint service (Server) and expects a Response;

## Event

- [ ] The Endpoint Proxy can signal events towards the Endpoint service (Server) in a fire and forget manner (also with activated reliable messaging).

## Command/Result

- [ ] The Endpoint service (Server) sends commands to the Endpoint Proxy (Client) and expects a Command Execution Result.

## Push Notification

- [ ] A notification is sent by the Endpoint service (Server) to the Endpoint Proxty (Client) also with reliable messaging.

# Other features
- [x] http keepalive support