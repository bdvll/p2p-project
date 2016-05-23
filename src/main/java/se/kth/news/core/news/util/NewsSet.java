package se.kth.news.core.news.util;

import se.kth.news.core.news.messages.NewsItem;
import se.kth.news.util.PaddedCharArrayList;

import java.util.*;

/**
 * Created by Love on 2016-04-27.
 */
public class NewsSet {

    private HashMap<Long, NewsItem> seenNews;
    private PaddedCharArrayList bitString = new PaddedCharArrayList('0');

    public NewsSet(){
        this.seenNews = new HashMap<>();
    }

    public void add(NewsItem item){
        seenNews.put(item.getId(), item);
        bitString.add((int) item.getId(), '1');
    }

    public boolean hasSeen(NewsItem item){
        return bitString.get((int) item.getId()) == '1';
    }

    public NewsItem getItem(long index){
        return seenNews.get(index);
    }

    public int getNewsCount(){
        return seenNews.size();
    }

    public String getBitString(){
        return bitString.toString();
    }

    public int getNextId(){
        for(int i = bitString.size() -1; i >= 0; --i){
            if(bitString.get(i) == '1')
                return i + 1;
        }
        return 0;
    }

    public NewsItem[] getUnseenNews(String bitString){
        ArrayList<NewsItem> items = new ArrayList<>();

        // add news that requester is missing
        for(int i = 0; i < bitString.length(); ++i){
            if(bitString.charAt(i) == '0'){
                if(this.bitString.get(i) == '1'){
                    items.add(seenNews.get(i));
                }
            }
        }

        // add remaining news that requester hasn't seen
        for(int i = bitString.length(); i < this.bitString.size(); ++i){
            if(this.bitString.get(i) == '1'){
                items.add(seenNews.get(i));
            }
        }

        return (NewsItem[]) items.toArray();
    }

}
