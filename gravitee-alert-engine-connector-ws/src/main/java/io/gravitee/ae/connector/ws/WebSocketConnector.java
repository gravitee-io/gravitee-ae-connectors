/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.ae.connector.ws;

import io.gravitee.ae.connector.api.command.Command;
import io.gravitee.ae.connector.ws.command.CommandHandlerManager;
import io.gravitee.ae.connector.ws.configuration.Engine;
import io.gravitee.ae.connector.ws.listener.ListenerManager;
import io.gravitee.alert.api.event.EventProducer;
import io.gravitee.alert.api.trigger.TriggerProvider;
import io.gravitee.node.api.Node;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WebSocketConnector extends AbstractConnector<WebSocketConnector> {

    private static final String HTTPS_SCHEME = "https";
    private final Logger logger = LoggerFactory.getLogger(WebSocketConnector.class);

    private static final long PING_HANDLER_DELAY = 5000;

    @Autowired
    private ListenerManager listenerManager;

    @Autowired
    private Node node;

    private WebSocket webSocket;
    private long pongHandlerId;
    private CircuitBreaker circuitBreaker;
    private final Engine engine;
    private final String path;

    @Autowired
    private CommandHandlerManager commandHandlerManager;

    public WebSocketConnector(Engine engine, String path) {
        this.engine = engine;
        this.path = path;
    }

    @Override
    protected void doStart() {
        if (enabled) {
            logger.info("AlertEngine connector is enabled. Starting WS connector.");

            circuitBreaker =
                CircuitBreaker.create(
                    "alert-engine-event-producer",
                    vertx,
                    new CircuitBreakerOptions().setMaxRetries(Integer.MAX_VALUE).setNotificationAddress(null)
                );

            // Back-off retry
            // TODO use jitter
            circuitBreaker.retryPolicy(integer -> 5000L);

            connect();
        } else {
            logger.info("AlertEngine connector is disabled.");
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        if (pongHandlerId != 0L) {
            vertx.cancelTimer(pongHandlerId);
        }
    }

    private void connect() {
        circuitBreaker
            .execute(this::doConnect)
            .onComplete(event -> {
                // The connection has been established
                if (event.succeeded()) {
                    WebSocketConnector.this.webSocket = event.result();

                    // Initialize ping-pong
                    // See RFC 6455 Section <a href="https://tools.ietf.org/html/rfc6455#section-5.5.2"
                    pongHandlerId =
                        vertx.setPeriodic(
                            PING_HANDLER_DELAY,
                            aLong -> webSocket.writePing(Buffer.buffer(node.id() + " - " + node.hostname()))
                        );

                    if (discovery) {
                        logger.info("Discovery mode is enabled, listening for alert engine instances...");
                    }

                    WebSocketConnector.this.webSocket.handler(buffer -> {
                            final String sCommand = buffer.toString();

                            try {
                                Command command = DatabindCodec.mapper().readValue(sCommand, Command.class);

                                commandHandlerManager.handle(
                                    command,
                                    result -> {
                                        if (result != null) {
                                            WebSocketConnector.this.webSocket.writeTextMessage(
                                                    "reply:" + command.id() + ":" + Json.encode(result)
                                                );
                                        }
                                    }
                                );
                            } catch (IOException ioe) {
                                logger.error("Unexpected error while reading command: {}", sCommand, ioe);
                            }
                        });

                    WebSocketConnector.this.webSocket.exceptionHandler(throwable ->
                            logger.error("An error occurred on the websocket connection", throwable)
                        );

                    WebSocketConnector.this.webSocket.pongHandler(data -> logger.debug("Get a pong from Alert Engine server"));

                    WebSocketConnector.this.webSocket.closeHandler(event1 -> {
                            logger.debug("Connection to Alert Engine server has been closed.");

                            if (pongHandlerId != 0L) {
                                vertx.cancelTimer(pongHandlerId);
                            }

                            // Release reference to the websocket connection
                            WebSocketConnector.this.webSocket = null;

                            invokeOnDisconnectionListeners();

                            // How to force to reconnect ?
                            connect();
                        });

                    invokeOnConnectionListeners();
                } else {
                    // Retry the connection
                    connect();
                }
            });
    }

    @Override
    public Future<Void> writeTextMessage(String text) {
        if (webSocket != null) {
            if (!webSocket.writeQueueFull()) {
                return webSocket.writeTextMessage(text);
            } else {
                return Future.failedFuture("An alert event has been skipped, write queue full...");
            }
        }

        return Future.failedFuture("The connector is not yet ready");
    }

    private void doConnect(Promise<WebSocket> webSocketPromise) {
        initHttpClient(engine);
        Endpoint endpoint = engine.currentEndpoint();

        if (endpoint != null) {
            URI target = URI.create(endpoint.getUrl());

            WebSocketConnectOptions webSocketConnectOptions = new WebSocketConnectOptions();
            webSocketConnectOptions.setHeaders(getDefaultHeaders(engine));
            webSocketConnectOptions.setURI(target.getRawPath() + path);

            var connectorConfig = engine.getConnectorConfiguration();

            if (connectorConfig.isUseSystemProxy()) {
                ProxyOptions proxyOptions = new ProxyOptions().setType(ProxyType.valueOf(connectorConfig.getProxyType()));
                if (HTTPS_SCHEME.equals(target.getScheme())) {
                    webSocketConnectOptions.setProxyOptions(
                        proxyOptions
                            .setHost(connectorConfig.getProxyHttpsHost())
                            .setPort(connectorConfig.getProxyHttpsPort())
                            .setUsername(connectorConfig.getProxyHttpsUsername())
                            .setPassword(connectorConfig.getProxyHttpsPassword())
                    );
                } else {
                    webSocketConnectOptions.setProxyOptions(
                        proxyOptions
                            .setHost(connectorConfig.getProxyHttpHost())
                            .setPort(connectorConfig.getProxyHttpPort())
                            .setUsername(connectorConfig.getProxyHttpUsername())
                            .setPassword(connectorConfig.getProxyHttpPassword())
                    );
                }
            }

            httpClient
                .webSocket(webSocketConnectOptions)
                .onSuccess(ws -> {
                    // Re-init endpoint counter
                    engine.resetEndpointRetryCount(endpoint);

                    logger.info("Channel is ready to send data to Alert Engine through websocket from {}", endpoint.getUrl() + path);
                    webSocketPromise.complete(ws);
                })
                .onFailure(throwable -> {
                    logger.error(
                        "An error occurred while trying to connect to the alert engine: {} [{} times]",
                        throwable.getMessage(),
                        engine.getEndpointRetryCount(endpoint)
                    );
                    webSocketPromise.fail(throwable);

                    // Force the HTTP client to close after a defect
                    httpClient.close();
                });
        }
    }

    private void invokeOnConnectionListeners() {
        if (path.equals(WsTriggerProvider.WS_TRIGGER_PATH)) {
            listenerManager
                .getListeners(TriggerProvider.OnConnectionListener.class)
                .forEach(TriggerProvider.OnConnectionListener::doOnConnect);
        } else if (path.equals(WsEventProducer.WS_EVENT_PATH)) {
            listenerManager.getListeners(EventProducer.OnConnectionListener.class).forEach(EventProducer.OnConnectionListener::doOnConnect);
        }
    }

    private void invokeOnDisconnectionListeners() {
        if (path.equals(WsTriggerProvider.WS_TRIGGER_PATH)) {
            listenerManager
                .getListeners(TriggerProvider.OnDisconnectionListener.class)
                .forEach(TriggerProvider.OnDisconnectionListener::doOnDisconnect);
        } else if (path.equals(WsEventProducer.WS_EVENT_PATH)) {
            listenerManager
                .getListeners(EventProducer.OnDisconnectionListener.class)
                .forEach(EventProducer.OnDisconnectionListener::doOnDisconnect);
        }
    }
}
