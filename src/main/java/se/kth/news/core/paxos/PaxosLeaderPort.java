package se.kth.news.core.paxos;

import se.sics.kompics.PortType;

/**
 * Created by Love on 2016-05-17.
 */
public class PaxosLeaderPort extends PortType {
    {
        indication(PaxosLeaderStart.class);
        request(PaxosLeaderResponse.class);
        request(PaxosLeaderInternalCheck.class);
        indication(PaxosLeaderInternalResponse.class);
        request(PaxosLeaderAnnounce.class);
    }
}
