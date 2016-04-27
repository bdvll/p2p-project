package se.kth.news.core.news.messages;

import com.sun.org.apache.bcel.internal.generic.NEW;

import java.util.UUID;

/**
 * Created by Love on 2016-04-27.
 */
public class NewsItem {

    private UUID id;
    private int ttl;

    public NewsItem(int ttl) {
        this.id = UUID.randomUUID();
        this.ttl = ttl;
    }

    public UUID getId() {
        return id;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public void decreaseTTL(){
        this.ttl--;
    }

    public boolean shouldFlood(){
        return this.ttl > 0;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof NewsItem){
            NewsItem item = (NewsItem) obj;
            return item.id.equals(id);
        }
        return false;
    }

}
