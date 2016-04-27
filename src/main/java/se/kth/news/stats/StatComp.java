package se.kth.news.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.news.core.news.messages.NewsItem;
import se.kth.news.stats.messages.NewsStat;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;

import java.sql.Time;
import java.util.*;

/**
 * Created by Love on 2016-04-27.
 */
public class StatComp extends ComponentDefinition {

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    private static final Logger LOG = LoggerFactory.getLogger(StatComp.class);

    private HashMap<KAddress, UUID[]> news = new HashMap<>();

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
            news.put(source, item.getIds());
        }
    };

    Handler<AggregateTimeout> aggreaTimeoutHandler = new Handler<AggregateTimeout>() {
        @Override
        public void handle(AggregateTimeout aggregateTimeout) {
            calculateStats();
        }
    };

    private void calculateStats(){
        HashSet<UUID> uniqueNews = new HashSet<>();

        Iterator<KAddress> it = news.keySet().iterator();
        while(it.hasNext()){
            KAddress node = it.next();
            UUID[] newsSeen = news.get(node);

            uniqueNews.addAll(Arrays.asList(newsSeen));
        }

        it = news.keySet().iterator();
        while(it.hasNext()){
            KAddress node = it.next();
            UUID[] newsSeen = news.get(node);

            
        }

    }

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