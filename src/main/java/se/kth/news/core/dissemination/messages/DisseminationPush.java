package se.kth.news.core.dissemination.messages;

import se.kth.news.core.news.messages.NewsItem;
import se.sics.ktoolbox.util.network.KAddress;

import java.util.ArrayList;

/**
 * Created by Love on 2016-05-20.
 */
public class DisseminationPush {

    private KAddress leader;
    private NewsItem[] unseenNews;

    public DisseminationPush(KAddress leader, NewsItem[] unseenNews) {
        this.leader = leader;
        this.unseenNews = unseenNews;
    }

    public KAddress getLeader() {
        return leader;
    }

    public NewsItem[] getUnseenNews() {
        return unseenNews;
    }
}
