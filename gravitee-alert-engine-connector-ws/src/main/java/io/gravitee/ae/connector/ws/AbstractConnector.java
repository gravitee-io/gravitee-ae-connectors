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

import io.gravitee.ae.connector.ws.configuration.ConnectorConfiguration;
import io.gravitee.ae.connector.ws.configuration.Engine;
import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.http.HttpHeaders;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractConnector<T> extends AbstractLifecycleComponent<T> {

    private final Logger logger = LoggerFactory.getLogger(AbstractConnector.class);
    protected HttpClient httpClient;

    @Autowired
    protected Vertx vertx;

    @Value("${alerts.alert-engine.enabled:false}")
    protected boolean enabled;

    @Value("${alerts.alert-engine.ws.discovery:true}")
    protected boolean discovery;

    @Autowired
    protected ConnectorConfiguration connectorConfiguration;

    @Override
    protected void doStop() throws Exception {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IllegalStateException ise) {
                logger.warn(ise.getMessage());
            }
        }
    }

    public abstract Future<Void> writeTextMessage(String text);

    protected void initHttpClient(Engine engine) {
        Assert.notNull(engine, "Engine can not be null");
        Endpoint endpoint = engine.nextEndpoint();
        httpClient = vertx.createHttpClient(engine.getHttpClientOptions(endpoint));
    }

    protected MultiMap getDefaultHeaders(Engine engine) {
        MultiMap headers = HeadersMultiMap.httpHeaders();

        // Read configuration to authenticate calls to Elasticsearch (basic authentication only)
        if (engine.getSecurity().getUsername() != null) {
            String basicAuthorizationHeader =
                this.initEncodedAuthorization(engine.getSecurity().getUsername(), engine.getSecurity().getPassword());
            headers.set(HttpHeaders.AUTHORIZATION, basicAuthorizationHeader);
        }

        return headers;
    }

    protected String initEncodedAuthorization(final String username, final String password) {
        final String auth = username + ':' + password;
        final String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedAuth;
    }
}
