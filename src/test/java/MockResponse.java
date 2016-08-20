import spark.Response;

/**
 * Created by Sebastian on 15/08/2016.
 */
public class MockResponse extends Response {
    public void status(int status) {

    }
    public int status() {
        return 400;
    }

    public String body() {
        return "  ";
    }

    public void body(String d){

    }

}
