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
package se.kth.news.core.leader;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.core.news.util.NewsViewComparator;
import se.kth.news.core.paxos.*;
import se.kth.news.util.BoundedArrayList;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.util.config.KConfigHelper;
import se.sics.ktoolbox.util.config.KConfigOption;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.other.Container;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LeaderSelectComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(LeaderSelectComp.class);
    private String logPrefix = " ";

    private static final int GRADIENT_NEIGHBOUR_SIZE = 2;

    //*******************************CONNECTIONS********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<GradientPort> gradientPort = requires(GradientPort.class);
    Negative<LeaderSelectPort> leaderUpdate = provides(LeaderSelectPort.class);
    Negative<PaxosLeaderPort> paxosLeaderPort = provides(PaxosLeaderPort.class);
    //*******************************EXTERNAL_STATE*****************************
    private KAddress selfAdr;
    //*******************************INTERNAL_STATE*****************************
    private Comparator viewComparator;
    private NewsView localNewsView;
    private List<Container<KAddress, NewsView>> gradientNeighbours;

    private KAddress currentLeader;

    private int gradientRounds = 0;
    private static int STATIC_ROUNDS;
    private static int CLIQUE_SIZE;
    private static final KConfigOption.Basic<Integer> centerNodes = new KConfigOption.Basic("gradient.viewSize", Integer.class);
    private static final KConfigOption.Basic<Integer> staticRounds = new KConfigOption.Basic("leaderSelection.staticRounds", Integer.class);

    private boolean leaderSelectionInitiated = false;

    public LeaderSelectComp(Init init) {
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        LOG.info("{}initiating...", logPrefix);

        viewComparator = init.viewComparator;

        CLIQUE_SIZE = centerNodes.readValue(this.config()).get().intValue();
        STATIC_ROUNDS = staticRounds.readValue(this.config()).get().intValue();

        subscribe(handleStart, control);
        subscribe(handleGradientSample, gradientPort);
        subscribe(paxosLeaderResponseHandler, paxosLeaderPort);
        subscribe(paxosLeaderInternalCheckHandler, paxosLeaderPort);
        subscribe(paxosLeaderAnnounceHandler, paxosLeaderPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };
    
    Handler handleGradientSample = new Handler<TGradientSample>() {
        @Override
        public void handle(TGradientSample sample) {
            gradientRounds++;
           // LOG.debug("{}neighbours:{}", logPrefix, sample.gradientNeighbours);
           // LOG.debug("{}fingers:{}", logPrefix, sample.gradientFingers);
           // LOG.debug("{}local view:{}", logPrefix, sample.selfView);

            gradientNeighbours = sample.getGradientNeighbours();

            localNewsView = (NewsView) sample.selfView;

            if(iAmLocalLeader(gradientNeighbours, localNewsView) && !iAmGlobalLeader()){
                startPaxosSelection();
            }

        }
    };

    private boolean iAmLocalLeader(List<Container<KAddress, NewsView>> containers, NewsView self){
        if(gradientRounds < STATIC_ROUNDS) return false;
        for(Container<KAddress, NewsView> container: containers)
            if(viewComparator.compare(container.getContent(), self) >= 0)
                return false;
        return true;
    }

    private boolean iAmGlobalLeader(){
        return currentLeader != null && currentLeader.equals(selfAdr);
    }

    private void startPaxosSelection(){
        if(!leaderSelectionInitiated) {
            leaderSelectionInitiated = true;

            ArrayList<KAddress> quorum = new ArrayList<>(gradientNeighbours.size());
            for(Container<KAddress, ?> container : gradientNeighbours)
                quorum.add(container.getSource());

            Container<KAddress, NewsView> myContainer = new GradientContainer<>(selfAdr, localNewsView, 0 ,0);

            trigger(new PaxosLeaderStart(gradientRounds, quorum, myContainer), paxosLeaderPort);
        }
    }

    Handler<PaxosLeaderResponse> paxosLeaderResponseHandler = new Handler<PaxosLeaderResponse>() {
        @Override
        public void handle(PaxosLeaderResponse paxosLeaderResponse) {
            leaderSelectionInitiated = false;
            if(paxosLeaderResponse.isAccepted()){
                currentLeader = selfAdr;
                LOG.info("{} is GREATEST LEADER!", selfAdr);
                trigger(new LeaderUpdate(selfAdr), leaderUpdate);
            }else{
                LOG.info("{} is not allowed to be leader :(", logPrefix);
            }

        }
    };

    Handler<PaxosLeaderInternalCheck> paxosLeaderInternalCheckHandler = new Handler<PaxosLeaderInternalCheck>() {
        @Override
        public void handle(PaxosLeaderInternalCheck paxosLeaderInternalCheck) {
            ArrayList<NewsView> myViews = new ArrayList<>();
            myViews.add(localNewsView);
            for(Container<?, NewsView> view: gradientNeighbours)
                myViews.add(view.getContent());


            NewsView suggestedView = paxosLeaderInternalCheck.getSuggestedLeader().getContent();
            boolean possibleLeader = true;

            for(NewsView view: myViews){
                if(viewComparator.compare(suggestedView, view) < 0){
                    possibleLeader = false;
                }
            }

            trigger(new PaxosLeaderInternalResponse(paxosLeaderInternalCheck.getSuggestedLeader(), possibleLeader, paxosLeaderInternalCheck.getMsgId()), paxosLeaderPort);

        }
    };

    Handler<PaxosLeaderAnnounce> paxosLeaderAnnounceHandler = new Handler<PaxosLeaderAnnounce>() {
        @Override
        public void handle(PaxosLeaderAnnounce paxosLeaderAnnounce) {
            currentLeader = paxosLeaderAnnounce.getLeader();
            trigger(new LeaderUpdate(currentLeader), leaderUpdate);
        }
    };

    public static class Init extends se.sics.kompics.Init<LeaderSelectComp> {

        public final KAddress selfAdr;
        public final Comparator viewComparator;

        public Init(KAddress selfAdr, Comparator viewComparator) {
            this.selfAdr = selfAdr;
            this.viewComparator = viewComparator;
        }
    }
}
