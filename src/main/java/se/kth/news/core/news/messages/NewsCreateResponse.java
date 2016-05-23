package se.kth.news.core.news.messages;

import se.sics.ktoolbox.util.network.KAddress;

/**
 * Created by Love on 2016-05-23.
 */
public class NewsCreateResponse {

    private KAddress leader;
    private boolean success;
    private int messageId;

    public NewsCreateResponse(KAddress leader, boolean success, int messageId) {
        this.leader = leader;
        this.success = success;
        this.messageId = messageId;
    }

    public KAddress getLeader() {
        return leader;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getMessageId() {
        return messageId;
    }
}
