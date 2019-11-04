package phases;

import analysis.ThreadStatistics;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;

public class UploadDayLiftRides implements Runnable {

    final static Logger logger = Logger.getLogger(UploadDayLiftRides.class);

    private final int numRuns;
    private final int numSkiers;
    private final int numLifts;
    private final int numThreads;
    private final int startSkierIDRange;
    private final int endSkierIDRange;
    private final int startTime;
    private final int endTime;
    private final double meanPercentage;
    private final String serverAddress;
    private final CountDownLatch latch;
    private final ArrayBlockingQueue<ThreadStatistics> totalStats;

    public UploadDayLiftRides(int numThreads, int numSkiers, int numLifts, int numRuns, int startSkierIDRange, int endSkierIDRange,
                              int startTime, int endTime, double meanPercentage, String serverAddress, CountDownLatch latch,
                              ArrayBlockingQueue<ThreadStatistics> totalStats) {
        this.numThreads = numThreads;
        this.numSkiers = numSkiers;
        this.numLifts = numLifts;
        this.numRuns = numRuns;
        this.startSkierIDRange = startSkierIDRange;
        this.endSkierIDRange = endSkierIDRange;
        this.startTime = startTime;
        this.endTime = endTime;
        this.meanPercentage = meanPercentage;
        this.serverAddress = serverAddress;
        this.latch = latch;
        this.totalStats = totalStats;
    }

    @Override
    public void run() {
        int numberOfRequest = (int) ((numRuns*meanPercentage)*(numSkiers/numThreads));
        List<ThreadStatistics> localStats = new ArrayList<>(numberOfRequest);

        for (int i = 0; i < numberOfRequest; i++) {
            int resortID = ThreadLocalRandom.current().nextInt(1, 11);
            int skierID = ThreadLocalRandom.current().nextInt(startSkierIDRange, endSkierIDRange + 1);
            String seasonID = "" + ThreadLocalRandom.current().nextInt(2010,  2020);
            String dayID = "" + ThreadLocalRandom.current().nextInt(1,  367);
            int liftID = ThreadLocalRandom.current().nextInt(1, numLifts + 1);
            int time = ThreadLocalRandom.current().nextInt(startTime, endTime + 1);

            SkiersApi apiInstance = new SkiersApi();
            ApiClient client = apiInstance.getApiClient();
            client.setBasePath(serverAddress);
            try {
                LiftRide inputLiftRide = new LiftRide();
                inputLiftRide.setLiftID(liftID);
                inputLiftRide.setTime(time);

                long startTime = System.currentTimeMillis();

                ApiResponse output = apiInstance
                        .writeNewLiftRideWithHttpInfo(inputLiftRide,resortID, seasonID, dayID, skierID);

                long endTime = System.currentTimeMillis();

                int statusCode = output.getStatusCode();

                localStats.add(new ThreadStatistics(startTime, endTime, statusCode));

                if (statusCode/100 == 4) {
                    System.out.println("Print 4xx");
                    UploadDayLiftRidesPhases.badRequestCounter.incrementAndGet();
                    logger.error("Invalid inputs supplied");
                    continue;
                }

                if(statusCode/100 == 5) {
                    System.out.println("Print 5xx");
                    UploadDayLiftRidesPhases.badRequestCounter.incrementAndGet();
                    logger.error("Web Server Error");
                    continue;
                }

                if (latch == null) {

                    SkiersApi getApiInstance = new SkiersApi();
                    ApiClient getClient = apiInstance.getApiClient();
                    getClient.setBasePath(serverAddress);

                    startTime = System.currentTimeMillis();

                    output = getApiInstance
                            .getSkierDayVerticalWithHttpInfo(resortID, seasonID, dayID, skierID);

                    endTime = System.currentTimeMillis();


                    statusCode = output.getStatusCode();

                    localStats.add(new ThreadStatistics(startTime, endTime, statusCode));

                    if (statusCode/100 == 4) {
                        System.out.println("Print 4xx");
                        UploadDayLiftRidesPhases.badRequestCounter.incrementAndGet();
                        logger.error("Invalid inputs supplied");
                    }

                    if(statusCode/100 == 5) {
                        System.out.println("Print 5xx");
                        UploadDayLiftRidesPhases.badRequestCounter.incrementAndGet();
                        logger.error("Web Server Error");
                    }
                }
            } catch (ApiException e) {
                System.out.println("Print Error " + e);
                UploadDayLiftRidesPhases.badRequestCounter.incrementAndGet();
                logger.error("Exception when calling ResortsApi#getResorts");
            }
        }
        totalStats.addAll(localStats);
        if (latch != null) {
            latch.countDown();
        }
    }
}
