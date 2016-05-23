package se.kth.news.core.paxos.events;

import se.kth.news.core.news.util.NewsView;
import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.other.Container;

import java.util.ArrayList;

/**
 * Created by Love on 2016-05-17.
 */
public class PaxosLeaderStart implements KompicsEvent {

    private int ballot;
    private ArrayList<KAddress> quorum;
    private Container<KAddress,NewsView> suggestedLeader;

    public PaxosLeaderStart(int ballot, ArrayList<KAddress> quorum, Container<KAddress,NewsView> suggestedLeader) {
        this.ballot = ballot;
        this.quorum = quorum;
        this.suggestedLeader = suggestedLeader;
    }

    public int getBallot() {
        return ballot;
    }

    public ArrayList<KAddress> getQuorum() {
        return quorum;
    }

    public Container<KAddress, NewsView> getSuggestedLeader() {
        return suggestedLeader;
    }
}
