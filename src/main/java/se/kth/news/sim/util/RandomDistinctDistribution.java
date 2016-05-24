package se.kth.news.sim.util;

import se.sics.kompics.simulator.adaptor.distributions.Distribution;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Love on 2016-05-24.
 */
public class RandomDistinctDistribution extends Distribution {

    private ArrayList<Integer> distinctSet;
    private Random random = new Random();

    public RandomDistinctDistribution(int min, int max){
        super(Type.OTHER, Integer.class);
        distinctSet = new ArrayList<>(max-min);

        for(int i = min; i < max; ++i)
            distinctSet.add(i);
    }

    public RandomDistinctDistribution(int min, int max, Random random){
        this(min, max);
        this.random = random;
    }

    @Override
    public Integer draw() {
        if(distinctSet.isEmpty()){
            return null;
        }else{
            int index = random.nextInt(distinctSet.size());
            int value = distinctSet.remove(index);
            return Integer.valueOf(value);
        }
    }
}
