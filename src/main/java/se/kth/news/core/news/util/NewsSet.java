package se.kth.news.core.news.util;

import se.kth.news.core.news.messages.NewsItem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

/**
 * Created by Love on 2016-04-27.
 */
public class NewsSet {

    private HashSet<UUID> seenNews;

    public NewsSet(){
        this.seenNews = new HashSet<>();
    }

    public void add(NewsItem item){
        seenNews.add(item.getId());
    }

    public boolean hasSeen(NewsItem item){
        return seenNews.contains(item.getId());
    }

    public int getNewsCount(){
        return seenNews.size();
    }

    public UUID[] getIds(){
        UUID[] ids = new UUID[seenNews.size()];
        return seenNews.toArray(ids);
    }

}
