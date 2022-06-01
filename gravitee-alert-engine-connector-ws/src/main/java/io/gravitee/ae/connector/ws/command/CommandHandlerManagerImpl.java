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
package io.gravitee.ae.connector.ws.command;

import io.gravitee.ae.connector.api.command.Command;
import io.gravitee.ae.connector.api.command.Handler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CommandHandlerManagerImpl implements CommandHandlerManager {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(CommandHandlerManagerImpl.class);

    @Autowired
    private List<CommandHandler<? extends Command>> commandHandlers;

    private final Map<Command.Type, CommandHandler> handlers = new HashMap<>();

    @PostConstruct
    private void init() {
        this.commandHandlers.forEach(command -> this.handlers.put(command.getSupportedType(), command));
    }

    @Override
    public void handle(Command command, Handler resultHandler) {
        final CommandHandler handler = this.handlers.get(command.type());

        if (handler == null) {
            logger.error("No handler found to handle command of type {}", command.type());
        } else {
            handler.handle(command, resultHandler);
        }
    }
}
