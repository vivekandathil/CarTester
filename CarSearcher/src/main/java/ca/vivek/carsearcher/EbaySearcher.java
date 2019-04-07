package ca.vivek.carsearcher;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.common.graph.ElementOrder.Type;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class EbaySearcher 
{
	public EbaySearcher() throws IOException, JSONException 
	{
		
	}
	public HashMap<String, String> get_response(String name, double pricing) throws IOException, JSONException
	{
		//format spaces properly for the api search
		name = name.replace(" ", "%20");
		
		//list of useful preperties to retrieve from JSON
		HashMap<String, String> properties = new HashMap<String, String>();
		
		//Url to perform API call to ebay's finding service
		String url = "http://svcs.ebay.com/services/search/FindingService/v1?OPERATION-NAME=findItemsAdvanced&SERVICE-VERSION=1.12.0&SECURITY-APPNAME=VivekKan-Car-PRD-a79658d5d-28f5bcc7&RESPONSE-DATA-FORMAT=JSON&REST-PAYLOAD&paginationInput.entriesPerPage=2&keywords=" + name + "&categoryId=6001&MaxPrice=" + String.valueOf(pricing) + "&descriptionSearch=true&descriptionSearch=false&outputSelector=PictureURLLarge";
		
		System.out.println("Making API Call: " + url);
		
		//Create a URL Object from the string
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		int responseCode = con.getResponseCode();
		
		//Logging purposes: Print out response code (200 = good, 401 = invalid key)
		System.out.println("Response Code : " + responseCode);
		
		//RECIEVE JSON AND PARSE
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) 
		{
			response.append(inputLine);
		}
		in.close();

		// Pretty print to a file (just for debugging purposes, I find JSON so difficult to work with)
		try (FileWriter file = new FileWriter("/Users/vivekkandathil/Documents/file.txt")) 
		{
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonParser jp = new JsonParser();
			JsonElement je = jp.parse(response.toString());
			String prettyJsonString = gson.toJson(je);

			file.write(prettyJsonString);
			System.out.println("Successfully Copied JSON Object to File...");

			file.flush();
			file.close();

			// some properties aren't always there, catch missing tags and don't add
			try
			{
				// Try and see if I can do this in a more efficient way later on
				// Also learn more about JSON!
				JSONObject req = new JSONObject(response.toString());
				JSONArray locs = req.getJSONArray("findItemsAdvancedResponse");
				JSONObject rec = locs.getJSONObject(0);
				JSONArray a = rec.getJSONArray("searchResult");
				JSONObject rec2 = a.getJSONObject(0);
				JSONArray b = rec2.getJSONArray("item");
				JSONObject rec3 = b.getJSONObject(0);
				
				String title = rec3.getString("title");
				properties.put("car",title.substring(2, title.length() - 2));
				String itemID = rec3.getString("itemId");
				properties.put("itemID",itemID.substring(2, itemID.length() - 2));
				String id = rec3.getString("globalId");
				properties.put("id", id.substring(2, id.length() - 2));
				String imageURL = rec3.getString("pictureURLLarge");
				properties.put("imageURL",imageURL.substring(2, imageURL.length() - 2));
				String location = rec3.getString("location");
				properties.put("location",location.substring(2, location.length() - 2));
				String paymentMethod = rec3.getString("paymentMethod");
				properties.put("paymentMethod",paymentMethod.substring(2, paymentMethod.length() - 2));
				String itemURL = rec3.getString("viewItemURL");
				properties.put("itemURL",itemURL.substring(2, itemURL.length() - 2));
				
				JSONArray c = rec3.getJSONArray("sellingStatus");
				JSONObject rec4 = c.getJSONObject(0);
				JSONArray d = rec4.getJSONArray("currentPrice");
				JSONObject rec5 = d.getJSONObject(0);
				
				String price = rec5.getString("__value__");
				properties.put("price",price);
				
				
			}
			catch (JSONException e)
			{
				System.out.println("Tag missing or no search results found");
			}
			catch (NullPointerException e)
			{
				System.out.println("Tag missing or no search results found");
			}
			
			System.out.println(properties);
			
			return properties;
		}



	}
}