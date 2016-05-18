package se.kth.news.core.paxos;

import se.kth.news.core.news.messages.NewsItem;
import se.sics.kompics.KompicsEvent;

/**
 * Created by Love on 2016-05-18.
 */
public class PaxosNewsResponse implements KompicsEvent {

    private NewsItem newsItem;
    private boolean success;

    public PaxosNewsResponse(NewsItem newsItem, boolean success) {
        this.newsItem = newsItem;
        this.success = success;
    }

    public NewsItem getNewsItem() {
        return newsItem;
    }

    public boolean isSuccess() {
        return success;
    }
}
