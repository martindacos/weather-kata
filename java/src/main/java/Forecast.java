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

    public String predict(String city, Date day, boolean wind) throws IOException {
        if (wind) {
            return predictWindSpeed(city, day);
        }
        return predictWheater(city, day);
    }

    public String predictWindSpeed(String city, Date dayRequested) throws IOException {
        if (dayRequested == null) {
            dayRequested = new Date();
        }


        if (requestedDateFiveDaysAhead(dayRequested)) {
            return  "";
        }
        // If there are predictions

        JSONObject requestedDay = getPredictionFromAPI(city, dayRequested);


        return requestedDay.get("wind_speed").toString();
    }

    public String predictWheater(String city, Date dayRequested) throws IOException {
        // When date is not provided we look for the current prediction
        if (dayRequested == null) {
            dayRequested = new Date();
        }


        if (requestedDateFiveDaysAhead(dayRequested)) {
            return  "";
        }


        JSONObject requestedDay = getPredictionFromAPI(city, dayRequested);

        return requestedDay.get("weather_state_name").toString();
    }



    private String getCityIDFromAPI(String city) throws IOException {
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

    private JSONArray getWheatherFromAPI(String id) throws IOException {
        // Find the predictions for the city
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
        HttpRequest request = requestFactory.buildGetRequest(
                new GenericUrl("https://www.metaweather.com/api/location/" + id));
        String rawResponse = request.execute().parseAsString();
        JSONArray results = new JSONObject(rawResponse).getJSONArray("consolidated_weather");

        return results;
    }


    public boolean requestedDateFiveDaysAhead(Date dateRequested) {
        Date sixDaysAhead = new Date(new Date().getTime() + ONE_DAY * 5);
        if (sixDaysAhead.before(dateRequested)) {
            return true;
        }
        return false;
    }

    public JSONObject extractRequestedDay(JSONArray results, Date day) {
        String dayFormatted = new SimpleDateFormat("yyyy-MM-dd").format(day);
        for (int i = 0; i < results.length(); i++) {
            // When the date is the expected
            if (dayFormatted.equals(results.getJSONObject(i).get("applicable_date").toString())) {
                return results.getJSONObject(i);
            }
        }
        return new JSONObject();
    }



    private JSONObject getPredictionFromAPI(String city, Date dayRequested) throws IOException {
        String id = getCityIDFromAPI(city);
        JSONArray results = getWheatherFromAPI(id);
        return extractRequestedDay(results, dayRequested);
    }

}
