package se.kth.news.core.paxos.events;

import se.kth.news.core.news.messages.NewsItem;
import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Created by Love on 2016-05-18.
 */
public class PaxosNewsAnnounce implements KompicsEvent {

    private NewsItem newsItem;

    public PaxosNewsAnnounce(NewsItem newsItem) {
        this.newsItem = newsItem;
    }

    public NewsItem getNewsItem() {
        return newsItem;
    }
}
