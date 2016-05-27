package se.kth.news.sim;

import java.io.*;
import java.util.Scanner;

/**
 * Created by Love on 2016-05-26.
 */
public class DisseminationConfig {

    public static int NODE_COUNT = 100;
    public static int DISSEMINATION_RATE = 5000;
    public static int CHURN_PERCENTAGE = 10;

    public static void saveParams(int rate, int nodeCount, int churnPercent){
        try {
            FileWriter writer = new FileWriter(new File("testinput/inputparams"));
            writer.write(rate+","+nodeCount+","+churnPercent);
            DISSEMINATION_RATE = rate;
            NODE_COUNT = nodeCount;
            CHURN_PERCENTAGE = churnPercent;
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readParams(){
        try {
            Scanner scan = new Scanner(new File("testinput/inputparams"));
            String line = scan.nextLine();

            String[] data = line.split(",");
            DISSEMINATION_RATE = Integer.parseInt(data[0]);
            NODE_COUNT = Integer.parseInt(data[1]);
            CHURN_PERCENTAGE = Integer.parseInt(data[2]);

            scan.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}