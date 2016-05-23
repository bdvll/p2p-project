package se.kth.news.core.dissemination.ports;

import se.kth.news.core.dissemination.events.*;
import se.sics.kompics.PortType;

/**
 * Created by Love on 2016-05-20.
 */
public class NewsDisseminationPort extends PortType {

    {
        indication(NewsRequest.class);
        indication(NewsUpdate.class);
        indication(BitStringRequest.class);

        request(NewsResponse.class);
        request(BitStringResponse.class);
    }

}
