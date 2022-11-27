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
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConnectorConfiguration {

    private static final String DEFAULT_ENDPOINT = "http://localhost:8072";
    private static final String DEFAULT_ENGINE_NAME = "default";

    private final Environment environment;

    public ConnectorConfiguration(Environment environment) {
        this.environment = environment;
    }

    /**
     * Alert Engine basic auth login.
     */
    @Value("${alerts.alert-engine.ws.security.username:#{null}}")
    private String username;

    /**
     * Alert Engine basic auth password.
     */
    @Value("${alerts.alert-engine.ws.security.password:#{null}}")
    private String password;

    /**
     * Alert Engine ssl keystore type. (jks, pkcs12,)
     */
    @Value("${alerts.alert-engine.ws.ssl.keystore.type:#{null}}")
    private String keystoreType;

    /**
     * Alert Engine ssl keystore path.
     */
    @Value("${alerts.alert-engine.ws.ssl.keystore.path:#{null}}")
    private String keystorePath;

    /**
     * Alert Engine ssl keystore password.
     */
    @Value("${alerts.alert-engine.ws.ssl.keystore.password:#{null}}")
    private String keystorePassword;

    /**
     * Alert Engine ssl pem certs paths
     */
    private List<String> keystorePemCerts;

    /**
     * Alert Engine ssl pem keys paths
     */
    private List<String> keystorePemKeys;

    /**
     * Alert Engine ssl truststore trustall.
     */
    @Value("${alerts.alert-engine.ws.ssl.trustall:false}")
    private boolean trustAll;

    /**
     * Alert Engine ssl truststore hostname verifier.
     */
    @Value("${alerts.alert-engine.ws.ssl.verifyHostname:true}")
    private boolean hostnameVerifier;

    /**
     * Alert Engine ssl truststore type.
     */
    @Value("${alerts.alert-engine.ws.ssl.truststore.type:#{null}}")
    private String truststoreType;

    /**
     * Alert Engine ssl truststore path.
     */
    @Value("${alerts.alert-engine.ws.ssl.truststore.path:#{null}}")
    private String truststorePath;

    /**
     * Alert Engine ssl truststore password.
     */
    @Value("${alerts.alert-engine.ws.ssl.truststore.password:#{null}}")
    private String truststorePassword;

    /**
     * Connection timeout. Default to 5000.
     */
    @Value("${alerts.alert-engine.ws.connectTimeout:5000}")
    private int connectTimeout;

    /**
     * Request timeout (useful when relying on http to send events). Default is 2000ms.
     */
    @Value("${alerts.alert-engine.ws.requestTimeout:2000}")
    private int requestTimeout;

    /**
     * Idle timeout. After this duration, the connection will be released.
     * Default is 120000 ms (2 minutes).
     */
    @Value("${alerts.alert-engine.ws.idleTimeout:120000}")
    private int idleTimeout;

    /**
     * Indicates if connection keep alive is enabled or not.
     * Default is true.
     */
    @Value("${alerts.alert-engine.ws.keepAlive:true}")
    private boolean keepAlive;

    /**
     * Indicates if pipelining is enabled or not. When pipelining is enabled, mulitple event packets will be sent in a single connection without waiting for the previous responses.
     * Enabling pipeline can increase performances.
     * Default is true.
     */
    @Value("${alerts.alert-engine.ws.pipelining:true}")
    private boolean pipelining;

    /**
     * Indicates if compression is enabled when sending events. The compression must also be enabled on alert engine ingester.
     * Default is true.
     */
    @Value("${alerts.alert-engine.ws.tryCompression:true}")
    private boolean tryCompression;

    /**
     * Set the maximum number of connections (useful when relying on http to send events). Default is 50.
     */
    @Value("${alerts.alert-engine.ws.maxPoolSize:50}")
    private int maxPoolSize;

    /**
     * Set bulk events size. Default is 100.
     */
    @Value("${alerts.alert-engine.ws.bulkEventsSize:100}")
    private int bulkEventsSize;

    /**
     * Set the duration to wait for bulk events to be ready for sending.
     * Ex: a value of 100ms means that if after 100ms the number of events to send is below to <code>bulkEventSize</code>, then the events will be sent.
     * If the number of events is larger than <code>bulkEventSize</code>, then a bulk of <code>bulkEventSize</code> events will be sent immediately.
     * Default is 100ms.
     */
    @Value("${alerts.alert-engine.ws.bulkEventsWait:100}")
    private int bulkEventsWait;

    /**
     * Indicates if events should be sent over http or not.
     * By default, to keep the same behavior of the previous version, events are sent over a websocket connection.
     * The default behavior will switch to http in a next future version.
     */
    @Value("${alerts.alert-engine.ws.sendEventsOnHttp:false}")
    private boolean sendEventsOnHttp;

    @Value("${httpClient.proxy.type:HTTP}")
    private String proxyType;

    @Value("${alerts.alert-engine.ws.useSystemProxy:false}")
    private boolean useSystemProxy;

    @Value("${httpClient.proxy.http.host:#{systemProperties['http.proxyHost'] ?: 'localhost'}}")
    private String proxyHttpHost;

    @Value("${httpClient.proxy.http.port:#{systemProperties['http.proxyPort'] ?: 3128}}")
    private int proxyHttpPort;

    @Value("${httpClient.proxy.http.username:#{null}}")
    private String proxyHttpUsername;

    @Value("${httpClient.proxy.http.password:#{null}}")
    private String proxyHttpPassword;

    @Value("${httpClient.proxy.https.host:#{systemProperties['https.proxyHost'] ?: 'localhost'}}")
    private String proxyHttpsHost;

    @Value("${httpClient.proxy.https.port:#{systemProperties['https.proxyPort'] ?: 3128}}")
    private int proxyHttpsPort;

    @Value("${httpClient.proxy.https.username:#{null}}")
    private String proxyHttpsUsername;

    @Value("${httpClient.proxy.https.password:#{null}}")
    private String proxyHttpsPassword;

    private Map<String, Engine> engines;

    public Engine getDefaultEngine() {
        return getEngines().get(DEFAULT_ENGINE_NAME);
    }

    private List<String> initializeEndpoints(String key) {
        String fullKey = String.format("%s[%s]", key, 0);
        List<String> result = new ArrayList<>();

        while (environment.containsProperty(fullKey)) {
            String url = environment.getProperty(fullKey);
            result.add(url);

            fullKey = String.format("%s[%s]", key, result.size());
        }

        // Use default host if required
        if (result.isEmpty()) {
            result.add(DEFAULT_ENDPOINT);
        }

        return result;
    }

    private Map<String, Engine> initializeEngines() {
        String keyInitial = "alerts.alert-engine.engines.";
        Map<String, Engine> result = new HashMap<>();

        // try first with engines tag
        final var engineNames = new HashSet<String>();
        ((AbstractEnvironment) environment).getPropertySources()
            .stream()
            .collect(Collectors.toList())
            .stream()
            .filter(propertySource -> propertySource instanceof MapPropertySource)
            .forEach(propertySource -> {
                ((MapPropertySource) propertySource).getSource()
                    .forEach((k, v) -> {
                        if (k.startsWith(keyInitial)) {
                            String replace = k.replace(keyInitial, "");
                            String name = replace.substring(0, replace.indexOf("."));
                            engineNames.add(name);
                        }
                    });
            });

        engineNames.forEach(name -> {
            String key = String.format("%s%s", keyInitial, name);
            List<Endpoint> endpointList = initializeEndpoints(key + ".endpoints").stream().map(Endpoint::new).collect(Collectors.toList());
            String uname = environment.getProperty(key + ".security.username");
            String pass = environment.getProperty(key + ".security.password");

            result.put(name, new Engine(this, endpointList, new Engine.Security(uname, pass)));
        });

        if (!result.isEmpty()) {
            if (result.get(DEFAULT_ENGINE_NAME) == null) {
                throw new RuntimeException("Default engine is not found! You need to have a default engine in your configuration.");
            }
        } else { // for backward compatibility
            Engine defaultEngine = new Engine(
                this,
                initializeEndpoints("alerts.alert-engine.ws.endpoints").stream().map(Endpoint::new).collect(Collectors.toList()),
                new Engine.Security(this.getUsername(), this.getPassword())
            );

            result.put(DEFAULT_ENGINE_NAME, defaultEngine);
        }

        return result;
    }

    // Property methods
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public List<String> getKeystorePemCerts() {
        if (keystorePemCerts == null) {
            keystorePemCerts = initializeKeystorePemCerts("alerts.alert-engine.ws.ssl.keystore.certs[%s]");
        }

        return keystorePemCerts;
    }

    private List<String> initializeKeystorePemCerts(String property) {
        String key = String.format(property, 0);
        List<String> values = new ArrayList<>();

        while (environment.containsProperty(key)) {
            values.add(environment.getProperty(key));
            key = String.format(property, values.size());
        }

        return values;
    }

    public void setKeystorePemCerts(List<String> keystorePemCerts) {
        this.keystorePemCerts = keystorePemCerts;
    }

    public List<String> getKeystorePemKeys() {
        if (keystorePemKeys == null) {
            keystorePemKeys = initializeKeystorePemCerts("alerts.alert-engine.ws.ssl.keystore.keys[%s]");
        }

        return keystorePemKeys;
    }

    public void setKeystorePemKeys(List<String> keystorePemKeys) {
        this.keystorePemKeys = keystorePemKeys;
    }

    public boolean isTrustAll() {
        return trustAll;
    }

    public void setTrustAll(boolean trustAll) {
        this.trustAll = trustAll;
    }

    public boolean isHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(boolean hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public String getTruststoreType() {
        return truststoreType;
    }

    public void setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public int getBulkEventsSize() {
        return bulkEventsSize;
    }

    public void setBulkEventsSize(int bulkEventsSize) {
        this.bulkEventsSize = bulkEventsSize;
    }

    public int getBulkEventsWait() {
        return bulkEventsWait;
    }

    public void setBulkEventsWait(int bulkEventsWait) {
        this.bulkEventsWait = bulkEventsWait;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public boolean isSendEventsOnHttp() {
        return sendEventsOnHttp;
    }

    public void setSendEventsOnHttp(boolean sendEventsOnHttp) {
        this.sendEventsOnHttp = sendEventsOnHttp;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isPipelining() {
        return pipelining;
    }

    public void setPipelining(boolean pipelining) {
        this.pipelining = pipelining;
    }

    public boolean isTryCompression() {
        return tryCompression;
    }

    public void setTryCompression(boolean tryCompression) {
        this.tryCompression = tryCompression;
    }

    public Map<String, Engine> getEngines() {
        if (engines == null) {
            engines = initializeEngines();
        }

        return engines;
    }

    public void setEngines(Map<String, Engine> engines) {
        this.engines = engines;
    }

    public String getProxyType() {
        return proxyType;
    }

    public ConnectorConfiguration setProxyType(String proxyType) {
        this.proxyType = proxyType;
        return this;
    }

    public boolean isUseSystemProxy() {
        return useSystemProxy;
    }

    public ConnectorConfiguration setUseSystemProxy(boolean useSystemProxy) {
        this.useSystemProxy = useSystemProxy;
        return this;
    }

    public String getProxyHttpHost() {
        return proxyHttpHost;
    }

    public ConnectorConfiguration setProxyHttpHost(String proxyHttpHost) {
        this.proxyHttpHost = proxyHttpHost;
        return this;
    }

    public int getProxyHttpPort() {
        return proxyHttpPort;
    }

    public ConnectorConfiguration setProxyHttpPort(int proxyHttpPort) {
        this.proxyHttpPort = proxyHttpPort;
        return this;
    }

    public String getProxyHttpUsername() {
        return proxyHttpUsername;
    }

    public ConnectorConfiguration setProxyHttpUsername(String proxyHttpUsername) {
        this.proxyHttpUsername = proxyHttpUsername;
        return this;
    }

    public String getProxyHttpPassword() {
        return proxyHttpPassword;
    }

    public ConnectorConfiguration setProxyHttpPassword(String proxyHttpPassword) {
        this.proxyHttpPassword = proxyHttpPassword;
        return this;
    }

    public String getProxyHttpsHost() {
        return proxyHttpsHost;
    }

    public ConnectorConfiguration setProxyHttpsHost(String proxyHttpsHost) {
        this.proxyHttpsHost = proxyHttpsHost;
        return this;
    }

    public int getProxyHttpsPort() {
        return proxyHttpsPort;
    }

    public ConnectorConfiguration setProxyHttpsPort(int proxyHttpsPort) {
        this.proxyHttpsPort = proxyHttpsPort;
        return this;
    }

    public String getProxyHttpsUsername() {
        return proxyHttpsUsername;
    }

    public ConnectorConfiguration setProxyHttpsUsername(String proxyHttpsUsername) {
        this.proxyHttpsUsername = proxyHttpsUsername;
        return this;
    }

    public String getProxyHttpsPassword() {
        return proxyHttpsPassword;
    }

    public ConnectorConfiguration setProxyHttpsPassword(String proxyHttpsPassword) {
        this.proxyHttpsPassword = proxyHttpsPassword;
        return this;
    }
}
