package se.kth.news.core.dissemination.events;

import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Created by Love on 2016-05-20.
 */
public class LeaderResponse implements KompicsEvent {

    private KAddress leader;
    private KAddress requester;
    private String bitString;

    public LeaderResponse(KAddress leader, KAddress requester, String bitString) {
        this.leader = leader;
        this.requester = requester;
        this.bitString = bitString;
    }

    public KAddress getLeader() {
        return leader;
    }

    public KAddress getRequester() {
        return requester;
    }

    public String getBitString() {
        return bitString;
    }

}
