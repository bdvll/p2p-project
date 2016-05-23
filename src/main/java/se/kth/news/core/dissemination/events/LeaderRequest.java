package se.kth.news.core.dissemination.events;

import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Created by Love on 2016-05-20.
 */
public class LeaderRequest implements KompicsEvent {

    private KAddress requester;
    private String bitString;

    public LeaderRequest(String bitString, KAddress requester) {
        this.bitString = bitString;
        this.requester = requester;
    }

    public KAddress getRequester() {
        return requester;
    }

    public String getBitString() {
        return bitString;
    }
}
