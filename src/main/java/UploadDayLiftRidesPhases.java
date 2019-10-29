import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadDayLiftRidesPhases {

    private final int numThreads;
    private final int numSkiers;
    private final int numLifts;
    private final int numRuns;
    private final String serverAddress;

    public UploadDayLiftRidesPhases(int numThreads, int numSkiers, int numLifts, int numRuns, String serverAddress) {
        this.numThreads = numThreads;
        this.numSkiers = numSkiers;
        this.numLifts = numLifts;
        this.numRuns = numRuns;
        this.serverAddress = serverAddress;
    }

    public void initiate() throws InterruptedException {

        int firstPhaseNumThreads = numThreads/4;
        int secondPhaseCountDown = (int) (firstPhaseNumThreads * 0.1);
        int firstPhaseStartTime = 1;
        int firstPhaseEndTime = 90;

        ExecutorService firstPhaseThreadPool = Executors.newFixedThreadPool(firstPhaseNumThreads);
        CountDownLatch firstPhaseLatch = new CountDownLatch(secondPhaseCountDown);

//        int numRuns, int numSkiers, int numThreads, int startSkierIDRange, int endSkierIDRange,
//        int startTime, int endTime, float meanPercentage, String serverAddress, CountDownLatch latch

        for (int i = 0; i < firstPhaseNumThreads; i++) {
            int startSkierIdRange = getThreadStartSkierIDRange(firstPhaseNumThreads, i);
            int endSkierIdRange = getThreadEndSkierIDRange(firstPhaseNumThreads, i);
            firstPhaseThreadPool
                    .execute(new UploadDayLiftRides(firstPhaseNumThreads, numSkiers, numLifts, numRuns, startSkierIdRange,
                            endSkierIdRange, firstPhaseStartTime, firstPhaseEndTime, 0.1, serverAddress, firstPhaseLatch));
        }
        firstPhaseLatch.await();
    }

    private int getThreadStartSkierIDRange(int firstPhaseNumThreads, int threadCounter) {
        return (threadCounter * (numSkiers/firstPhaseNumThreads)) + 1;
    }

    private int getThreadEndSkierIDRange(int firstPhaseNumThreads, int threadCounter) {
        return ((numSkiers/firstPhaseNumThreads) * (threadCounter + 1));
    }
}
