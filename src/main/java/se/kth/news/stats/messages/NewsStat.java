package se.kth.news.stats.messages;

import java.util.UUID;

/**
 * Created by Love on 2016-04-27.
 */
public class NewsStat {

    private UUID[] ids;

    public NewsStat(UUID[] ids) {
        this.ids = ids;
    }

    public UUID[] getIds() {
        return ids;
    }
}
