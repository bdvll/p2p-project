package se.kth.news.stats.messages;

import java.util.UUID;

/**
 * Created by Love on 2016-04-27.
 */
public class NewsStat {

    private String bitString;

    public NewsStat(String bitString) {
        this.bitString = bitString;
    }

    public String getBitString() {
        return bitString;
    }
}
