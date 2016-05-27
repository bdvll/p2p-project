package se.kth.news.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.news.sim.DisseminationConfig;
import se.kth.news.stats.messages.NewsStat;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.config.KConfigOption;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private static final KConfigOption.Basic<Integer> viewSize = new KConfigOption.Basic("gradient.viewSize", Integer.class);

    private float minutesPassed  = 0;
    private static final int UPDATE_RATE = 5000;
    private static final float minutesPerRound = UPDATE_RATE/(float) 60000;
    private ArrayList<Float> dataPoints = new ArrayList<>();
    private boolean hasWritten = false;
    private int VIEW_SIZE;

    public StatComp(Init init){

        subscribe(startHandler, control);
        subscribe(handleNewsStat, this.network);
        subscribe(aggreaTimeoutHandler, timer);
        DisseminationConfig.readParams();
        VIEW_SIZE = viewSize.readValue(this.config()).get().intValue();

    }

    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {
            LOG.info("Stat started...");
            startTimers();
        }
    };

    private void startTimers(){
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(0, UPDATE_RATE);
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

            calculateStats();
            nodeData.clear();
        }
    };

    private void calculateStats(){
        float newsCount = longestBitStringSize();
        float nodeCount = nodeData.size();

        float[] newsCounters = new float[(int) newsCount];
        float[] nodeCounters = new float[(int) nodeCount];

        Iterator<String> biterator = nodeData.values().iterator();

        int nodeId = 0;
        while(biterator.hasNext()){
            String bitString = biterator.next();

            for(int i = 0; i < bitString.length(); ++i){
                if(bitString.charAt(i) == '1'){
                    nodeCounters[nodeId] ++;
                    newsCounters[i] ++;
                }
            }

            nodeId++;
        }

        float totalNewsCount = 0;
        float totalNodeCount = 0;

        for(int i = 0; i < newsCount; ++i) {
            newsCounters[i] /= nodeCount;
            totalNewsCount += newsCounters[i];
        }

        for(int i = 0; i < nodeCount; ++i) {
            nodeCounters[i] /= newsCount;
            totalNodeCount += nodeCounters[i];
        }

        totalNewsCount /= newsCount;
        totalNodeCount /= nodeCount;

        LOG.info("news: {}%, {}", totalNewsCount*100, "");//Arrays.toString(newsCounters));
        LOG.info("node: {}%, {}", totalNodeCount*100, "");//Arrays.toString(nodeCounters));

        dataPoints.add(totalNewsCount);
        if(!hasWritten && (int) totalNewsCount == 1){
            writeFile();
        }
    }

    private void writeFile(){
        hasWritten = true;
        int rate = DisseminationConfig.DISSEMINATION_RATE;
        int nodes = DisseminationConfig.NODE_COUNT;
        int churnRate = DisseminationConfig.CHURN_PERCENTAGE;
        try {
            FileWriter writer = new FileWriter(new File("testdata/imperfect_"+rate+"_"+nodes+"_"+VIEW_SIZE+"_"+churnRate));
            StringBuilder sb = new StringBuilder();
            sb.append("Minute,NewsCoverage\n");
            for(float value: dataPoints){
                minutesPassed += minutesPerRound;
                sb.append(minutesPassed).append(",").append(value).append("\n");
            }
            writer.write(sb.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int longestBitStringSize(){
        Iterator<String> iterator = nodeData.values().iterator();
        int largestSize = 0;
        while(iterator.hasNext()){
            String bitString = iterator.next();
            for(int i = bitString.length() -1; i >= 0; --i){
                if(bitString.charAt(i) == '1')
                    if(i + 1 > largestSize){
                        largestSize = i + 1;
                        break;
                    }
            }

        }
        return largestSize;
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
