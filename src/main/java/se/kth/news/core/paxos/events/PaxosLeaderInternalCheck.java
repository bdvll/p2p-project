package se.kth.news.core.paxos.events;

import se.kth.news.core.news.util.NewsView;
import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.other.Container;

/**
 * Created by Love on 2016-05-18.
 */
public class PaxosLeaderInternalCheck implements KompicsEvent {

    private Container<KAddress, NewsView> suggestedLeader;
    private int msgId;

    public PaxosLeaderInternalCheck(Container<KAddress, NewsView> suggestedLeader, int msgId) {
        this.suggestedLeader = suggestedLeader;
        this.msgId = msgId;
    }

    public Container<KAddress, NewsView> getSuggestedLeader() {
        return suggestedLeader;
    }

    public int getMsgId() {
        return msgId;
    }
}
