package se.kth.news.core.paxos;

import se.kth.news.core.news.util.NewsView;
import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.other.Container;

/**
 * Created by Love on 2016-05-18.
 */
public class PaxosLeaderInternalResponse implements KompicsEvent {

    private Container<KAddress, NewsView> suggestedLeader;
    private boolean possibleLeader;
    private int msgId;

    public PaxosLeaderInternalResponse(Container<KAddress, NewsView> suggestedLeader, boolean possibleLeader, int msgId) {
        this.suggestedLeader = suggestedLeader;
        this.possibleLeader = possibleLeader;
        this.msgId = msgId;
    }

    public Container<KAddress, NewsView> getSuggestedLeader() {
        return suggestedLeader;
    }

    public boolean isPossibleLeader() {
        return possibleLeader;
    }

    public int getMsgId() {
        return msgId;
    }
}
