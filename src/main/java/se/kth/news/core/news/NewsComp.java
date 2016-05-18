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
import se.kth.news.core.leader.LeaderSelectPort;
import se.kth.news.core.leader.LeaderUpdate;
import se.kth.news.core.news.messages.NewsItem;
import se.kth.news.core.news.util.NewsSet;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.core.paxos.PaxosLeaderPort;
import se.kth.news.core.paxos.PaxosNewsPort;
import se.kth.news.play.Ping;
import se.kth.news.play.Pong;
import se.kth.news.stats.messages.NewsStat;
import se.kth.news.util.BoundedArrayList;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
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
    //*******************************EXTERNAL_STATE*****************************
    private KAddress selfAdr;
    private Identifier gradientOId;
    private KAddress statServer;
    //*******************************INTERNAL_STATE*****************************
    private NewsView localNewsView;
    private CroupierSample<NewsView> currentNeighbours;
    private NewsSet newsSet = new NewsSet();
    private ArrayList<NewsItem> unsentNews = new ArrayList<>();


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
        subscribe(handleNewsItem, networkPort);
        subscribe(handlePong, networkPort);
        subscribe(unFloodedTimeoutHandler, timerPort);
        subscribe(sendStatTimeoutHandler, timerPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            updateLocalNewsView();
            generateNewsAtStart();
            startTimers();
        }
    };

    private void startTimers(){
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(100000, 1000);
        UnFloodedTimeout floodTimeout = new UnFloodedTimeout(spt);
        spt.setTimeoutEvent(floodTimeout);
        trigger(spt, timerPort);

        SchedulePeriodicTimeout spt2 = new SchedulePeriodicTimeout(0, 10000);
        SendStatsTimeout statsTimeout = new SendStatsTimeout(spt2);
        spt2.setTimeoutEvent(statsTimeout);
        trigger(spt2, timerPort);

    }

    private void generateNewsAtStart(){
        if(Math.random() >= 0.5f) {
            NewsItem item = new NewsItem(15);
            //unsentNews.add(item);
        }
       // floodToNeighbours(item);
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

            currentNeighbours = castSample;
        }
    };

    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {
            //Leave leader selection to the correct component
        }
    };

    Handler handleLeader = new Handler<LeaderUpdate>() {
        @Override
        public void handle(LeaderUpdate event) {
            LOG.info("{} was informed of a new leader: {}", logPrefix, event.leaderAdr);
        }
    };

    ClassMatchedHandler handleNewsItem
            = new ClassMatchedHandler<NewsItem, KContentMsg<?, ?, NewsItem>>() {

                @Override
                public void handle(NewsItem item, KContentMsg<?, ?, NewsItem> container) {
                    LOG.debug("{}received newsitem from:{}", logPrefix, container.getHeader().getSource());
                    newsSet.add(item);
                    updateLocalNewsView();
                    floodToNeighbours(item);
                }
            };

    private void floodToNeighbours(NewsItem item){
        if(!hasNeighbours()){
            unsentNews.add(item);
            return;
        }

        item.decreaseTTL();
        if(item.shouldFlood()) {

            Iterator<Identifier> it = currentNeighbours.publicSample.keySet().iterator();

            while (it.hasNext()) {
                KAddress neighbour = currentNeighbours.publicSample.get(it.next()).getSource();
                KHeader header = new BasicHeader(selfAdr, neighbour, Transport.UDP);

                KContentMsg msg = new BasicContentMsg(header, item);
                LOG.debug("{}sent item {} to {}", logPrefix, item.getId(), neighbour);
                trigger(msg, networkPort);
            }
        }
    }

    ClassMatchedHandler handlePong
            = new ClassMatchedHandler<Pong, KContentMsg<?, KHeader<?>, Pong>>() {

                @Override
                public void handle(Pong content, KContentMsg<?, KHeader<?>, Pong> container) {
                    LOG.info("{}received pong from:{}", logPrefix, container.getHeader().getSource());
                }
            };

    Handler<UnFloodedTimeout> unFloodedTimeoutHandler = new Handler<UnFloodedTimeout>() {
        @Override
        public void handle(UnFloodedTimeout unFloodedTimeout) {
            int unsentCount = unsentNews.size();
            for(int i = 0; i < unsentCount; ++i){
                NewsItem item = unsentNews.remove(0);
                floodToNeighbours(item);
            }
        }
    };

    Handler<SendStatsTimeout> sendStatTimeoutHandler = new Handler<SendStatsTimeout>() {

        @Override
        public void handle(SendStatsTimeout sendStatsTimeout) {
            KHeader header = new BasicHeader(selfAdr, statServer, Transport.UDP);
            KContentMsg msg = new BasicContentMsg(header, new NewsStat(newsSet.getIds()));
            trigger(msg, networkPort);
        }
    };

    private boolean hasNeighbours(){
        return currentNeighbours != null && !currentNeighbours.publicSample.isEmpty();
    }

    private static class UnFloodedTimeout extends Timeout{

        public UnFloodedTimeout(SchedulePeriodicTimeout request){
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
