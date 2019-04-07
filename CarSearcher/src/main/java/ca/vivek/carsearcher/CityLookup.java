package ca.vivek.carsearcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONException;
import org.json.JSONObject;

class CityLookup {
	
	private String city, region, country, currency;

    public CityLookup() throws IOException 
    {
    	
    }
    
    protected void findLocation(String ip) throws IOException, JSONException
    {
        URL ipapi = new URL("https://ipapi.co/" + ip + "/json/");

        URLConnection c = ipapi.openConnection();
        c.setRequestProperty("User-Agent", "java-ipapi-client");
        BufferedReader reader = new BufferedReader(
          new InputStreamReader(c.getInputStream())
        );

        
        String line;
        String response = "";
        
        while ((line = reader.readLine()) != null)
        {
            response += line;
        }
        reader.close();
        
        JSONObject req = new JSONObject(response);
        
        city = req.getString("city");
        region = req.getString("region");
        country = req.getString("country_name");
        currency = req.getString("currency");
    }
    
    public String getCity() {
        return city;
    }

    public String getRegion() {
        return region;
    }

    public String getCountry() {
        return country;
    }
    
    public String getCurrency() {
        return currency;
    }
}