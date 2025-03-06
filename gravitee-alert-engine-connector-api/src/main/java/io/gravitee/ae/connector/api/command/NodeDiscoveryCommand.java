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
package io.gravitee.ae.connector.api.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeDiscoveryCommand extends AbstractCommand {

    private final String action;

    private final String member;

    private final String endpoint;

    @JsonCreator
    public NodeDiscoveryCommand(
        @JsonProperty(value = "action", required = true) String action,
        @JsonProperty(value = "member", required = true) String member,
        @JsonProperty(value = "endpoint", required = true) String endpoint
    ) {
        this.action = action;
        this.member = member;
        this.endpoint = endpoint;
    }

    @Override
    public Type type() {
        return Type.NODE_DISCOVERY;
    }

    public String getAction() {
        return action;
    }

    public String getMember() {
        return member;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
