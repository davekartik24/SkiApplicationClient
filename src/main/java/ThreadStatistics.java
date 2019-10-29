public class ThreadStatistics {

    private final long startTime;
    private final long endTime;
    private final long latency;
    private final int statusCode;
    private final String REQUEST_TYPE = "POST";

    public ThreadStatistics(long startTime, long endTime, int statusCode) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.latency = endTime - startTime;
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return startTime + " " +
                endTime + " " +
                latency + " " +
                statusCode + " " +
                REQUEST_TYPE;
    }
}
