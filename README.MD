*Thingsplode Connect - blazingly fast, super-light communication library for anti-fragile cloud architecture.*

*Work in progress*

# Use cases
* shines in embedded environment where CPU cycles are precious;
* scales in the cloud, when millions of concurrent connections need to be served;

# Packed with features, yet easy to use
*Thingsplode Synapse Proxy*
Offers containerless communication:
- [x] REST over HTTP
- [] REST over HTTPS
- [] Websocket
- [] MQTT

Hides the complexity of micro-service communication for you. It provides:
- [x] generated Swagger definition for your published endpoints;
- [x] built-in Swagger UI for browsing and calling services;
- [x] HTTP file download support;
- [x] message introspectors on the Endpoint (server) and Endpoint Proxy (client);
- [] load balancer with partitioning / fail-over;
- [] circuit breaker (fast failing of connections);
- [] replicated service registry;
- [] reliable messaging;
- [] error handling and message resend mechanism;
- [] built-in performance monitoring
** pluggable serialization:
- [x] json with Jackson
- [] json with Gson
- [] message pack
- [] google protocol buffer

*Thingsplode Synapse Endpoint (Server)*
- [x] request/response pattern
- [x] rpc pattern
- [] publish/subscribe patterns
- [] publish one service over multiple channels (Websocket / Rest / MqTT)
- [] reactive programming

*Thingsplode Synapse Endpoint Proxy (Client)*
- [x] client proxy with sequential/blocking dispatching;
- [x] client proxy with asynchronous message dispatching;
- [] message pipelining on the Endpoint (Server) side (see HTTP pipelining);

# Speed
Some architectural choices are supporting the performance of the library:
- [x] non-blocking message processing 
- [] reactive programming in the user space
- [] Unix domain sockets for micro-services within the same Docker container (or other host-only communication)
- [] memory mapped files for persistence

# Supported Message patterns

## Request/Response

- [x] The client service sends a Request to the serving service and expects a Response;

## Event

- [] The client can signal events towards the service service (server) in a fire and forget manner.

## Command/Result

- [] The serving service sends commands to the client and expects a command execution result.

## Push Notification

- [] A notification is sent by the serving service to the client.