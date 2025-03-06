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

import io.gravitee.ae.connector.ws.configuration.Engine;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpConnector extends AbstractConnector<HttpConnector> {

    private final Logger logger = LoggerFactory.getLogger(HttpConnector.class);

    private final Engine engine;
    private final String path;

    public HttpConnector(Engine engine, String path) {
        this.engine = engine;
        this.path = path;
    }

    @Override
    protected void doStart() {
        if (enabled) {
            logger.info("AlertEngine connector is enabled. Starting http connector.");
            initHttpClient(engine);
            logger.info("Channel is ready to send data to Alert Engine through http");
        } else {
            logger.info("AlertEngine connector is disabled.");
        }
    }

    @Override
    public Future<Void> writeTextMessage(String text) {
        final Promise<Void> promise = Promise.promise();

        if (httpClient != null) {
            RequestOptions requestOptions = new RequestOptions()
                .setURI(path)
                .setHeaders(getDefaultHeaders(engine))
                .setMethod(HttpMethod.POST)
                .setTimeout(connectorConfiguration.getRequestTimeout());

            httpClient
                .request(requestOptions)
                .onFailure(promise::fail)
                .onSuccess(httpClientRequest -> {
                    // Connection is made, lets continue.
                    httpClientRequest
                        .send(Buffer.buffer(text))
                        .onFailure(promise::fail)
                        .onSuccess(httpResponse -> {
                            if (httpResponse.statusCode() >= 200 && httpResponse.statusCode() <= 299) {
                                promise.complete();
                                logger.debug("Events successfully sent.");
                            } else {
                                promise.fail("Unable to send events. Server replies with status " + httpResponse.statusCode());
                            }
                        });
                });
        } else {
            promise.fail("The connector is not yet ready");
        }

        return promise.future();
    }
}
