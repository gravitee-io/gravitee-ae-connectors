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
package io.gravitee.ae.connector.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.ae.connector.core.ContextBuilder;
import io.gravitee.ae.connector.core.probe.AlertProbe;
import io.gravitee.ae.connector.ws.configuration.ConnectorConfiguration;
import io.gravitee.ae.connector.ws.configuration.Engine;
import io.gravitee.ae.connector.ws.listener.ListenerManager;
import io.gravitee.alert.api.event.*;
import io.gravitee.node.api.healthcheck.ProbeManager;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.impl.ConcurrentHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WsEventProducer extends AbstractEventProducer implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(WsEventProducer.class);

    private static final int MAX_PENDING_EVENTS = 1000;
    public static final String WS_EVENT_PATH = "/ws/events";
    public static final String HTTP_EVENT_PATH = "/http/events";

    private final ObjectMapper mapper = new ObjectMapper();

    private AbstractConnector<?> connector;

    private ApplicationContext applicationContext;

    @Autowired
    private ListenerManager listenerManager;

    @Autowired
    private ContextBuilder contextBuilder;

    @Autowired
    private ConnectorConfiguration configuration;

    @Autowired
    ProbeManager probeManager;

    @Autowired
    private AlertProbe alertProbe;

    private Context context;

    // Keep a copy of events while waiting for producer being started
    private final Set<Event> pendingEvents = new ConcurrentHashSet<>();

    private FlowableEmitter<Event> eventEmitter;

    @Override
    public void send(Event event) {
        if (eventEmitter == null) {
            if (pendingEvents.size() < MAX_PENDING_EVENTS) {
                pendingEvents.add(event);
            } else {
                // Track dropped events count.
                alertProbe.addDroppedEvents(1);
            }
        } else {
            eventEmitter.onNext(event);
        }
    }

    @Override
    protected void doStart() throws Exception {
        context = contextBuilder.build();

        final Flowable<Event> bufferedEvents = Flowable
            .create(this::prepareEventEmitter, BackpressureStrategy.LATEST)
            .doOnNext(this::copyContextToEvent);

        logger.info("AlertEngine connector is enabled. Starting connector.");
        Engine defaultEngine = configuration.getDefaultEngine(); // event producer only send the events to the default engine

        if (configuration.isSendEventsOnHttp()) {
            connector = new HttpConnector(defaultEngine, HTTP_EVENT_PATH);
            applicationContext.getAutowireCapableBeanFactory().autowireBean(connector);
            connector.start();

            // Events are buffered to be sent by packets.
            bufferedEvents
                .buffer(configuration.getBulkEventsWait(), TimeUnit.MILLISECONDS, configuration.getBulkEventsSize())
                .doOnNext(this::writeEvents)
                .onBackpressureDrop(events -> alertProbe.setUnhealthy(events.size(), new RuntimeException("AE is overloaded.")))
                .observeOn(Schedulers.computation())
                .subscribe(events -> {}, throwable -> {});
        } else {
            connector = new WebSocketConnector(defaultEngine, WS_EVENT_PATH);
            applicationContext.getAutowireCapableBeanFactory().autowireBean(connector);

            // Listen on connection only makes sense for WebSocket. For now, events are sent 1 by 1 with websocket to keep it backward compatible.
            listenerManager.addListener(
                (EventProducer.OnConnectionListener) () ->
                    bufferedEvents
                        .doOnNext(this::writeEvent)
                        .onBackpressureDrop(event -> alertProbe.setUnhealthy(1, new RuntimeException("AE is overloaded.")))
                        .observeOn(Schedulers.computation())
                        .subscribe(events -> {}, throwable -> {})
            );
            connector.start();
        }

        alertProbe.setReady();
    }

    private void prepareEventEmitter(FlowableEmitter<Event> emitter) {
        this.eventEmitter = emitter;
        // Now we have internally subscribed, flush the pending events.
        pendingEvents.forEach(event -> eventEmitter.onNext(event));
        pendingEvents.clear();
    }

    private void copyContextToEvent(Event event) {
        // Copy the property from context to event if not already exists.
        if (context != null) {
            if (event.properties() == null && event instanceof DefaultEvent) {
                ((DefaultEvent) event).setProperties(new HashMap<>());
            }

            if (event.properties() != null) {
                context.forEach((k, v) -> event.properties().putIfAbsent(k, v));
            }
        }
    }

    private void writeEvents(List<Event> eventList) {
        if (!eventList.isEmpty()) {
            try {
                connector
                    .writeTextMessage(mapper.writeValueAsString(eventList))
                    .onSuccess(aVoid -> alertProbe.setHealthy())
                    .onFailure(throwable -> {
                        if (isCurrentlyHealthy()) {
                            logger.warn("An error occurred trying to send events: {}", throwable.getMessage());
                        }
                        alertProbe.setUnhealthy(eventList.size(), throwable);
                    });
            } catch (Exception e) {
                if (isCurrentlyHealthy()) {
                    logger.error("Unexpected error while writing event", e);
                }
                alertProbe.setUnhealthy(eventList.size(), e);
            }
        }
    }

    private boolean isCurrentlyHealthy() {
        try {
            return alertProbe.check().toCompletableFuture().get().isHealthy();
        } catch (Exception e) {
            return false;
        }
    }

    private void writeEvent(Event event) {
        if (event != null) {
            try {
                connector
                    .writeTextMessage(mapper.writeValueAsString(event))
                    .onSuccess(aVoid -> alertProbe.setHealthy())
                    .onFailure(throwable -> {
                        if (isCurrentlyHealthy()) {
                            logger.warn("An error occurred trying to send event: {}", throwable.getMessage());
                        }
                        alertProbe.setUnhealthy(1, throwable);
                    });
            } catch (Exception e) {
                if (isCurrentlyHealthy()) {
                    logger.error("Unexpected error while writing event", e);
                }
                alertProbe.setUnhealthy(1, e);
            }
        }
    }

    @Override
    protected void doStop() throws Exception {
        TimeUnit.MILLISECONDS.sleep(configuration.getBulkEventsWait());
        connector.stop();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
