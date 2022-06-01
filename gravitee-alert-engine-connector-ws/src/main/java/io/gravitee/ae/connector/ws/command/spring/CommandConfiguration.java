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
package io.gravitee.ae.connector.ws.command.spring;

import io.gravitee.ae.connector.ws.command.CommandHandlerManager;
import io.gravitee.ae.connector.ws.command.CommandHandlerManagerImpl;
import io.gravitee.ae.connector.ws.command.discovery.NodeDiscoveryCommandHandler;
import io.gravitee.ae.connector.ws.command.notification.AlertNotificationCommandHandler;
import io.gravitee.ae.connector.ws.command.resolver.ResolvePropertyCommandHandler;
import org.springframework.context.annotation.Bean;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CommandConfiguration {

    @Bean
    public CommandHandlerManager commandHandlerManager() {
        return new CommandHandlerManagerImpl();
    }

    @Bean
    public NodeDiscoveryCommandHandler nodeDiscoveryCommandHandler() {
        return new NodeDiscoveryCommandHandler();
    }

    @Bean
    public AlertNotificationCommandHandler alertNotificationCommandHandler() {
        return new AlertNotificationCommandHandler();
    }

    @Bean
    public ResolvePropertyCommandHandler resolvePropertyCommandHandler() {
        return new ResolvePropertyCommandHandler();
    }
}
