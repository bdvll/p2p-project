package se.kth.news.core.news.messages;

import se.kth.news.core.news.util.ClickBaiter;

/**
 * Created by Love on 2016-04-27.
 */
public class NewsItem {

    private long id;
    private String newsContent;

    public NewsItem(){
        this.id = -1;
        this.newsContent = generateContent();
    }

    public NewsItem(long id) {
        this.id = id;
        this.newsContent = generateContent();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNewsContent() {
        return newsContent;
    }

    @Override
    public int hashCode() {
        return (int) id;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof NewsItem){
            NewsItem item = (NewsItem) obj;
            return item.id == id;
        }
        return false;
    }

    private String generateContent(){
        return ClickBaiter.getBait();
    }

}
