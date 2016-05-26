package se.kth.news.sim;

import java.io.*;
import java.util.Scanner;

/**
 * Created by Love on 2016-05-26.
 */
public class FloodingConfig {

    public static int TTL;
    public static int NODE_COUNT = 100;

    public static void saveParams(int ttl, int nodeCount){
        try {
            FileWriter writer = new FileWriter(new File("testinput/inputparams"));
            writer.write(ttl+","+nodeCount);
            TTL = ttl;
            NODE_COUNT = nodeCount;
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
            TTL = Integer.parseInt(data[0]);
            NODE_COUNT = Integer.parseInt(data[1]);

            scan.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
