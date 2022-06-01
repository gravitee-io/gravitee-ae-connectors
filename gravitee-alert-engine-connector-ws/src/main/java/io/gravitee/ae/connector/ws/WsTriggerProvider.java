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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.ae.connector.core.ContextBuilder;
import io.gravitee.ae.connector.ws.configuration.ConnectorConfiguration;
import io.gravitee.ae.connector.ws.configuration.Engine;
import io.gravitee.ae.connector.ws.listener.ListenerManager;
import io.gravitee.alert.api.condition.StringCondition;
import io.gravitee.alert.api.event.Context;
import io.gravitee.alert.api.trigger.AbstractTriggerProvider;
import io.gravitee.alert.api.trigger.Trigger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WsTriggerProvider extends AbstractTriggerProvider implements ApplicationContextAware {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(WsTriggerProvider.class);

    public static final String WS_TRIGGER_PATH = "/ws/triggers";

    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<Engine, WebSocketConnector> engineWebSocketConnectorMap = new HashMap<>();

    @Autowired
    private ConnectorConfiguration connectorConfiguration;

    @Autowired
    private ListenerManager listenerManager;

    @Autowired
    private ContextBuilder contextBuilder;

    private Context context;

    private ApplicationContext applicationContext;

    @Override
    public void register(Trigger trigger) {
        try {
            final String installationId = context != null ? context.get(Context.CONTEXT_INSTALLATION) : null;
            if (installationId != null) {
                // Automatically add a constraint on the installation.
                final StringCondition installationFilter = StringCondition.equals(Context.CONTEXT_INSTALLATION, installationId).build();

                if (trigger.getFilters() == null) {
                    trigger.setFilters(new ArrayList<>());
                } else {
                    // Create a new collection and make sure we will be able to add the filter (ie: get rid of SingletonList, UnmodifiableList, ...).
                    trigger.setFilters(new ArrayList<>(trigger.getFilters()));
                }

                trigger.getFilters().add(installationFilter);
            }

            String value = mapper.writeValueAsString(trigger);
            engineWebSocketConnectorMap.forEach((engine, webSocketConnector) -> webSocketConnector.writeTextMessage(value));
        } catch (JsonProcessingException jpe) {
            logger.error("Unexpected error while transforming the trigger into a json format", jpe);
        }
    }

    @Override
    public void unregister(Trigger trigger) {
        try {
            String value = mapper.writeValueAsString(trigger);
            engineWebSocketConnectorMap.forEach((engine, webSocketConnector) -> webSocketConnector.writeTextMessage(value));
        } catch (JsonProcessingException jpe) {
            logger.error("Unexpected error while transforming the trigger into a json format", jpe);
        }
    }

    @Override
    public void addListener(Listener listener) {
        listenerManager.addListener(listener);
    }

    @Override
    protected void doStart() throws Exception {
        context = contextBuilder.build();
        logger.info("AlertEngine connector is enabled. Starting connector.");
        for (Map.Entry<String, Engine> entry : connectorConfiguration.getEngines().entrySet()) {
            Engine engine = entry.getValue();
            WebSocketConnector webSocketConnector = new WebSocketConnector(engine, WS_TRIGGER_PATH);
            applicationContext.getAutowireCapableBeanFactory().autowireBean(webSocketConnector);
            webSocketConnector.start();
            engineWebSocketConnectorMap.put(engine, webSocketConnector);
        }
    }

    @Override
    protected void doStop() throws Exception {
        for (WebSocketConnector webSocketConnector : engineWebSocketConnectorMap.values()) {
            webSocketConnector.stop();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
