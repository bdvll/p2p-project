package se.kth.news.core.paxos.ports;

import se.kth.news.core.paxos.events.PaxosLeaderResponse;
import se.kth.news.core.paxos.events.PaxosNewsAnnounce;
import se.kth.news.core.paxos.events.PaxosNewsPrepare;
import se.sics.kompics.PortType;

/**
 * Created by Love on 2016-05-18.
 */
public class PaxosNewsPort extends PortType {

    {
        indication(PaxosNewsPrepare.class);
        request(PaxosNewsAnnounce.class);
    }

}
