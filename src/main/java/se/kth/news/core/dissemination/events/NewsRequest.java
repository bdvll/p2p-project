package se.kth.news.core.dissemination.events;

import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Created by Love on 2016-05-20.
 */
public class NewsRequest implements KompicsEvent{

    private KAddress requester;
    private KAddress leader;
    private String bitString;

    public NewsRequest(KAddress requester, KAddress leader, String bitString) {
        this.requester = requester;
        this.leader = leader;
        this.bitString = bitString;
    }

    public KAddress getRequester() {
        return requester;
    }

    public KAddress getLeader() {
        return leader;
    }

    public String getBitString() {
        return bitString;
    }
}
