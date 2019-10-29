import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)) {

            System.out.println("Please enter the details to start the SKI application client program");

            System.out.println("Maximum number of threads to run (max 256) : ");
            int numThreads = scanner.nextInt();

            System.out.println("Number of skier to generate lift rides for (max 50000): ");
            int numSkiers = scanner.nextInt();

            System.out.println("Number of ski lifts (range 5-60 and hit enter) : ");
            int numLifts = scanner.nextInt();

            System.out.println("Mean number of ski lifts each skier rides each day");
            int numRuns = scanner.nextInt();

            System.out.println("IP/Port address of the server");
            String serverAddress = scanner.next();

            UploadDayLiftRidesPhases uploadDayLiftRides = new UploadDayLiftRidesPhases(numThreads, numSkiers, numLifts, numRuns, serverAddress);

        }
    }
}
