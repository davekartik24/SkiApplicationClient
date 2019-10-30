package phases;

import analysis.ExecutionPerformance;
import analysis.ThreadStatistics;

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
    private long phasesStartTime;
    private long phasesEndTime;

    private int totalRequest;
    public final ArrayBlockingQueue<ThreadStatistics> totalStats;

    public UploadDayLiftRidesPhases(int numThreads, int numSkiers, int numLifts, int numRuns, String serverAddress) {
        this.numThreads = numThreads;
        this.numSkiers = numSkiers;
        this.numLifts = numLifts;
        this.numRuns = numRuns;
        this.serverAddress = serverAddress;
//        Calculating the total number of request sent to the server based on the requirements.
        this.totalRequest = (int) ((((numRuns * 0.1) * (numSkiers / (numThreads/4)) * (numThreads/4)) * 2)
                                + (((numRuns * 0.8) * (numSkiers / numThreads)) * numThreads));
        this.totalStats = new ArrayBlockingQueue<>(totalRequest);
    }

    public void initiate() throws InterruptedException, IOException {

        phasesStartTime = System.currentTimeMillis();

        ExecutorService firstPhaseThreadPool = firstPhaseInitiate();

        ExecutorService secondPhaseThreadPool = secondPhaseInitiate();

        ExecutorService thirdPhaseThreadPool = thirdPhaseInitiate();

        firstPhaseThreadPool.awaitTermination(20, TimeUnit.MINUTES);
        secondPhaseThreadPool.awaitTermination(20, TimeUnit.MINUTES);
        thirdPhaseThreadPool.awaitTermination(20, TimeUnit.MINUTES);

        phasesEndTime = System.currentTimeMillis();

        int unsuccessfulRequest = badRequestCounter.get();

        long wallTime = TimeUnit.MILLISECONDS.toSeconds(phasesEndTime - phasesStartTime);

        System.out.println("Number of successful requests: " + (totalRequest - unsuccessfulRequest));
        System.out.println("Number of unsuccessful requests: " + unsuccessfulRequest);
        System.out.println("The total run time (wall time) of the all phases to complete: " + wallTime);

        ExecutionPerformance executionPerformance = new ExecutionPerformance(totalStats, phasesStartTime, phasesEndTime);

        executionPerformance.storeAndGetAnalysis();
    }

    private ExecutorService firstPhaseInitiate() throws InterruptedException {
        int firstPhaseNumThreads = numThreads/4;
        int secondPhaseCountDown = (int) (firstPhaseNumThreads * 0.1);
        int firstPhaseStartTime = 1;
        int firstPhaseEndTime = 90;

        ExecutorService firstPhaseThreadPool = Executors.newFixedThreadPool(firstPhaseNumThreads);
        CountDownLatch firstPhaseLatch = new CountDownLatch(secondPhaseCountDown);

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
        return firstPhaseThreadPool;
    }

    private ExecutorService secondPhaseInitiate() throws InterruptedException {
        int secondPhaseNumThreads = numThreads;
        int thirdPhaseCountDown = (int) (secondPhaseNumThreads * 0.1);
        int secondPhaseStartTime = 91;
        int secondPhaseEndTime = 360;
        double meanPercentage = 0.8;

        ExecutorService secondPhaseThreadPool = Executors.newFixedThreadPool(secondPhaseNumThreads);
        CountDownLatch secondPhaseLatch = new CountDownLatch(thirdPhaseCountDown);

        for (int i = 0; i < secondPhaseNumThreads; i++) {
            int startSkierIdRange = getThreadStartSkierIDRange(secondPhaseNumThreads, i);
            int endSkierIdRange = getThreadEndSkierIDRange(secondPhaseNumThreads, i);
            secondPhaseThreadPool
                    .execute(new UploadDayLiftRides(secondPhaseNumThreads, numSkiers, numLifts, numRuns, startSkierIdRange,
                            endSkierIdRange, secondPhaseStartTime, secondPhaseEndTime, meanPercentage, serverAddress, secondPhaseLatch, totalStats));
        }
        secondPhaseThreadPool.shutdown();
        secondPhaseLatch.await();
        return secondPhaseThreadPool;
    }

    private ExecutorService thirdPhaseInitiate() {
        int thirdPhaseNumThreads = numThreads/4;
        int thirdPhaseStartTime = 361;
        int thirdPhaseEndTime = 420;
        double meanPercentage = 0.1;

        ExecutorService thirdPhaseThreadPool = Executors.newFixedThreadPool(thirdPhaseNumThreads);

        for (int i = 0; i < thirdPhaseNumThreads; i++) {
            int startSkierIdRange = getThreadStartSkierIDRange(thirdPhaseNumThreads, i);
            int endSkierIdRange = getThreadEndSkierIDRange(thirdPhaseNumThreads, i);
            thirdPhaseThreadPool
                    .execute(new UploadDayLiftRides(thirdPhaseNumThreads, numSkiers, numLifts, numRuns, startSkierIdRange,
                            endSkierIdRange, thirdPhaseStartTime, thirdPhaseEndTime, meanPercentage, serverAddress,
                            null, totalStats));
        }
        thirdPhaseThreadPool.shutdown();
        return thirdPhaseThreadPool;
    }

    private int getThreadStartSkierIDRange(int firstPhaseNumThreads, int threadCounter) {
        return (threadCounter * (numSkiers/firstPhaseNumThreads)) + 1;
    }

    private int getThreadEndSkierIDRange(int firstPhaseNumThreads, int threadCounter) {
        return ((numSkiers/firstPhaseNumThreads) * (threadCounter + 1));
    }


}
