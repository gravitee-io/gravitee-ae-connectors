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
package io.gravitee.ae.connector.ws.command.notification;

import io.gravitee.ae.connector.api.command.AlertNotificationCommand;
import io.gravitee.ae.connector.api.command.Command;
import io.gravitee.ae.connector.api.command.Handler;
import io.gravitee.ae.connector.ws.command.CommandHandler;
import io.gravitee.ae.connector.ws.listener.ListenerManager;
import io.gravitee.alert.api.trigger.TriggerProvider;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertNotificationCommandHandler implements CommandHandler<AlertNotificationCommand> {

    @Autowired
    private ListenerManager listenerManager;

    @Override
    public Command.Type getSupportedType() {
        return Command.Type.ALERT_NOTIFICATION;
    }

    @Override
    public void handle(AlertNotificationCommand command, Handler resultHandler) {
        listenerManager
            .getListeners(TriggerProvider.OnCommandListener.class)
            .forEach(
                new Consumer<TriggerProvider.OnCommandListener>() {
                    @Override
                    public void accept(TriggerProvider.OnCommandListener listener) {
                        io.gravitee.alert.api.trigger.command.AlertNotificationCommand alert =
                            new io.gravitee.alert.api.trigger.command.AlertNotificationCommand(
                                command.getTrigger(),
                                command.getTimestamp()
                            );

                        alert.setMessage(command.getMessage());

                        listener.doOnCommand(alert);
                        resultHandler.handle(null);
                    }
                }
            );
    }
}
