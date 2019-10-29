import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UploadDayLiftRidesPhases {

    public static final AtomicInteger badRequestCounter = new AtomicInteger(0);

    private final int numThreads;
    private final int numSkiers;
    private final int numLifts;
    private final int numRuns;
    private final String serverAddress;
    private int totalRequest;
    public final ArrayBlockingQueue<ThreadStatistics> totalStats;

    public UploadDayLiftRidesPhases(int numThreads, int numSkiers, int numLifts, int numRuns, String serverAddress) {
        this.numThreads = numThreads;
        this.numSkiers = numSkiers;
        this.numLifts = numLifts;
        this.numRuns = numRuns;
        this.serverAddress = serverAddress;
        this.totalRequest = (int) ((((numRuns * 0.1) * (numSkiers / (numThreads/4)) * (numThreads/4)) * 2) + (((numRuns * 0.8) * (numSkiers / numThreads)) * numThreads));
        this.totalStats = new ArrayBlockingQueue<>(totalRequest);
    }

    public void initiate() throws InterruptedException, IOException {

        int firstPhaseNumThreads = numThreads/4;
        int secondPhaseCountDown = (int) (firstPhaseNumThreads * 0.1);
        int firstPhaseStartTime = 1;
        int firstPhaseEndTime = 90;

        ExecutorService firstPhaseThreadPool = Executors.newFixedThreadPool(firstPhaseNumThreads);
        CountDownLatch firstPhaseLatch = new CountDownLatch(secondPhaseCountDown);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < firstPhaseNumThreads; i++) {
            int startSkierIdRange = getThreadStartSkierIDRange(firstPhaseNumThreads, i);
            int endSkierIdRange = getThreadEndSkierIDRange(firstPhaseNumThreads, i);
            firstPhaseThreadPool
                    .execute(new UploadDayLiftRides(firstPhaseNumThreads, numSkiers, numLifts, numRuns, startSkierIdRange,
                            endSkierIdRange, firstPhaseStartTime, firstPhaseEndTime, 0.1, serverAddress,
                            firstPhaseLatch, totalStats));
        }
        firstPhaseThreadPool.shutdown();
        firstPhaseLatch.await();

        int secondPhaseNumThreads = numThreads;
        int thirdPhaseCountDown = (int) (secondPhaseNumThreads * 0.1);
        int secondPhaseStartTime = 91;
        int secondPhaseEndTime = 360;

        ExecutorService secondPhaseThreadPool = Executors.newFixedThreadPool(secondPhaseNumThreads);
        CountDownLatch secondPhaseLatch = new CountDownLatch(thirdPhaseCountDown);

        for (int i = 0; i < secondPhaseNumThreads; i++) {
            int startSkierIdRange = getThreadStartSkierIDRange(secondPhaseNumThreads, i);
            int endSkierIdRange = getThreadEndSkierIDRange(secondPhaseNumThreads, i);
            secondPhaseThreadPool
                    .execute(new UploadDayLiftRides(secondPhaseNumThreads, numSkiers, numLifts, numRuns, startSkierIdRange,
                            endSkierIdRange, secondPhaseStartTime, secondPhaseEndTime, 0.8, serverAddress, secondPhaseLatch, totalStats));
        }
        secondPhaseThreadPool.shutdown();
        secondPhaseLatch.await();

        int thirdPhaseNumThreads = numThreads/4;
        int thirdPhaseStartTime = 361;
        int thirdPhaseEndTime = 420;

        ExecutorService thirdPhaseThreadPool = Executors.newFixedThreadPool(firstPhaseNumThreads);

        for (int i = 0; i < thirdPhaseNumThreads; i++) {
            int startSkierIdRange = getThreadStartSkierIDRange(thirdPhaseNumThreads, i);
            int endSkierIdRange = getThreadEndSkierIDRange(thirdPhaseNumThreads, i);
            thirdPhaseThreadPool
                    .execute(new UploadDayLiftRides(thirdPhaseNumThreads, numSkiers, numLifts, numRuns, startSkierIdRange,
                            endSkierIdRange, thirdPhaseStartTime, thirdPhaseEndTime, 0.1, serverAddress, null, totalStats));
        }

        thirdPhaseThreadPool.shutdown();

        firstPhaseThreadPool.awaitTermination(20, TimeUnit.MINUTES);
        secondPhaseThreadPool.awaitTermination(20, TimeUnit.MINUTES);
        thirdPhaseThreadPool.awaitTermination(20, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        int unsuccessfulRequest = badRequestCounter.get();

        try (FileWriter fw = new FileWriter("totalStats.csv")) {
            for (ThreadStatistics element : totalStats) {
                fw.write(element.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader in = new BufferedReader(new FileReader("totalStats.csv"));
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

        long wallTime = TimeUnit.MILLISECONDS.toSeconds(endTime - startTime);

        System.out.println("Number of successful requests: " + (totalRequest - unsuccessfulRequest));
        System.out.println("Number of unsuccessful requests: " + unsuccessfulRequest);
        System.out.println("The total run time (wall time) of the all phases to complete: " + wallTime);
        System.out.println("The statistics of the run -> ");
        System.out.println("Mean Response time: " + stats.getAverage());
        System.out.println("Median response time: " + median(totalStatsResponseTime));
        System.out.println("Throughput: " + (stats.getCount()/wallTime));
        System.out.println("99th Percentile response time: " + percentile(totalStatsResponseTime, 99));
        System.out.println("Maximum Response Time: " + stats.getMax());
    }

    private int getThreadStartSkierIDRange(int firstPhaseNumThreads, int threadCounter) {
        return (threadCounter * (numSkiers/firstPhaseNumThreads)) + 1;
    }

    private int getThreadEndSkierIDRange(int firstPhaseNumThreads, int threadCounter) {
        return ((numSkiers/firstPhaseNumThreads) * (threadCounter + 1));
    }

    private int percentile(List<Integer> latencies, double Percentile)
    {
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
