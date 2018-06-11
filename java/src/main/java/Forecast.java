import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Forecast {
    public static final int ONE_DAY = 1000 * 60 * 60 * 24 * 1;

    private String cityID(String city) throws IOException {
        // Find the id of the city on metawheather
        HttpRequestFactory requestFactory
                = new NetHttpTransport().createRequestFactory();
        HttpRequest request = requestFactory.buildGetRequest(
                new GenericUrl("https://www.metaweather.com/api/location/search/?query=" + city));
        String rawResponse = request.execute().parseAsString();
        JSONArray jsonArray = new JSONArray(rawResponse);
        String id = jsonArray.getJSONObject(0).get("woeid").toString();

        return id;
    }

    private JSONArray consolidatedWhetaher(String id) throws IOException {
        // Find the predictions for the city
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
        HttpRequest request = requestFactory.buildGetRequest(
                new GenericUrl("https://www.metaweather.com/api/location/" + id));
        String rawResponse = request.execute().parseAsString();
        JSONArray results = new JSONObject(rawResponse).getJSONArray("consolidated_weather");

        return results;
    }


    public boolean requestedDateSixDaysAhead(Date dateRequested) {
        Date sixDaysAhead = new Date(new Date().getTime() + ONE_DAY * 5);
        if (sixDaysAhead.before(dateRequested)) {
            return true;
        }
        return false;
    }

    public String predict(String city, Date datetime, boolean wind) throws IOException {
        // When date is not provided we look for the current prediction
        if (datetime == null) {
            datetime = new Date();
        }
        String format = new SimpleDateFormat("yyyy-MM-dd").format(datetime);

        if (requestedDateSixDaysAhead(datetime)) {
            throw new OutOfDate();
        }
        // If there are predictions

        String id = cityID(city);
        JSONArray results = consolidatedWhetaher(id);

        for (int i = 0; i < results.length(); i++) {
//            // When the date is the expected
            if (format.equals(results.getJSONObject(i).get("applicable_date").toString())) {
//                // If we have to return the wind information
                if (wind) {
                    return results.getJSONObject(i).get("wind_speed").toString();
                } else {
                    return results.getJSONObject(i).get("weather_state_name").toString();
                }
            }
        }
        return "";
    }
}
