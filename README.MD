*Thingsplode Synapse - blazingly fast, super-light communication library for anti-fragile cloud architecture.*

*_Work in progress_* - version 1.0.0-snapshot

# Use cases
* shines in embedded environment where CPU cycles are precious;
* scales in the cloud, when millions of concurrent connections need to be served;

# Packed with features, yet easy to use
## Containerless communication
Supported protocols:
- [x] REST over HTTP (simplicity);
- [ ] REST over HTTPS (simplicity and security);
- [ ] Websocket (bidirectional communication for mobile and web);
- [ ] MQTT (small size for IoT);
- [ ] Unix domain sockets for microservices within the same Docker container (or other host-only communication) (speed);

## Simplified microservice communication
Hides the complexity of microservice communication for you. It provides:
- [x] generated Swagger definition for your published endpoints;
- [x] built-in Swagger UI for browsing and calling services;
- [x] HTTP file download support;
- [x] message introspectors on the Endpoint (server) and Endpoint Proxy (client);
- [ ] load balancer with partitioning / fail-over;
- [ ] circuit breaker (fast failing of connections);
- [ ] replicated service registry;
- [ ] reliable messaging;
- [ ] error handling and message resend mechanism;
- [ ] built-in performance monitoring
### Pluggable serialization:
- [x] json with Jackson
- [ ] json with Gson
- [ ] message pack
- [ ] google protocol buffer

## Synapse Endpoint (Server)
- [x] request/response pattern
- [x] rpc pattern
- [ ] publish/subscribe patterns
- [ ] publish one service over multiple channels (Websocket / Rest / MqTT)
- [ ] reactive programming

## Synapse Endpoint Proxy (Client)
- [x] client proxy with sequential/blocking dispatching;
- [x] client proxy with asynchronous message dispatching;
- [ ] message pipelining on the Endpoint (Server) side (see HTTP pipelining);

## Non functional features
Some architectural choices are supporting the performance of the library:
- [x] non-blocking message processing (speed)
- [ ] reactive programming in the user space
- [ ] memory mapped files for persistence (speed)

# Supported Message patterns

## Request/Response

- [x] The client service sends a Request to the serving service and expects a Response;

## Event

- [ ] The client can signal events towards the service service (server) in a fire and forget manner.

## Command/Result

- [ ] The serving service sends commands to the client and expects a command execution result.

## Push Notification

- [ ] A notification is sent by the serving service to the client.