package se.kth.news.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.news.stats.messages.NewsStat;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Created by Love on 2016-04-27.
 */
public class StatComp extends ComponentDefinition {

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    private static final Logger LOG = LoggerFactory.getLogger(StatComp.class);

    private HashMap<KAddress, String> nodeData = new HashMap<>();

    public StatComp(Init init){

        subscribe(startHandler, control);
        subscribe(handleNewsStat, this.network);
        subscribe(aggreaTimeoutHandler, timer);

    }

    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {
            LOG.info("Stat started...");
            startTimers();
        }
    };

    private void startTimers(){
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(0, 60000);
        AggregateTimeout aggTimeout = new AggregateTimeout(spt);
        spt.setTimeoutEvent(aggTimeout);
        trigger(spt, timer);
    }


    ClassMatchedHandler handleNewsStat
            = new ClassMatchedHandler<NewsStat, KContentMsg<KAddress, ?, NewsStat>>() {

        @Override
        public void handle(NewsStat item, KContentMsg<KAddress, ?, NewsStat> container) {
            KAddress source = container.getHeader().getSource();
            nodeData.put(source, item.getBitString());
        }
    };

    Handler<AggregateTimeout> aggreaTimeoutHandler = new Handler<AggregateTimeout>() {
        @Override
        public void handle(AggregateTimeout aggregateTimeout) {

            //calculateStats();
        }
    };
/*
    private void calculateStats(){
        HashMap<UUID, Integer> uniqueNews = new HashMap<>();

        Iterator<KAddress> it = nodeData.keySet().iterator();
        while(it.hasNext()){
            KAddress node = it.next();
            UUID[] newsSeen = nodeData.get(node);

            for(UUID id: newsSeen)
                uniqueNews.putIfAbsent(id, 0);
        }

        it = nodeData.keySet().iterator();
        float avgCoverage = 0;
        while(it.hasNext()){
            KAddress node = it.next();
            UUID[] newsSeen = nodeData.get(node);
            float nodeCoverage = (float) newsSeen.length / uniqueNews.size();
            avgCoverage += nodeCoverage;
            for(UUID id: newsSeen){
                int count = uniqueNews.get(id);
                uniqueNews.put(id, ++count);
            }
        }

        avgCoverage /= nodeData.size();

        LOG.info("avg node coverage {}", avgCoverage);

        float avgNewsCoverage = 0;
        Iterator<UUID> newsIds = uniqueNews.keySet().iterator();
        while(newsIds.hasNext()){
            UUID newsItem = newsIds.next();

            int count = uniqueNews.get(newsItem);
            float coverage = (float) count / nodeData.size();

            avgNewsCoverage += coverage;
        }

        avgNewsCoverage /= uniqueNews.size();

        LOG.info("avg news coverage {}", avgNewsCoverage);

    }

    */

    private static class AggregateTimeout extends Timeout{

        public AggregateTimeout(SchedulePeriodicTimeout request){
            super(request);
        }

    }

    public static class Init extends se.sics.kompics.Init<StatComp>{
        private KAddress self;

        public Init(KAddress self) {
            this.self = self;
        }
    }
}
