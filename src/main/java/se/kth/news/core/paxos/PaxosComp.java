package se.kth.news.core.paxos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.news.core.news.messages.NewsItem;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.core.paxos.events.*;
import se.kth.news.core.paxos.messages.Accept;
import se.kth.news.core.paxos.messages.Prepare;
import se.kth.news.core.paxos.messages.Promise;
import se.kth.news.core.paxos.ports.PaxosLeaderPort;
import se.kth.news.core.paxos.ports.PaxosNewsPort;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.other.Container;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Love on 2016-05-17.
 */
public class PaxosComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(PaxosComp.class);
    private String logPrefix;

    //*******************************CONNECTIONS********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<PaxosLeaderPort> paxosLeaderPort = requires(PaxosLeaderPort.class);
    Positive<PaxosNewsPort> paxosNewsPort = requires(PaxosNewsPort.class);

    //*******************************EXTERNAL_STATE*****************************
    private KAddress selfAdr;
    //*******************************INTERNAL_STATE*****************************
    private int quorumSize;
    private ArrayList<KAddress> quorum = new ArrayList<>();
    private HashMap<Integer, Integer> acks = new HashMap<>();

    public PaxosComp(Init init){
        selfAdr = init.selfAdr;

        logPrefix = "<nid:" + selfAdr.getId() + ">";

        subscribe(leaderStartHandler, paxosLeaderPort);
        subscribe(newsPrepareHandler, paxosNewsPort);
        subscribe(handleStart, control);

        subscribe(handleLeaderPrepare, networkPort);
        subscribe(handleLeaderPromise, networkPort);
        subscribe(handleAccept, networkPort);

        subscribe(internalResponseHandler, paxosLeaderPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", "Paxos");
        }
    };

    //-------------------- proposer role --------------------

    Handler<PaxosLeaderStart> leaderStartHandler = new Handler<PaxosLeaderStart>() {
        @Override
        public void handle(PaxosLeaderStart paxosLeaderStart) {
            LOG.debug("{} got leader start for {}", logPrefix, paxosLeaderStart.getBallot());
            quorum = paxosLeaderStart.getQuorum();
            quorumSize = quorum.size();

            Prepare<Container<KAddress, NewsView>> leaderPrepare = new Prepare<>(paxosLeaderStart.getSuggestedLeader(), paxosLeaderStart.getBallot());

            for(KAddress member : quorum){
                KHeader header = new BasicHeader(selfAdr, member, Transport.UDP);
                KContentMsg msg = new BasicContentMsg(header, leaderPrepare);
                LOG.trace("{} sent leader select prepare to {}", logPrefix, member);
                trigger(msg, networkPort);
            }

        }
    };


    ClassMatchedHandler handleLeaderPromise
            = new ClassMatchedHandler<Promise<Boolean>, KContentMsg<?, ?, Promise<Boolean>>>() {

        @Override
        public void handle(Promise<Boolean> promise, KContentMsg<?, ?, Promise<Boolean>> container) {
            LOG.trace("{} received promise from: {}", logPrefix, container.getHeader().getSource());

            if(promise.getValue()){
                if(acks.containsKey(promise.getBallot())){
                    int temp = acks.get(promise.getBallot());
                    acks.put(promise.getBallot(), ++temp);
                }else{
                    acks.put(promise.getBallot(), 1);
                }

                if(acks.get(promise.getBallot()) == quorumSize){
                    trigger(new PaxosLeaderResponse(true), paxosLeaderPort);
                    acks.remove(promise.getBallot());
                    announceLeaderToQuorum();
                }
            }else{
                trigger(new PaxosLeaderResponse(false), paxosLeaderPort);
            }
        }
    };

    private void announceLeaderToQuorum(){
        Accept<KAddress> leaderAnnounce = new Accept<>(selfAdr);
        for(KAddress member: quorum){
            KHeader header = new BasicHeader(selfAdr, member, Transport.UDP);
            KContentMsg msg = new BasicContentMsg(header, leaderAnnounce);
            trigger(msg, networkPort);
        }
    }


    Handler<PaxosNewsPrepare> newsPrepareHandler = new Handler<PaxosNewsPrepare>() {
        @Override
        public void handle(PaxosNewsPrepare prepare) {
            LOG.info("got news prepare for {}", prepare.getNewsItem().getId());
            Accept<NewsItem> accept = new Accept<>(prepare.getNewsItem());

            for(KAddress member: prepare.getQuorum()){
                KHeader header = new BasicHeader(selfAdr, member, Transport.UDP);
                KContentMsg msg = new BasicContentMsg(header, accept);
                trigger(msg, networkPort);
            }

        }
    };

    //---------------------- acceptor role -----------------

    ClassMatchedHandler handleLeaderPrepare
            = new ClassMatchedHandler<Prepare<Container<KAddress, NewsView>>, KContentMsg<?, ?, Prepare<Container<KAddress, NewsView>>>>() {

        @Override
        public void handle(Prepare<Container<KAddress, NewsView>> prepare, KContentMsg<?, ?, Prepare<Container<KAddress, NewsView>>> container) {
            LOG.trace("{} received prepare from: {}", logPrefix, container.getHeader().getSource());

            trigger(new PaxosLeaderInternalCheck(prepare.getValue(), prepare.getBallot()), paxosLeaderPort);

        }
    };

    ClassMatchedHandler handleAccept = new ClassMatchedHandler<Accept<?>, KContentMsg<?, ?, Accept<?>>>() {
        @Override
        public void handle(Accept<?> accept, KContentMsg<?, ?, Accept<?>> container) {
           handleAccept(accept);
        }
    };

    Handler<PaxosLeaderInternalResponse> internalResponseHandler = new Handler<PaxosLeaderInternalResponse>() {
        @Override
        public void handle(PaxosLeaderInternalResponse paxosLeaderInternalResponse) {

            KHeader header = new BasicHeader(selfAdr, paxosLeaderInternalResponse.getSuggestedLeader().getSource(), Transport.UDP);
            KContentMsg msg = new BasicContentMsg(header, new Promise<>(paxosLeaderInternalResponse.isPossibleLeader(), paxosLeaderInternalResponse.getMsgId()));
            trigger(msg, networkPort);
        }
    };

    private void handleAccept(Accept<?> accept){
        Object value = accept.getValue();
        if(value instanceof NewsItem)
            handle((NewsItem) value);
        else if(value instanceof KAddress)
            handle((KAddress) value);
    }

    private void handle(NewsItem newsItem){
        trigger(new PaxosNewsAnnounce(newsItem), paxosNewsPort);
    }

    private void handle(KAddress leader){
        trigger(new PaxosLeaderAnnounce(leader), paxosLeaderPort);
    }

    public static class Init extends se.sics.kompics.Init<PaxosComp> {

        public final KAddress selfAdr;

        public Init(KAddress selfAdr) {
            this.selfAdr = selfAdr;
        }
    }

}
