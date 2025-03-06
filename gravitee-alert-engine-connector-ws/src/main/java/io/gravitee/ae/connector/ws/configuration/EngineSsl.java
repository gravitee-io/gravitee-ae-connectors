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

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EngineSsl {

    /**
     * Alert Engine ssl keystore type. (jks, pkcs12,)
     */
    private String keystoreType;

    /**
     * Alert Engine ssl keystore path.
     */
    private String keystorePath;

    /**
     * Alert Engine ssl keystore password.
     */
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
    private boolean trustAll;

    /**
     * Alert Engine ssl truststore hostname verifier.
     */
    private boolean hostnameVerifier;

    /**
     * Alert Engine ssl truststore type.
     */
    private String truststoreType;

    /**
     * Alert Engine ssl truststore path.
     */
    private String truststorePath;

    /**
     * Alert Engine ssl truststore password.
     */
    private String truststorePassword;

    public String getKeystoreType() {
        return keystoreType;
    }

    public EngineSsl setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
        return this;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public EngineSsl setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
        return this;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public EngineSsl setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }

    public List<String> getKeystorePemCerts() {
        return keystorePemCerts;
    }

    public EngineSsl setKeystorePemCerts(List<String> keystorePemCerts) {
        this.keystorePemCerts = keystorePemCerts;
        return this;
    }

    public List<String> getKeystorePemKeys() {
        return keystorePemKeys;
    }

    public EngineSsl setKeystorePemKeys(List<String> keystorePemKeys) {
        this.keystorePemKeys = keystorePemKeys;
        return this;
    }

    public boolean isTrustAll() {
        return trustAll;
    }

    public EngineSsl setTrustAll(boolean trustAll) {
        this.trustAll = trustAll;
        return this;
    }

    public boolean isHostnameVerifier() {
        return hostnameVerifier;
    }

    public EngineSsl setHostnameVerifier(boolean hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    public String getTruststoreType() {
        return truststoreType;
    }

    public EngineSsl setTruststoreType(String truststoreType) {
        this.truststoreType = truststoreType;
        return this;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public EngineSsl setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
        return this;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public EngineSsl setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EngineSsl engineSsl = (EngineSsl) o;
        return (
            trustAll == engineSsl.trustAll &&
            hostnameVerifier == engineSsl.hostnameVerifier &&
            Objects.equals(keystoreType, engineSsl.keystoreType) &&
            Objects.equals(keystorePath, engineSsl.keystorePath) &&
            Objects.equals(keystorePassword, engineSsl.keystorePassword) &&
            Objects.equals(keystorePemCerts, engineSsl.keystorePemCerts) &&
            Objects.equals(keystorePemKeys, engineSsl.keystorePemKeys) &&
            Objects.equals(truststoreType, engineSsl.truststoreType) &&
            Objects.equals(truststorePath, engineSsl.truststorePath) &&
            Objects.equals(truststorePassword, engineSsl.truststorePassword)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            keystoreType,
            keystorePath,
            keystorePassword,
            keystorePemCerts,
            keystorePemKeys,
            trustAll,
            hostnameVerifier,
            truststoreType,
            truststorePath,
            truststorePassword
        );
    }
}
