package se.kth.news.core.paxos;

import se.sics.kompics.KompicsEvent;

/**
 * Created by Love on 2016-05-17.
 */
public class PaxosLeaderResponse implements KompicsEvent {

    private boolean accepted;

    public PaxosLeaderResponse(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }
}
