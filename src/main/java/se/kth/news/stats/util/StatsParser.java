package se.kth.news.stats.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Love on 2016-05-26.
 */
public class StatsParser {

    private enum Type {FIXED_TTL, FIXED_SIZE}

    public static void main(String[] args){
        parse("testdata/5_flood", Type.FIXED_TTL);
        parse("testdata/100_flood", Type.FIXED_SIZE);
    }

    private static void parse(String filename, Enum type){
        Pattern pattern = Pattern.compile("(\\d[.\\d]*)");
        try {
            String content = new Scanner(new File(filename)).useDelimiter("\\Z").next();
            Matcher matcher = pattern.matcher(content);
            ArrayList<Data> dataNodes = new ArrayList<>();
            int counter = 0;
            Data data = null;
            while(matcher.find()) {
                if(counter % 4 == 0){
                    data = new Data();
                }
                data.setData(counter, matcher.group(1));

                if(counter % 4 == 3)
                    dataNodes.add(data);
                counter++;

            }
            if(type.equals(Type.FIXED_TTL))
                toTTLCSV(dataNodes, "testdata/5_flood.csv");
            else
                toSizeCSV(dataNodes, "testdata/100_flood.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void toTTLCSV(ArrayList<Data> dataNodes, String filename){
        StringBuilder sb = new StringBuilder();
        sb.append("NodeCount,NewsCoverage\n");
        for(Data data: dataNodes){
            sb.append(data.nodeCount).append(",").append(data.nodeCoverage).append("\n");
        }
        write(sb.toString(), filename);
    }


    private static void toSizeCSV(ArrayList<Data> dataNodes, String filename){
        StringBuilder sb = new StringBuilder();
        sb.append("TTL,NewsCoverage\n");
        for(Data data: dataNodes){
            sb.append(data.ttl).append(",").append(data.nodeCoverage).append("\n");
        }
        write(sb.toString(), filename);
    }



    private static void write(String data, String filename){
        try {
            FileWriter writer = new FileWriter(new File(filename));
            writer.write(data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Data{
        int ttl;
        int nodeCount;
        float nodeCoverage;
        float newsCoverage;

        void setData(int i, String data){
            switch (i % 4){
                case 0: ttl = Integer.parseInt(data); break;
                case 1: nodeCount = Integer.parseInt(data); break;
                case 2: nodeCoverage = Float.parseFloat(data); break;
                case 3: newsCoverage = Float.parseFloat(data);break;
            }
        }
    }
}
