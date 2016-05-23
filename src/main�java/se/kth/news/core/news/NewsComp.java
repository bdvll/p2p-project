/*
 * 2016 Royal Institute of Technology (KTH)
 *
 * LSelector is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.news.core.news;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.news.core.dissemination.events.*;
import se.kth.news.core.dissemination.ports.NewsDisseminationPort;
import se.kth.news.core.leader.ports.LeaderSelectPort;
import se.kth.news.core.leader.events.LeaderUpdate;
import se.kth.news.core.news.messages.NewsCreateRequest;
import se.kth.news.core.news.messages.NewsCreateResponse;
import se.kth.news.core.news.messages.NewsItem;
import se.kth.news.core.news.util.NewsSet;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.core.paxos.events.PaxosNewsAnnounce;
import se.kth.news.core.paxos.events.PaxosNewsPrepare;
import se.kth.news.core.paxos.ports.PaxosNewsPort;
import se.kth.news.play.Pong;
import se.kth.news.stats.messages.NewsStat;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.other.Container;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NewsComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(NewsComp.class);
    private String logPrefix = " ";



    //*******************************CONNECTIONS********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<CroupierPort> croupierPort = requires(CroupierPort.class);
    Positive<GradientPort> gradientPort = requires(GradientPort.class);
    Positive<LeaderSelectPort> leaderPort = requires(LeaderSelectPort.class);
    Negative<OverlayViewUpdatePort> viewUpdatePort = provides(OverlayViewUpdatePort.class);
    Negative<PaxosNewsPort> paxosNewsPort = provides(PaxosNewsPort.class);
    Positive<NewsDisseminationPort> newsDisseminationPort = requires(NewsDisseminationPort.class);
    //*******************************EXTERNAL_STATE*****************************
    private KAddress selfAdr;
    private Identifier gradientOId;
    private KAddress statServer;
    //*******************************INTERNAL_STATE*****************************
    private NewsView localNewsView;

    private NewsSet newsSet = new NewsSet();
    private ArrayList<NewsCreateRequest> unsentNews = new ArrayList<>();
    private KAddress currentLeader;
    private ArrayList<KAddress> gradientNeighbours = new ArrayList<>();


    public NewsComp(Init init) {
        selfAdr = init.selfAdr;
        statServer = init.statServer;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        LOG.info("{}initiating...", logPrefix);

        gradientOId = init.gradientOId;

        subscribe(handleStart, control);
        subscribe(handleCroupierSample, croupierPort);
        subscribe(handleGradientSample, gradientPort);
        subscribe(handleLeader, leaderPort);
        subscribe(newsRequestTimeoutHandler, timerPort);
        subscribe(sendStatTimeoutHandler, timerPort);

        subscribe(bitStringRequestHandler, newsDisseminationPort);
        subscribe(newsUpdate, newsDisseminationPort);
        subscribe(newsRequestHandler, newsDisseminationPort);

        subscribe(newsCreateRequestHandler, networkPort);
        subscribe(newsCreateResponseHandler, networkPort);

        subscribe(paxosNewsAnnounceHandler, paxosNewsPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            updateLocalNewsView();
            startTimers();
        }
    };

    private void startTimers(){
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(100000, 5000);
        NewsRequestTimeout newsRequestTimeout = new NewsRequestTimeout(spt);
        spt.setTimeoutEvent(newsRequestTimeout);
        trigger(spt, timerPort);

        SchedulePeriodicTimeout spt2 = new SchedulePeriodicTimeout(0, 10000);
        SendStatsTimeout statsTimeout = new SendStatsTimeout(spt2);
        spt2.setTimeoutEvent(statsTimeout);
        trigger(spt2, timerPort);

    }

    private void updateLocalNewsView() {
        localNewsView = new NewsView(selfAdr.getId(), newsSet.getNewsCount());
        LOG.debug("{}informing overlays of new view", logPrefix);
        trigger(new OverlayViewUpdate.Indication<>(gradientOId, false, localNewsView.copy()), viewUpdatePort);
    }

    Handler handleCroupierSample = new Handler<CroupierSample<NewsView>>() {
        @Override
        public void handle(CroupierSample<NewsView> castSample) {
            if (castSample.publicSample.isEmpty()) {
                return;
            }
        }
    };

    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {
            gradientNeighbours.clear();
            List<Container<KAddress, ? >> list = sample.getGradientNeighbours();
            for(Container<KAddress, ?> container: list){
                gradientNeighbours.add(container.getSource());
            }
        }
    };

    Handler handleLeader = new Handler<LeaderUpdate>() {
        @Override
        public void handle(LeaderUpdate event) {
            LOG.info("{} was informed of a new leader: {}", logPrefix, event.leaderAdr);
            currentLeader = event.leaderAdr;
        }
    };

    Handler<NewsRequestTimeout> newsRequestTimeoutHandler = new Handler<NewsRequestTimeout>() {
        @Override
        public void handle(NewsRequestTimeout newsRequestTimeout) {
            sendUnsentNews();
        }
    };

    Handler<SendStatsTimeout> sendStatTimeoutHandler = new Handler<SendStatsTimeout>() {

        @Override
        public void handle(SendStatsTimeout sendStatsTimeout) {
            KHeader header = new BasicHeader(selfAdr, statServer, Transport.UDP);
            KContentMsg msg = new BasicContentMsg(header, new NewsStat(newsSet.getBitString()));
            trigger(msg, networkPort);
        }
    };

    Handler<BitStringRequest> bitStringRequestHandler = new Handler<BitStringRequest>() {
        @Override
        public void handle(BitStringRequest bitStringRequest) {
            trigger(new BitStringResponse(newsSet.getBitString()), newsDisseminationPort);
        }
    };

    Handler<NewsUpdate> newsUpdate = new Handler<NewsUpdate>() {
        @Override
        public void handle(NewsUpdate newsUpdate) {
            for(NewsItem item: newsUpdate.getNews())
                newsSet.add(item);
            updateLocalNewsView();
        }
    };

    Handler<NewsRequest> newsRequestHandler = new Handler<NewsRequest>() {
        @Override
        public void handle(NewsRequest newsRequest) {
            NewsItem[] unseenNews = newsSet.getUnseenNews(newsRequest.getBitString());
            trigger(new NewsResponse(newsRequest.getRequester(), newsRequest.getLeader(), unseenNews), newsDisseminationPort);
        }
    };

    ClassMatchedHandler<NewsCreateRequest, KContentMsg<KAddress, ?, NewsCreateRequest>> newsCreateRequestHandler = new ClassMatchedHandler<NewsCreateRequest, KContentMsg<KAddress, ?, NewsCreateRequest>>() {
        @Override
        public void handle(NewsCreateRequest request, KContentMsg<KAddress, ?, NewsCreateRequest> content) {
            if(selfAdr.equals(currentLeader)){
                NewsItem item = request.getNewsItem();
                item.setId(newsSet.getNextId());
                newsSet.add(item);
                updateLocalNewsView();
                trigger(new PaxosNewsPrepare(item, gradientNeighbours), paxosNewsPort);

                KHeader header = new BasicHeader(selfAdr, content.getHeader().getSource(), Transport.UDP);
                KContentMsg msg = new BasicContentMsg(header, new NewsCreateResponse(currentLeader, true, request.getMessageId()));
                trigger(msg, networkPort);
            }else{
                // if you're not leader, tell the requester to ask your leader
                KHeader header = new BasicHeader(selfAdr, content.getHeader().getSource(), Transport.UDP);
                KContentMsg msg = new BasicContentMsg(header, new NewsCreateResponse(currentLeader, false, request.getMessageId()));
                trigger(msg, networkPort);
            }
        }
    };

    ClassMatchedHandler<NewsCreateResponse, KContentMsg<?, ?, NewsCreateResponse>> newsCreateResponseHandler = new ClassMatchedHandler<NewsCreateResponse, KContentMsg<?, ?, NewsCreateResponse>>() {
        @Override
        public void handle(NewsCreateResponse response, KContentMsg<?, ?, NewsCreateResponse> content) {
            if(response.isSuccess()){
                NewsCreateRequest fakeRequest = new NewsCreateRequest(null, response.getMessageId());
                unsentNews.remove(fakeRequest);
            }else{
                trigger(new LeaderUpdate(response.getLeader()), leaderPort);
            }
        }
    };

    Handler<PaxosNewsAnnounce> paxosNewsAnnounceHandler = new Handler<PaxosNewsAnnounce>() {
        @Override
        public void handle(PaxosNewsAnnounce paxosNewsAnnounce) {
            newsSet.add(paxosNewsAnnounce.getNewsItem());
            updateLocalNewsView();
        }
    };

    private void sendUnsentNews(){
        KHeader header = new BasicHeader(selfAdr, currentLeader, Transport.UDP);
        for(NewsCreateRequest request: unsentNews){
            KContentMsg msg = new BasicContentMsg(header, request);
            trigger(msg, networkPort);
        }
    }

    private void publishNewsItem(NewsItem item){
        NewsCreateRequest request = new NewsCreateRequest(item, (int) (Math.random()*Integer.MAX_VALUE));
        unsentNews.add(request);

        KHeader header = new BasicHeader(selfAdr, currentLeader, Transport.UDP);
        KContentMsg msg = new BasicContentMsg(header, request);
        trigger(msg, networkPort);
    }

    private static class NewsRequestTimeout extends Timeout{

        public NewsRequestTimeout(SchedulePeriodicTimeout request){
            super(request);
        }

    }

    private static class SendStatsTimeout extends Timeout{

        public SendStatsTimeout(SchedulePeriodicTimeout request){
            super(request);
        }
    }

    public static class Init extends se.sics.kompics.Init<NewsComp> {

        public final KAddress selfAdr;
        public final Identifier gradientOId;
        public final KAddress statServer;

        public Init(KAddress selfAdr, Identifier gradientOId, KAddress statServer) {
            this.selfAdr = selfAdr;
            this.gradientOId = gradientOId;
            this.statServer = statServer;
        }
    }
}
