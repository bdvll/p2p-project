package se.kth.news.core.paxos;

import se.sics.kompics.PortType;

/**
 * Created by Love on 2016-05-18.
 */
public class PaxosNewsPort extends PortType {

    {
        indication(PaxosNewsPrepare.class);
        request(PaxosLeaderResponse.class);
    }

}
