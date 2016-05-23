package se.kth.news.core.dissemination.events;

import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Created by Love on 2016-05-20.
 */
public class LeaderUpdate implements KompicsEvent {

    private KAddress leader;

    public LeaderUpdate(KAddress leader) {
        this.leader = leader;
    }

    public KAddress getLeader() {
        return leader;
    }
}
