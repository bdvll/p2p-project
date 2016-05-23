package se.kth.news.core.dissemination.events;

import se.kth.news.core.news.messages.NewsItem;
import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Created by Love on 2016-05-20.
 */
public class NewsResponse implements KompicsEvent {

    private KAddress requester;
    private KAddress leader;
    private NewsItem[] missingNews;

    public NewsResponse(KAddress requester, KAddress leader, NewsItem[] missingNews) {
        this.requester = requester;
        this.leader = leader;
        this.missingNews = missingNews;
    }

    public KAddress getRequester() {
        return requester;
    }

    public KAddress getLeader() {
        return leader;
    }

    public NewsItem[] getMissingNews() {
        return missingNews;
    }
}
