package se.kth.news.core.paxos.events;

import se.kth.news.core.news.messages.NewsItem;
import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;

import java.util.ArrayList;

/**
 * Created by Love on 2016-05-18.
 */
public class PaxosNewsPrepare implements KompicsEvent {

    private NewsItem newsItem;
    private ArrayList<KAddress> quorum;

    public PaxosNewsPrepare(NewsItem newsItem, ArrayList<KAddress> quorum) {
        this.newsItem = newsItem;
        this.quorum = quorum;
    }

    public NewsItem getNewsItem() {
        return newsItem;
    }

    public ArrayList<KAddress> getQuorum() {
        return quorum;
    }
}
