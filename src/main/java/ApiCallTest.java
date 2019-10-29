import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

public class ApiCallTest {

    public static void main(String[] args) {

        String basePath = "http://ec2-54-186-128-171.us-west-2.compute.amazonaws.com:8080/SkierAssignment_war";
        SkiersApi apiInstance = new SkiersApi();
        ApiClient client = apiInstance.getApiClient();
        client.setBasePath(basePath);
        try {
            LiftRide inputLiftRide = new LiftRide();
            inputLiftRide.setLiftID(21);
            inputLiftRide.setTime(271);
            ApiResponse output = apiInstance.writeNewLiftRideWithHttpInfo(inputLiftRide,1, "2019", "1", 123);
            System.out.println(output.getStatusCode());
        } catch (ApiException e) {
            System.err.println("Exception when calling ResortsApi#getResorts");
            e.printStackTrace();
        }
    }
}
