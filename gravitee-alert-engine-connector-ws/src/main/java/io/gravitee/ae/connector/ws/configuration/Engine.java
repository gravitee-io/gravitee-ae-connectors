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
package io.gravitee.ae.connector.ws.configuration;

import io.gravitee.ae.connector.ws.Endpoint;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.*;
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
    private EngineSecurity security;
    private EngineSsl sslConfig;

    public Engine() {}

    public Engine(ConnectorConfiguration connectorConfiguration, List<Endpoint> endpoints, EngineSecurity security, EngineSsl sslConfig) {
        this.connectorConfiguration = connectorConfiguration;
        this.endpoints = endpoints;
        this.security = security;
        this.sslConfig = sslConfig;
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
            httpClientOptions.setTrustAll(sslConfig.isTrustAll());
            httpClientOptions.setVerifyHost(sslConfig.isHostnameVerifier());
            setKeyStoreOptions(httpClientOptions);
            setTruststoreOptions(httpClientOptions);
        }

        setProxyOptions(target, httpClientOptions);
        return httpClientOptions;
    }

    private void setProxyOptions(URI target, HttpClientOptions httpClientOptions) {
        if (connectorConfiguration.isUseSystemProxy()) {
            ProxyOptions proxyOptions = new ProxyOptions().setType(ProxyType.valueOf(connectorConfiguration.getProxyType()));
            if (HTTPS_SCHEME.equals(target.getScheme())) {
                httpClientOptions.setProxyOptions(
                    proxyOptions
                        .setHost(connectorConfiguration.getProxyHttpsHost())
                        .setPort(connectorConfiguration.getProxyHttpsPort())
                        .setUsername(connectorConfiguration.getProxyHttpsUsername())
                        .setPassword(connectorConfiguration.getProxyHttpsPassword())
                );
            } else {
                httpClientOptions.setProxyOptions(
                    proxyOptions
                        .setHost(connectorConfiguration.getProxyHttpHost())
                        .setPort(connectorConfiguration.getProxyHttpPort())
                        .setUsername(connectorConfiguration.getProxyHttpUsername())
                        .setPassword(connectorConfiguration.getProxyHttpPassword())
                );
            }
        }
    }

    private void setTruststoreOptions(HttpClientOptions httpClientOptions) {
        if (sslConfig.getTruststoreType() != null) {
            switch (sslConfig.getTruststoreType().toUpperCase()) {
                case KEYSTORE_FORMAT_JKS:
                    httpClientOptions.setTrustStoreOptions(
                        new JksOptions().setPath(sslConfig.getTruststorePath()).setPassword(sslConfig.getTruststorePassword())
                    );
                    break;
                case KEYSTORE_FORMAT_PKCS12:
                    httpClientOptions.setPfxTrustOptions(
                        new PfxOptions().setPath(sslConfig.getTruststorePath()).setPassword(sslConfig.getTruststorePassword())
                    );
                    break;
                case KEYSTORE_FORMAT_PEM:
                    httpClientOptions.setPemTrustOptions(new PemTrustOptions().addCertPath(sslConfig.getTruststorePath()));
                    break;
                default:
                    //Do nothing
                    break;
            }
        }
    }

    private void setKeyStoreOptions(HttpClientOptions httpClientOptions) {
        if (sslConfig.getKeystoreType() != null) {
            switch (sslConfig.getKeystoreType().toUpperCase()) {
                case KEYSTORE_FORMAT_JKS:
                    httpClientOptions.setKeyStoreOptions(
                        new JksOptions().setPath(sslConfig.getKeystorePath()).setPassword(sslConfig.getKeystorePassword())
                    );
                    break;
                case KEYSTORE_FORMAT_PKCS12:
                    httpClientOptions.setPfxKeyCertOptions(
                        new PfxOptions().setPath(sslConfig.getKeystorePath()).setPassword(sslConfig.getKeystorePassword())
                    );
                    break;
                case KEYSTORE_FORMAT_PEM:
                    httpClientOptions.setPemKeyCertOptions(
                        new PemKeyCertOptions().setCertPaths(sslConfig.getKeystorePemCerts()).setKeyPaths(sslConfig.getKeystorePemKeys())
                    );
                    break;
                default:
                    //Do nothing
                    break;
            }
        }
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

    public EngineSecurity getSecurity() {
        return security;
    }

    public void setSecurity(EngineSecurity value) {
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
}
