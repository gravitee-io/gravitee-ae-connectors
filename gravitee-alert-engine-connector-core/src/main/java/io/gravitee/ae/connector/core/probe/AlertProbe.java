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
package io.gravitee.ae.connector.core.probe;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AlertProbe implements Probe {

    private AtomicLong droppedEvents = new AtomicLong(0);

    private boolean healthy = true;

    private boolean ready = false;

    private Throwable lastError;

    @Override
    public String id() {
        return "alert-engine";
    }

    @Override
    public boolean isVisibleByDefault() {
        return false;
    }

    @Override
    public CompletionStage<Result> check() {
        Result result;

        if (!ready) {
            result = Result.notReady();
        } else {
            if (healthy) {
                result = Result.healthy("Ok (total dropped events since startup [%s])", droppedEvents);
            } else {
                if (lastError != null) {
                    result = Result.unhealthy("%s (total dropped events since startup [%s])", lastError.getMessage(), droppedEvents);
                } else {
                    result = Result.unhealthy("Error (total dropped events since startup [%s])", droppedEvents);
                }
            }
        }

        return CompletableFuture.completedFuture(result);
    }

    public void addDroppedEvents(int count) {
        this.droppedEvents.addAndGet(count);
    }

    public void setReady() {
        this.ready = true;
    }

    public void setHealthy() {
        this.healthy = true;
    }

    public void setUnhealthy() {
        this.healthy = false;
    }

    public void setLastError(Throwable throwable) {
        this.lastError = throwable;
    }

    public void setUnhealthy(int count, Throwable throwable) {
        this.addDroppedEvents(count);
        this.setLastError(throwable);
        this.setUnhealthy();
    }
}
