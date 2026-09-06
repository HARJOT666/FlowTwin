package com.flowtwin.ws;

import com.flowtwin.twin.TwinState;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class TwinBroadcaster {

    private final SimpMessagingTemplate messaging;

    public TwinBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void broadcastState(TwinState state) {
        messaging.convertAndSend("/topic/twin", state);
    }

    public void broadcastInsight(Object insight) {
        messaging.convertAndSend("/topic/twin/insight", insight);
    }
}
