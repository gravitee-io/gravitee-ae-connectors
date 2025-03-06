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
package io.gravitee.ae.connector.ws.command.resolver;

import io.gravitee.ae.connector.api.command.Command;
import io.gravitee.ae.connector.api.command.Handler;
import io.gravitee.ae.connector.api.command.ResolvePropertyCommand;
import io.gravitee.ae.connector.ws.command.CommandHandler;
import io.gravitee.ae.connector.ws.listener.ListenerManager;
import io.gravitee.alert.api.trigger.TriggerProvider;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResolvePropertyCommandHandler implements CommandHandler<ResolvePropertyCommand> {

    @Autowired
    private ListenerManager listenerManager;

    @Override
    public Command.Type getSupportedType() {
        return Command.Type.PROPERTY_RESOLVER;
    }

    @Override
    public void handle(ResolvePropertyCommand command, Handler resultHandler) {
        Optional<TriggerProvider.OnCommandResultListener> optListener = listenerManager
            .getListeners(TriggerProvider.OnCommandResultListener.class)
            .stream()
            .findFirst();

        optListener.ifPresent(listener ->
            listener.doOnCommand(
                new io.gravitee.alert.api.trigger.command.ResolvePropertyCommand(command.getProperties()),
                result -> {
                    resultHandler.handle(result);
                }
            )
        );
    }
}
