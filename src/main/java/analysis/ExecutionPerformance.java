package analysis;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ExecutionPerformance {

    private final ArrayBlockingQueue<ThreadStatistics> totalStats;
    private final long executionStartTime;
    private final long executionEndTime;

    public ExecutionPerformance(ArrayBlockingQueue<ThreadStatistics> totalStats, long executionStartTime, long executionEndTime) {
        this.totalStats = totalStats;
        this.executionStartTime = executionStartTime;
        this.executionEndTime = executionEndTime;
    }

    public String storeAndGetAnalysis() throws IOException {
        String fileName = "totalStats.csv";
        try (FileWriter fw = new FileWriter(fileName)) {
            for (ThreadStatistics element : totalStats) {
                fw.write(element.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String str;

        List<Integer> totalStatsResponseTime = new ArrayList<>();
        while((str = in.readLine()) != null){
            String[] input = str.split(" ");
            if(input.length == 5) {
                totalStatsResponseTime.add(Integer.parseInt(input[2]));
            }
        }

        IntSummaryStatistics stats = totalStatsResponseTime.stream()
                .mapToInt((x) -> x)
                .summaryStatistics();

        long wallTime = TimeUnit.MILLISECONDS.toSeconds(executionEndTime - executionStartTime);

        System.out.println("The statistics of the run -> ");
        System.out.println("Mean Response time: " + stats.getAverage());
        System.out.println("Median response time: " + median(totalStatsResponseTime));
        System.out.println("Throughput: " + (stats.getCount()/wallTime));
        System.out.println("99th Percentile response time: " + percentile(totalStatsResponseTime, 99));
        System.out.println("Maximum Response Time: " + stats.getMax());
        return fileName;
    }


    private int percentile(List<Integer> latencies, double Percentile) {
        Collections.sort(latencies);
        int index = (int)Math.ceil(((double)Percentile / (double)100) * (double)latencies.size());
        return latencies.get(index-1);
    }

    private double median(List<Integer> latencies) {
        Collections.sort(latencies);
        double median;
        if (latencies.size() % 2 == 0)
            median = ((double)latencies.get(latencies.size()/2) + (double)latencies.get(latencies.size()/2 - 1))/2;
        else
            median = (double) latencies.get(latencies.size()/2);
        return median;
    }
}
