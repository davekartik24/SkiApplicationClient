import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class UploadDayLiftRidesPhases {

    public static final AtomicInteger badRequestCounter = new AtomicInteger(0);

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

    public static void main(String[] args) throws InterruptedException {
        UploadDayLiftRidesPhases obj = new UploadDayLiftRidesPhases(256, 20000, 40, 20, "http://ec2-54-186-128-171.us-west-2.compute.amazonaws.com:8080/SkierAssignment_war");
        obj.initiate();
    }

    public void initiate() throws InterruptedException {

        int total = 0;

        int firstPhaseNumThreads = numThreads/4;
        int secondPhaseCountDown = (int) (firstPhaseNumThreads * 0.1);
        int firstPhaseStartTime = 1;
        int firstPhaseEndTime = 90;

        ExecutorService firstPhaseThreadPool = Executors.newFixedThreadPool(firstPhaseNumThreads);
        CountDownLatch firstPhaseLatch = new CountDownLatch(secondPhaseCountDown);

        long startTime = System.currentTimeMillis();

        System.out.println("Phase1 Started");
        for (int i = 0; i < firstPhaseNumThreads; i++) {
            int startSkierIdRange = getThreadStartSkierIDRange(firstPhaseNumThreads, i);
            int endSkierIdRange = getThreadEndSkierIDRange(firstPhaseNumThreads, i);
            firstPhaseThreadPool
                    .execute(new UploadDayLiftRides(firstPhaseNumThreads, numSkiers, numLifts, numRuns, startSkierIdRange,
                            endSkierIdRange, firstPhaseStartTime, firstPhaseEndTime, 0.1, serverAddress, firstPhaseLatch));
        }
        total += (int) ((numRuns*0.1)*(numSkiers/firstPhaseNumThreads));
        firstPhaseThreadPool.shutdown();
        firstPhaseLatch.await();

        int secondPhaseNumThreads = numThreads;
        int thirdPhaseCountDown = (int) (secondPhaseNumThreads * 0.1);
        int secondPhaseStartTime = 91;
        int secondPhaseEndTime = 360;

        ExecutorService secondPhaseThreadPool = Executors.newFixedThreadPool(secondPhaseNumThreads);
        CountDownLatch secondPhaseLatch = new CountDownLatch(thirdPhaseCountDown);

        System.out.println("Phase2 Started");
        for (int i = 0; i < secondPhaseNumThreads; i++) {
            int startSkierIdRange = getThreadStartSkierIDRange(secondPhaseNumThreads, i);
            int endSkierIdRange = getThreadEndSkierIDRange(secondPhaseNumThreads, i);
            secondPhaseThreadPool
                    .execute(new UploadDayLiftRides(secondPhaseNumThreads, numSkiers, numLifts, numRuns, startSkierIdRange,
                            endSkierIdRange, secondPhaseStartTime, secondPhaseEndTime, 0.8, serverAddress, secondPhaseLatch));
        }
        total += (int) ((numRuns*0.8)*(numSkiers/secondPhaseNumThreads));
        secondPhaseThreadPool.shutdown();
        secondPhaseLatch.await();

        int thirdPhaseNumThreads = numThreads/4;
        int thirdPhaseStartTime = 361;
        int thirdPhaseEndTime = 420;

        ExecutorService thirdPhaseThreadPool = Executors.newFixedThreadPool(firstPhaseNumThreads);

        System.out.println("Phase3 Started");
        for (int i = 0; i < thirdPhaseNumThreads; i++) {
            int startSkierIdRange = getThreadStartSkierIDRange(thirdPhaseNumThreads, i);
            int endSkierIdRange = getThreadEndSkierIDRange(thirdPhaseNumThreads, i);
            thirdPhaseThreadPool
                    .execute(new UploadDayLiftRides(thirdPhaseNumThreads, numSkiers, numLifts, numRuns, startSkierIdRange,
                            endSkierIdRange, thirdPhaseStartTime, thirdPhaseEndTime, 0.1, serverAddress, null));
        }
        total += (int) ((numRuns*0.1)*(numSkiers/thirdPhaseNumThreads));

        thirdPhaseThreadPool.shutdown();

        firstPhaseThreadPool.awaitTermination(20, TimeUnit.MINUTES);
        secondPhaseThreadPool.awaitTermination(20, TimeUnit.MINUTES);
        thirdPhaseThreadPool.awaitTermination(20, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        System.out.println("Number of unsuccessful requests: " + badRequestCounter.get());
        System.out.println("The total run time (wall time) of the all phases to complete: " + TimeUnit.MILLISECONDS.toSeconds(endTime - startTime));
        System.out.println("Total : " + total);
    }

    private int getThreadStartSkierIDRange(int firstPhaseNumThreads, int threadCounter) {
        return (threadCounter * (numSkiers/firstPhaseNumThreads)) + 1;
    }

    private int getThreadEndSkierIDRange(int firstPhaseNumThreads, int threadCounter) {
        return ((numSkiers/firstPhaseNumThreads) * (threadCounter + 1));
    }
}
