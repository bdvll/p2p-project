package se.kth.news.core.dissemination.events;

import se.kth.news.core.news.messages.NewsItem;
import se.sics.kompics.KompicsEvent;

/**
 * Created by Love on 2016-05-20.
 */
public class NewsUpdate implements KompicsEvent {

    private NewsItem[] news;

    public NewsUpdate(NewsItem[] news) {
        this.news = news;
    }

    public NewsItem[] getNews() {
        return news;
    }
}
