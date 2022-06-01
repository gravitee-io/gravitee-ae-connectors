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
package io.gravitee.ae.connector.ws.listener;

import io.gravitee.alert.api.Listener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ListenerManagerImpl implements ListenerManager {

    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public <T extends Listener> List<T> getListeners(Class<T> listenerClass) {
        return listeners
            .stream()
            .filter(listener -> listenerClass.isAssignableFrom(listener.getClass()))
            .map(listener -> (T) listener)
            .collect(Collectors.toList());
    }
}
