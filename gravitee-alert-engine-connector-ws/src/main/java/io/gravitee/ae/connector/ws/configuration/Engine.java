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
package io.gravitee.ae.connector.ws.configuration;

import io.gravitee.ae.connector.ws.Endpoint;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Engine {

    private static final Logger logger = LoggerFactory.getLogger(Engine.class);
    private static final String KEYSTORE_FORMAT_JKS = "JKS";
    private static final String KEYSTORE_FORMAT_PEM = "PEM";
    private static final String KEYSTORE_FORMAT_PKCS12 = "PKCS12";
    private static final String HTTPS_SCHEME = "https";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;
    protected final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Endpoint, AtomicInteger> endpointsRetryCount = new HashMap<>();
    private Endpoint currentEndpoint;

    private ConnectorConfiguration connectorConfiguration;
    private List<Endpoint> endpoints;
    private Security security;

    public Engine() {}

    public Engine(ConnectorConfiguration connectorConfiguration, List<Endpoint> endpoints, Security security) {
        this.connectorConfiguration = connectorConfiguration;
        this.endpoints = endpoints;
        this.security = security;
    }

    public HttpClientOptions getHttpClientOptions(Endpoint endpoint) {
        URI target = URI.create(endpoint.getUrl());
        final int port = target.getPort() != -1
            ? target.getPort()
            : (HTTPS_SCHEME.equals(target.getScheme()) ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT);
        HttpClientOptions httpClientOptions = new HttpClientOptions();
        httpClientOptions.setConnectTimeout(connectorConfiguration.getConnectTimeout());
        httpClientOptions.setDefaultPort(port);
        httpClientOptions.setDefaultHost(target.getHost());
        httpClientOptions.setMaxPoolSize(connectorConfiguration.getMaxPoolSize());
        httpClientOptions.setIdleTimeout(connectorConfiguration.getIdleTimeout());
        httpClientOptions.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
        httpClientOptions.setPipelining(connectorConfiguration.isPipelining());
        httpClientOptions.setKeepAlive(connectorConfiguration.isKeepAlive());
        httpClientOptions.setTryUseCompression(connectorConfiguration.isTryCompression());

        if (HTTPS_SCHEME.equalsIgnoreCase(target.getScheme())) {
            // Configure SSL
            httpClientOptions.setSsl(true);
            httpClientOptions.setTrustAll(connectorConfiguration.isTrustAll());
            httpClientOptions.setVerifyHost(connectorConfiguration.isHostnameVerifier());

            if (connectorConfiguration.getKeystoreType() != null) {
                if (connectorConfiguration.getKeystoreType().equalsIgnoreCase(KEYSTORE_FORMAT_JKS)) {
                    httpClientOptions.setKeyStoreOptions(
                        new JksOptions()
                            .setPath(connectorConfiguration.getKeystorePath())
                            .setPassword(connectorConfiguration.getKeystorePassword())
                    );
                } else if (connectorConfiguration.getKeystoreType().equalsIgnoreCase(KEYSTORE_FORMAT_PKCS12)) {
                    httpClientOptions.setPfxKeyCertOptions(
                        new PfxOptions()
                            .setPath(connectorConfiguration.getKeystorePath())
                            .setPassword(connectorConfiguration.getKeystorePassword())
                    );
                } else if (connectorConfiguration.getKeystoreType().equalsIgnoreCase(KEYSTORE_FORMAT_PEM)) {
                    httpClientOptions.setPemKeyCertOptions(
                        new PemKeyCertOptions()
                            .setCertPaths(connectorConfiguration.getKeystorePemCerts())
                            .setKeyPaths(connectorConfiguration.getKeystorePemKeys())
                    );
                }
            }

            if (connectorConfiguration.getTruststoreType() != null) {
                if (connectorConfiguration.getTruststoreType().equalsIgnoreCase(KEYSTORE_FORMAT_JKS)) {
                    httpClientOptions.setTrustStoreOptions(
                        new JksOptions()
                            .setPath(connectorConfiguration.getTruststorePath())
                            .setPassword(connectorConfiguration.getTruststorePassword())
                    );
                } else if (connectorConfiguration.getTruststoreType().equalsIgnoreCase(KEYSTORE_FORMAT_PKCS12)) {
                    httpClientOptions.setPfxTrustOptions(
                        new PfxOptions()
                            .setPath(connectorConfiguration.getTruststorePath())
                            .setPassword(connectorConfiguration.getTruststorePassword())
                    );
                } else if (connectorConfiguration.getTruststoreType().equalsIgnoreCase(KEYSTORE_FORMAT_PEM)) {
                    httpClientOptions.setPemTrustOptions(new PemTrustOptions().addCertPath(connectorConfiguration.getTruststorePath()));
                }
            }
        }

        return httpClientOptions;
    }

    public Endpoint nextEndpoint() {
        int size = endpoints.size();
        if (size == 0) {
            currentEndpoint = null;
            return null;
        }

        Endpoint endpoint = endpoints.get(Math.abs(counter.getAndIncrement() % size));
        endpointsRetryCount.computeIfAbsent(endpoint, k -> new AtomicInteger(0));

        int tryConnect = endpointsRetryCount.get(endpoint).incrementAndGet();
        if (tryConnect > 5 && endpoint.isRemovable()) {
            logger.info("Alert engine connector tries to connect to instance at {} 5 times. Removing instance...", endpoint.getUrl());
            endpoints.remove(endpoint);
            return nextEndpoint();
        }

        currentEndpoint = endpoint;
        return endpoint;
    }

    public Endpoint currentEndpoint() {
        return currentEndpoint;
    }

    public void resetEndpointRetryCount(Endpoint endpoint) {
        endpointsRetryCount.get(endpoint).set(0);
    }

    public Integer getEndpointRetryCount(Endpoint endpoint) {
        return endpointsRetryCount.get(endpoint).get();
    }

    // Property methods
    public ConnectorConfiguration getConnectorConfiguration() {
        return connectorConfiguration;
    }

    public void setConnectorConfiguration(ConnectorConfiguration connectorConfiguration) {
        this.connectorConfiguration = connectorConfiguration;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security value) {
        this.security = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Engine engine = (Engine) o;
        return Objects.equals(endpoints, engine.endpoints) && Objects.equals(security, engine.security);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoints, security);
    }

    public static class Security {

        private String username;
        private String password;

        public Security(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String value) {
            this.username = value;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String value) {
            this.password = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Security security = (Security) o;
            return Objects.equals(username, security.username) && Objects.equals(password, security.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, password);
        }
    }
}
