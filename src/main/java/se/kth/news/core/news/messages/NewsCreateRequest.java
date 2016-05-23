package se.kth.news.core.news.messages;

import se.sics.ktoolbox.util.network.KAddress;

/**
 * Created by Love on 2016-05-23.
 */
public class NewsCreateRequest {

    private NewsItem newsItem;
    private int messageId;

    public NewsCreateRequest(NewsItem newsItem, int messageId) {
        this.newsItem = newsItem;
        this.messageId = messageId;
    }

    public NewsItem getNewsItem() {
        return newsItem;
    }

    public int getMessageId() {
        return messageId;
    }


    @Override
    public boolean equals(Object obj) {
        if(obj instanceof NewsCreateRequest){
            NewsCreateRequest req = (NewsCreateRequest) obj;
            return messageId == req.messageId;
        }
        return false;
    }
}
