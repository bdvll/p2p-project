package se.kth.news.core.dissemination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.news.core.dissemination.events.*;
import se.kth.news.core.dissemination.messages.DisseminationPull;
import se.kth.news.core.dissemination.messages.DisseminationPush;
import se.kth.news.core.dissemination.ports.LeaderDisseminationPort;
import se.kth.news.core.dissemination.ports.NewsDisseminationPort;
import se.kth.news.core.news.messages.NewsItem;
import se.kth.news.core.news.util.NewsView;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.other.Container;

import java.util.List;


/**
 * Created by Love on 2016-05-20.
 */
public class DisseminationComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(DisseminationComp.class);
    private String logPrefix;

    //*******************************CONNECTIONS********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    Negative<LeaderDisseminationPort> leaderPort = provides(LeaderDisseminationPort.class);
    Negative<NewsDisseminationPort> newsPort = provides(NewsDisseminationPort.class);
    Positive<GradientPort> gradientPort = requires(GradientPort.class);

    //*******************************EXTERNAL_STATE*****************************
    private KAddress selfAdr;
    //*******************************INTERNAL_STATE*****************************
    private List<Container<KAddress, NewsView>> gradientNeighbours;
    private int gradientRobin;


    public DisseminationComp(Init init){
        selfAdr = init.selfAdr;

        logPrefix = "<nid:" + selfAdr.getId() + ">";

        subscribe(handleStart, control);
        subscribe(handleGradientSample, gradientPort);
        subscribe(pullTimeoutHandler, timerPort);

        subscribe(handlePull, networkPort);
        subscribe(handlePush, networkPort);

        subscribe(leaderResponseHandler, leaderPort);
        subscribe(newsResponseHandler, newsPort);
        subscribe(bitStringResponseHandler, newsPort);

    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            startTimers();
        }
    };

    private void startTimers(){
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(10000, 5000);
        PullTimeout pullTimeout = new PullTimeout(spt);
        spt.setTimeoutEvent(pullTimeout);
        trigger(spt, timerPort);

    }

    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {
            gradientNeighbours = sample.getGradientNeighbours();
        }
    };

    //--------------------------- Initiator -----------------------------------

    Handler<BitStringResponse> bitStringResponseHandler = new Handler<BitStringResponse>() {
        @Override
        public void handle(BitStringResponse bitStringResponse) {
            KAddress neighbor = chooseNeighbor();
            if(neighbor != null) {
                KHeader header = new BasicHeader(selfAdr, neighbor, Transport.UDP);
                KContentMsg msg = new BasicContentMsg(header, new DisseminationPull(bitStringResponse.getBitString()));
                trigger(msg, networkPort);
            }
        }
    };

    ClassMatchedHandler handlePush
            = new ClassMatchedHandler<DisseminationPush, KContentMsg<KAddress, ?, DisseminationPush>>() {

        @Override
        public void handle(DisseminationPush push, KContentMsg<KAddress, ?, DisseminationPush> container) {
            trigger(new LeaderUpdate(push.getLeader()), leaderPort);
            NewsItem[] unseenItems = push.getUnseenNews();
            if(unseenItems.length > 0)
                trigger(new NewsUpdate(unseenItems), newsPort);
        }
    };

    Handler<PullTimeout> pullTimeoutHandler = new Handler<PullTimeout>() {
        @Override
        public void handle(PullTimeout pullTimeout) {
            trigger(new BitStringRequest(), newsPort);
        }
    };

    private KAddress chooseNeighbor(){
        if(gradientNeighbours == null || gradientNeighbours.size() == 0)
            return null;
        Container<KAddress, ?> neighbour = gradientNeighbours.get(gradientRobin++%gradientNeighbours.size());
        gradientRobin %= gradientNeighbours.size();
        return neighbour.getSource();
    }


    // -------------------------- Responder -------------------------------

    Handler<LeaderResponse> leaderResponseHandler = new Handler<LeaderResponse>() {
        @Override
        public void handle(LeaderResponse leaderResponse) {
            NewsRequest request = new NewsRequest(leaderResponse.getRequester(), leaderResponse.getLeader(), leaderResponse.getBitString());
            trigger(request, newsPort);
        }
    };

    Handler<NewsResponse> newsResponseHandler = new Handler<NewsResponse>() {
        @Override
        public void handle(NewsResponse response) {
            KHeader header = new BasicHeader(selfAdr, response.getRequester(), Transport.UDP);
            KContentMsg msg = new BasicContentMsg(header, new DisseminationPush(response.getLeader(), response.getMissingNews()));
            trigger(msg, networkPort);
        }
    };

    ClassMatchedHandler handlePull
            = new ClassMatchedHandler<DisseminationPull, KContentMsg<KAddress, ?, DisseminationPull>>() {

        @Override
        public void handle(DisseminationPull pull, KContentMsg<KAddress, ?, DisseminationPull> container) {
            trigger(new LeaderRequest(pull.getBitString(), container.getHeader().getSource()), leaderPort);
        }
    };

    private static class PullTimeout extends Timeout {

        public PullTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }

    public static class Init extends se.sics.kompics.Init<DisseminationComp> {

        public final KAddress selfAdr;

        public Init(KAddress selfAdr) {
            this.selfAdr = selfAdr;
        }
    }
}
