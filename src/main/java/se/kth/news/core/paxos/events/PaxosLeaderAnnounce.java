package se.kth.news.core.paxos.events;

import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Created by Love on 2016-05-18.
 */
public class PaxosLeaderAnnounce implements KompicsEvent {

    private KAddress leader;

    public PaxosLeaderAnnounce(KAddress leader) {
        this.leader = leader;
    }

    public KAddress getLeader() {
        return leader;
    }
}
