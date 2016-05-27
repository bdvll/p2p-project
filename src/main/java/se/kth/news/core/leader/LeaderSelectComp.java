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
import se.kth.news.core.dissemination.events.LeaderRequest;
import se.kth.news.core.dissemination.events.LeaderResponse;
import se.kth.news.core.dissemination.ports.LeaderDisseminationPort;
import se.kth.news.core.leader.events.LeaderUpdate;
import se.kth.news.core.leader.ports.LeaderSelectPort;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.core.paxos.events.*;
import se.kth.news.core.paxos.ports.PaxosLeaderPort;
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
import se.sics.ktoolbox.util.config.KConfigOption;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.other.Container;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LeaderSelectComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(LeaderSelectComp.class);
    private String logPrefix = " ";

    //*******************************CONNECTIONS********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<GradientPort> gradientPort = requires(GradientPort.class);
    Negative<LeaderSelectPort> leaderUpdate = provides(LeaderSelectPort.class);
    Negative<PaxosLeaderPort> paxosLeaderPort = provides(PaxosLeaderPort.class);
    Positive<LeaderDisseminationPort> leaderDisseminationPort = requires(LeaderDisseminationPort.class);
    //*******************************EXTERNAL_STATE*****************************
    private KAddress selfAdr;
    //*******************************INTERNAL_STATE*****************************
    private Comparator viewComparator;
    private NewsView localNewsView;
    private List<Container<KAddress, NewsView>> gradientNeighbours;

    private KAddress currentLeader;

    private int gradientRounds = 0;
    private int patienceRounds = 0;
    private static int STATIC_ROUNDS;
    private static int CLIQUE_SIZE;
    private static int PATIENCE_ROUNDS;
    private static final KConfigOption.Basic<Integer> centerNodes = new KConfigOption.Basic("gradient.viewSize", Integer.class);
    private static final KConfigOption.Basic<Integer> staticRounds = new KConfigOption.Basic("leaderSelection.staticRounds", Integer.class);
    private static final KConfigOption.Basic<Integer> patience = new KConfigOption.Basic("leaderSelection.patience", Integer.class);

    private boolean leaderSelectionInitiated = false;

    public LeaderSelectComp(Init init) {
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        LOG.info("{}initiating...", logPrefix);

        viewComparator = init.viewComparator;

        CLIQUE_SIZE = centerNodes.readValue(this.config()).get().intValue();
        STATIC_ROUNDS = staticRounds.readValue(this.config()).get().intValue();
        PATIENCE_ROUNDS = patience.readValue(this.config()).get().intValue();

        subscribe(handleStart, control);
        subscribe(handleGradientSample, gradientPort);
        subscribe(paxosLeaderResponseHandler, paxosLeaderPort);
        subscribe(paxosLeaderInternalCheckHandler, paxosLeaderPort);
        subscribe(paxosLeaderAnnounceHandler, paxosLeaderPort);

        subscribe(leaderRequestHandler, leaderDisseminationPort);
        subscribe(leaderUpdateHandler, leaderDisseminationPort);

        subscribe(handleLeaderUpdateFromNews, leaderUpdate);
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
                if(waitedPatiently()){
                    startPaxosSelection();
                }
            }else{
                patienceRounds = 0;
            }

        }
    };

    private boolean iAmLocalLeader(List<Container<KAddress, NewsView>> containers, NewsView self){
        if(gradientRounds < STATIC_ROUNDS) return false;
        if(gradientNeighbours.size() < CLIQUE_SIZE) return false;
        for(Container<KAddress, NewsView> container: containers)
            if(viewComparator.compare(container.getContent(), self) >= 0)
                return false;
        return true;
    }

    private boolean iAmGlobalLeader(){
        return currentLeader != null && currentLeader.equals(selfAdr);
    }

    private boolean waitedPatiently(){
        if(currentLeader == null)
            return true;
        return(++patienceRounds == PATIENCE_ROUNDS);
    }

    private void startPaxosSelection(){
        patienceRounds = 0;
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
            LOG.debug("{} has {} news and suggested leader has {} news", logPrefix, localNewsView.localNewsCount, paxosLeaderInternalCheck.getSuggestedLeader().getContent().localNewsCount);
            for(Container<?, NewsView> view: gradientNeighbours)
                myViews.add(view.getContent());


            NewsView suggestedView = paxosLeaderInternalCheck.getSuggestedLeader().getContent();
            boolean possibleLeader = true;

            for(NewsView view: myViews){
                if(viewComparator.compare(suggestedView, view) < 0){
                    possibleLeader = false;
                }
            }

            if(myViews.contains(suggestedView)){
                int index = myViews.indexOf(suggestedView);
                NewsView realView = myViews.get(index);

                if (realView.localNewsCount < suggestedView.localNewsCount){
                    LOG.debug("{} thinks {} is maybe lying about its news count, wont trust for now!", logPrefix, paxosLeaderInternalCheck.getSuggestedLeader().getSource());
                    possibleLeader = false;
                }

            }else{
                LOG.debug("{} got leader prepare from outside of top", logPrefix);
                possibleLeader = false;
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

    Handler<LeaderRequest> leaderRequestHandler = new Handler<LeaderRequest>() {
        @Override
        public void handle(LeaderRequest leaderRequest) {
            trigger(new LeaderResponse(currentLeader, leaderRequest.getRequester(), leaderRequest.getBitString()), leaderDisseminationPort);
        }
    };

    Handler<se.kth.news.core.dissemination.events.LeaderUpdate> leaderUpdateHandler = new Handler<se.kth.news.core.dissemination.events.LeaderUpdate>() {
        @Override
        public void handle(se.kth.news.core.dissemination.events.LeaderUpdate leaderUpdate) {
            if(leaderUpdate.getLeader() != null && !leaderUpdate.getLeader().equals(currentLeader)){
                currentLeader = leaderUpdate.getLeader();
                trigger(new LeaderUpdate(currentLeader), LeaderSelectComp.this.leaderUpdate);
            }
        }
    };

    // if the newscomponent has discovered a new leader it will inform this component and let it inform the news component
    // to avoid any split brain
    Handler<LeaderUpdate> handleLeaderUpdateFromNews = new Handler<LeaderUpdate>() {
        @Override
        public void handle(LeaderUpdate leaderUpdate) {
            currentLeader = leaderUpdate.leaderAdr;
            trigger(new LeaderUpdate(currentLeader), LeaderSelectComp.this.leaderUpdate);
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
