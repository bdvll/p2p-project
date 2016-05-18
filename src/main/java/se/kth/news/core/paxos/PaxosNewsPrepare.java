package se.kth.news.core.paxos;

import se.kth.news.core.news.messages.NewsItem;
import se.sics.kompics.KompicsEvent;

/**
 * Created by Love on 2016-05-18.
 */
public class PaxosNewsPrepare implements KompicsEvent {

    private NewsItem newsItem;

    public PaxosNewsPrepare(NewsItem newsItem) {
        this.newsItem = newsItem;
    }

    public NewsItem getNewsItem() {
        return newsItem;
    }
}
