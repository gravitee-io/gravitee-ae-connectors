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
package io.gravitee.ae.connector.ws.command.discovery;

import io.gravitee.ae.connector.api.command.Command;
import io.gravitee.ae.connector.api.command.Handler;
import io.gravitee.ae.connector.api.command.NodeDiscoveryCommand;
import io.gravitee.ae.connector.ws.command.CommandHandler;
import io.gravitee.ae.connector.ws.configuration.ConnectorConfiguration;
import io.gravitee.ae.connector.ws.configuration.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeDiscoveryCommandHandler implements CommandHandler<NodeDiscoveryCommand> {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(NodeDiscoveryCommandHandler.class);

    private static final String DISCOVERY_NODE_REMOVED = "REMOVE";
    private static final String DISCOVERY_NODE_CHANGED = "CHANGE";

    @Autowired
    private ConnectorConfiguration connectorConfiguration;

    @Value("${alerts.alert-engine.ws.discovery:true}")
    private boolean discovery;

    @Override
    public Command.Type getSupportedType() {
        return Command.Type.NODE_DISCOVERY;
    }

    @Override
    public void handle(NodeDiscoveryCommand command, Handler resultHandler) {
        DynamicEndpoint websocketEndpoint = new DynamicEndpoint(command.getMember(), command.getEndpoint());

        if (discovery) {
            Engine defaultEngine = connectorConfiguration.getDefaultEngine();
            if (DISCOVERY_NODE_CHANGED.equalsIgnoreCase(command.getAction())) {
                // Endpoint must be registered only if target does not already exist

                if (defaultEngine.getEndpoints().stream().noneMatch(edpt -> edpt.getUrl().equals(websocketEndpoint.getUrl()))) {
                    logger.info("An alert engine instance has been discovered at {}", websocketEndpoint.getUrl());
                    defaultEngine.getEndpoints().add(websocketEndpoint);
                }
            } else if (DISCOVERY_NODE_REMOVED.equalsIgnoreCase(command.getAction())) {
                logger.info("An alert engine instance has been removed from {}", websocketEndpoint.getUrl());
                defaultEngine.getEndpoints().remove(websocketEndpoint);
            }
        }

        resultHandler.handle(null);
    }
}
