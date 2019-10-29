import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

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

    public UploadDayLiftRides(int numThreads, int numSkiers, int numLifts, int numRuns, int startSkierIDRange, int endSkierIDRange,
                              int startTime, int endTime, double meanPercentage, String serverAddress, CountDownLatch latch) {
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
    }

    @Override
    public void run() {
        int numberOfRequest = (int) ((numRuns*meanPercentage)*(numSkiers/numThreads));

        for (int i = 0; i < numberOfRequest; i++) {
            int skierID = ThreadLocalRandom.current().nextInt(startSkierIDRange, endSkierIDRange + 1);
            int liftID = ThreadLocalRandom.current().nextInt(1, numLifts + 1);
            int time = endTime - startTime;

            String basePath = serverAddress;
            SkiersApi apiInstance = new SkiersApi();
            ApiClient client = apiInstance.getApiClient();
            client.setBasePath(basePath);
            try {
                LiftRide inputLiftRide = new LiftRide();
                inputLiftRide.setLiftID(liftID);
                inputLiftRide.setTime(time);
                ApiResponse output = apiInstance
                        .writeNewLiftRideWithHttpInfo(inputLiftRide,1, "2019", "1", skierID);
                int statusCode = output.getStatusCode();

                if (statusCode/100 == 4) {
                    logger.error("Invalid inputs supplied");
                }

                if(statusCode/100 == 5) {
                    logger.error("Web Server Error");
                }
            } catch (ApiException e) {
                logger.error("Exception when calling ResortsApi#getResorts");
            } finally {
                latch.countDown();
            }
        }
    }
}
