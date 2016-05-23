package se.kth.news.core.dissemination.messages;

/**
 * Created by Love on 2016-05-20.
 */
public class DisseminationPull {

    private String bitString;

    public DisseminationPull(String bitString) {
        this.bitString = bitString;
    }

    public String getBitString() {
        return bitString;
    }
}
