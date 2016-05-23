package se.kth.news.core.dissemination.ports;

import se.kth.news.core.dissemination.events.LeaderRequest;
import se.kth.news.core.dissemination.events.LeaderResponse;
import se.kth.news.core.dissemination.events.LeaderUpdate;
import se.sics.kompics.PortType;

/**
 * Created by Love on 2016-05-20.
 */
public class LeaderDisseminationPort extends PortType{

    {
        indication(LeaderRequest.class);
        indication(LeaderUpdate.class);

        request(LeaderResponse.class);
    }

}
