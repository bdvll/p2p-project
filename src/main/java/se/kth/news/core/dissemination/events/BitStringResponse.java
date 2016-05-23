package se.kth.news.core.dissemination.events;

import se.sics.kompics.KompicsEvent;

/**
 * Created by Love on 2016-05-23.
 */
public class BitStringResponse implements KompicsEvent {

    private String bitString;

    public BitStringResponse(String bitString) {
        this.bitString = bitString;
    }

    public String getBitString() {
        return bitString;
    }
}
