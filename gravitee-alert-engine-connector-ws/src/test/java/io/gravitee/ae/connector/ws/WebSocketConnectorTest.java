/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.ae.connector.ws;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.ae.connector.ws.command.CommandHandlerManager;
import io.gravitee.ae.connector.ws.configuration.ConnectorConfiguration;
import io.gravitee.ae.connector.ws.configuration.Engine;
import io.gravitee.ae.connector.ws.configuration.EngineSecurity;
import io.gravitee.ae.connector.ws.configuration.EngineSsl;
import io.gravitee.ae.connector.ws.listener.ListenerManager;
import io.gravitee.node.api.Node;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.internal.VertxInternal;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author GraviteeSource Team
 */
class WebSocketConnectorTest {

    private Vertx vertx;
    private WebSocketConnector connector;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connector != null) {
            connector.stop();
        }
        vertx.close().await(10, TimeUnit.SECONDS);
    }

    @Test
    void should_reuse_websocket_client_across_failed_attempts() throws Exception {
        // Nothing listens on this port: every attempt fails
        connector = createConnector("http://localhost:1");
        long before = countWebSocketClients();

        for (int i = 0; i < 5; i++) {
            Promise<WebSocket> promise = Promise.promise();
            ReflectionTestUtils.invokeMethod(connector, "doConnect", promise);
            await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> promise.future().isComplete());
            assertTrue(promise.future().failed());
        }

        assertEquals(1, countWebSocketClients() - before);
    }

    @Test
    void should_reuse_websocket_client_across_reconnections() throws Exception {
        // The server closes every accepted connection shortly after, which makes the connector reconnect immediately
        AtomicInteger connections = new AtomicInteger();
        HttpServer server = vertx
            .createHttpServer()
            .webSocketHandler(ws -> {
                connections.incrementAndGet();
                // Leave the connector time to register its handlers before closing
                vertx.setTimer(100, timerId -> ws.close());
            })
            .listen(0)
            .await(10, TimeUnit.SECONDS);

        connector = createConnector("http://localhost:" + server.actualPort());
        long before = countWebSocketClients();

        connector.start();
        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> connections.get() > 5);

        assertEquals(1, countWebSocketClients() - before);
    }

    private WebSocketConnector createConnector(String url) {
        ConnectorConfiguration connectorConfiguration = mock(ConnectorConfiguration.class);
        when(connectorConfiguration.getConnectTimeout()).thenReturn(2000);
        when(connectorConfiguration.getIdleTimeout()).thenReturn(30000);
        when(connectorConfiguration.getMaxPoolSize()).thenReturn(10);

        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(new Endpoint(url));
        Engine engine = new Engine(connectorConfiguration, endpoints, new EngineSecurity(null, null), mock(EngineSsl.class));

        WebSocketConnector webSocketConnector = new WebSocketConnector(engine, WsTriggerProvider.WS_TRIGGER_PATH);
        ReflectionTestUtils.setField(webSocketConnector, "vertx", vertx);
        ReflectionTestUtils.setField(webSocketConnector, "enabled", true);
        ReflectionTestUtils.setField(webSocketConnector, "connectorConfiguration", connectorConfiguration);
        ReflectionTestUtils.setField(webSocketConnector, "listenerManager", mock(ListenerManager.class));
        ReflectionTestUtils.setField(webSocketConnector, "node", mock(Node.class));
        ReflectionTestUtils.setField(webSocketConnector, "commandHandlerManager", mock(CommandHandlerManager.class));
        return webSocketConnector;
    }

    private long countWebSocketClients() throws Exception {
        Object closeFuture = ((VertxInternal) vertx).closeFuture();
        Field field = closeFuture.getClass().getDeclaredField("children");
        field.setAccessible(true);
        Map<?, ?> children = (Map<?, ?>) field.get(closeFuture);
        return children == null ? 0 : children.keySet().stream().filter(WebSocketClient.class::isInstance).count();
    }
}
