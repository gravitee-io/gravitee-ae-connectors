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
package io.gravitee.ae.connector.api.command;

import com.fasterxml.jackson.annotation.*;
import java.io.Serializable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = NodeDiscoveryCommand.class, name = "NODE_DISCOVERY"),
        @JsonSubTypes.Type(value = NodeDiscoveryCommand.class, name = "node_discovery"),
        @JsonSubTypes.Type(value = AlertNotificationCommand.class, name = "ALERT_NOTIFICATION"),
        @JsonSubTypes.Type(value = AlertNotificationCommand.class, name = "alert_notification"),
        @JsonSubTypes.Type(value = ResolvePropertyCommand.class, name = "PROPERTY_RESOLVER"),
        @JsonSubTypes.Type(value = ResolvePropertyCommand.class, name = "property_resolver"),
    }
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface Command extends Serializable {
    enum Type {
        NODE_DISCOVERY,
        ALERT_NOTIFICATION,
        PROPERTY_RESOLVER,
    }

    @JsonProperty
    String id();

    @JsonProperty
    Type type();
}
