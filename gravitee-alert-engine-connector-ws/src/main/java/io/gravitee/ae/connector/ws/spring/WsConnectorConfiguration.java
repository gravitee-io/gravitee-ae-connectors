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
package io.gravitee.ae.connector.ws.spring;

import io.gravitee.ae.connector.core.spring.CoreConnectorConfiguration;
import io.gravitee.ae.connector.ws.WsEventProducer;
import io.gravitee.ae.connector.ws.WsTriggerProvider;
import io.gravitee.ae.connector.ws.command.spring.CommandConfiguration;
import io.gravitee.ae.connector.ws.configuration.ConnectorConfiguration;
import io.gravitee.ae.connector.ws.listener.ListenerManager;
import io.gravitee.ae.connector.ws.listener.ListenerManagerImpl;
import io.gravitee.alert.api.event.EventProducer;
import io.gravitee.alert.api.trigger.TriggerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import({ CommandConfiguration.class, CoreConnectorConfiguration.class })
public class WsConnectorConfiguration {

    @Bean
    public ConnectorConfiguration websocketConnectorConfiguration(Environment environment) {
        return new ConnectorConfiguration(environment);
    }

    @Bean
    public EventProducer eventProducer() {
        return new WsEventProducer();
    }

    @Bean
    public TriggerProvider triggerProvider() {
        return new WsTriggerProvider();
    }

    @Bean
    public ListenerManager listenerManager() {
        return new ListenerManagerImpl();
    }
}
