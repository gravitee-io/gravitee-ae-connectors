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
package io.gravitee.ae.connector.core;

import io.gravitee.alert.api.event.Context;
import io.gravitee.node.api.Node;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ContextBuilder {

    private final Node node;

    public ContextBuilder(Node node) {
        this.node = node;
    }

    public Context build() {
        final Context context = new Context();

        // Try to get information from Node.
        context.put(Context.CONTEXT_NODE_ID, node.id());
        context.put(Context.CONTEXT_NODE_APPLICATION, node.application());
        context.put(Context.CONTEXT_NODE_HOSTNAME, node.hostname());
        context.put(Context.CONTEXT_INSTALLATION, (String) node.metadata().get(Node.META_INSTALLATION));

        return context;
    }
}
