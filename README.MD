*Thingsplode Connect - blazingly fast, superlight communication library for anti-fragile cloud architecture.*

*Work in progress*

# Use cases
* shines in embedded environment where CPU cycles are precious;
* scales in the cloud, when millions of concurrent connections need to be served;

# Main features
*Thingsplode Connect Proxy*
Offers containerless REST, WEBSOCKET, MQTT and Domain Socket communication;
Hides the complexity of micro-service communication for you. It implements:
* load balancer with partitioning / fail-over
* circuit breaker (fast failing connection library)
* replicated service registry
* reliable messaging
* error handling and resend

*Thingsplode Connect Endpoint*
* rpc and publish/subscribe patterns
* publish one service over multiple channels (Websocket / Rest / MqTT)
* response performance monitoring
* reactive programming

# Speed
Some architectural choices are supporting the performance of the library:
* non-blocking, reactive message processing (using LMAX disruptor)
* Unix domain sockets for micro-services within the same Docker container (or other host-only communication)
* memory mapped files for persistence

# Message patterns

## Request/Response

The client service sends a Request to the serving service and expects a Response;

## Event

The client can signal events towards the service service (server) in a fire and forget manner.

## Command/Result

The serving service sends commands to the client and expects a command execution result.

## PushNotification

A notification is sent by the serving service to the client.