package ics4u.car;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;



public class CarTable extends Application {
	
	//***** CLASS VARIABLES *****
    //API Key (expires in 7 days, I need to reactivate)
    final static String subscriptionKey = "a766bc08387c465daf5a479bcbd72bb9";

    //URI for API endpoint
    final static String host = "https://api.cognitive.microsoft.com";
    final static String path = "/bing/v5.0/images/search";
    static String searchTerm = "";
    static String modelName = "";
    
    static ProgressBar pb = new ProgressBar(0);
    static ImageView displayCar = new ImageView();
    static Label status;
    static Tab tC;

    //I got this function from the Bing API documentation
    private static SearchResults SearchImages (String searchQuery) throws Exception 
    {
        //construct URL of search request (endpoint + query string)
        URL url = new URL(host + path + "?q=" +  URLEncoder.encode(searchQuery, "UTF-8"));
        HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
        
        //pass in the API key from the string (I need to refresh this every 7 days)
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);

        //receive JSON body
        InputStream stream = connection.getInputStream();
        String response = new Scanner(stream).useDelimiter("\\A").next();

        //construct result object for return
        SearchResults results = new SearchResults(new HashMap<String, String>(), response);

        //extract Bing-related HTTP headers
        Map<String, List<String>> headers = connection.getHeaderFields();
        
        for (String header : headers.keySet()) 
        {
            if (header == null) continue; //may have null key
            
            if (header.startsWith("BingAPIs-") || header.startsWith("X-MSEdge-")) 
            {
                results.relevantHeaders.put(header, headers.get(header).get(0));
            }
        }

        stream.close();
        return results;
    }
 
	//I decided to just put the class within my tester class to make it easier to read while coding
    public class Car 
    {
 
        private SimpleStringProperty year, make, model;
 
        public String getYear() {
            return year.get();
        }
 
        public String getMake() {
            return make.get();
        }
 
        public String getModel() {
            return model.get();
        }
 
        Car(String f1, String f2, String f3) 
        {
            this.year = new SimpleStringProperty(f1);
            this.make = new SimpleStringProperty(f2);
            this.model = new SimpleStringProperty(f3);
        }
 
    }
 
    private static final TableView<Car> tableView = new TableView<>();
 
    private static final ObservableList<Car> dataList
            = FXCollections.observableArrayList();
    
    private static Car car;
 
	@Override
    public void start(Stage primaryStage) 
	{
        primaryStage.setTitle("The Car Catalog!");
        
		//There will be a tab for selecting a car, and one for displaying it's properties.
		TabPane layout = new TabPane();
		Tab tA = new Tab("Main Menu");
		
		createMainMenu(tA);
		
		Tab tB = new Tab("Select a car");
		tC = new Tab("Purchase");
 
        Group root = new Group();
 
        TableColumn columnF1 = new TableColumn("Year");
        columnF1.setCellValueFactory(
                new PropertyValueFactory<>("year"));
 
        TableColumn columnF2 = new TableColumn("Make");
        columnF2.setCellValueFactory(
                new PropertyValueFactory<>("make"));
 
        TableColumn columnF3 = new TableColumn("Model");
        columnF3.setCellValueFactory(
                new PropertyValueFactory<>("model"));
 
        tableView.setItems(dataList);
        tableView.getColumns().addAll(columnF1, columnF2, columnF3);
        
        tableView.setRowFactory( tv -> {
            TableRow<Car> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                	buttonAction();
                }
            });
            return row ;
        });
        
        TextField filterField = new TextField();
        
        status = new Label("Awaiting selection\nPress button once finished.");
        
        FilteredList<Car> filteredData = new FilteredList<>(dataList, p -> true);
        
        
        //**** SELECT A CAR FROM THE TABLE*****
        Button get = new Button("Get Car");
        
        //Event listener that gets a car object from the table cell that the User selects
        //For testing purposes I will be printing all the selected data in the console
        get.setOnAction((ActionEvent ae) -> 
        {
        	buttonAction();
        });
        
        //****** FILTERING TEXT IN THE TABLE VIEW ******
        filterField.textProperty().addListener((observable, oldValue, newValue) -> 
        {
        	//The set predicate function will be used to filter the table
        	//based on the value of the search field
            filteredData.setPredicate(car -> 
            {
                //User check: If filter text is empty, display all cars.
                if (newValue == null || newValue.isEmpty()) 
                {
                    return true;
                }
                
                //Compare the car's make and model of every car with filter text.
                //set all the characters to lower case so that it is not case sensitive
                String lowerCaseFilter = newValue.toLowerCase();
                
                //Check for matching car values
                if (car.getMake().toLowerCase().contains(lowerCaseFilter))
                {
                    return true; //Filter matches car make.
                } 
                else if (car.getModel().toLowerCase().contains(lowerCaseFilter)) 
                {
                    return true; //Filter matches car model.
                }
                return false; //Does not match.
            });
        });
        
        //Wrap the filtered list in a sorted list
        SortedList<Car> sortedData = new SortedList<>(filteredData);
        
        //(I took this next part from Stack Overflow)
        //Bind the SortedList comparator to the TableView comparator. 
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        
        //Add sorted (and filtered) data to the table.
        tableView.setItems(sortedData);
 
        //Vertical container to store the table
        VBox hbox = new VBox(filterField, get, pb, status);
        hbox.setSpacing(10);
        
        HBox vBox = new HBox();
        vBox.setPadding(new Insets(28,28,40,48));
        vBox.setSpacing(10);
        vBox.getChildren().addAll(tableView, hbox);
 
        //Add to the group node
        root.getChildren().add(vBox);
        
        //Put all content into second tab
		tB.setContent(root);
		
		layout.getTabs().addAll(tA, tB, tC);
 
        primaryStage.setScene(new Scene(layout, 600, 500));
        primaryStage.show();
 
        readCSV();
    }
 
    private void readCSV() 
    {
        String CsvFile = "/Users/vivekkandathil/Documents/car.csv";
        String FieldDelimiter = ",";
        BufferedReader br;
 
        try 
        {
        	//Buffered reader to output data stream
            br = new BufferedReader(new FileReader(CsvFile));

            String line;
            
            //read all lines of the CSV and split into arrays for the table cell values
            while ((line = br.readLine()) != null) 
            {
                String[] fields = line.split(FieldDelimiter, -1);
 
                Car record = new Car(fields[0], fields[1], fields[2]); //For later: try and replace the extra crap in the fourth field
                
                //Add to the data list
                dataList.add(record);
            }
 
        } 
        //User check for missing csv
        catch (FileNotFoundException ex) 
        {
            Logger.getLogger(CarTester.class.getName()).log(Level.SEVERE, null, ex);
        }
        //General user check to catch other errors
        catch (IOException ex) 
        {
            Logger.getLogger(CarTester.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void buttonAction()
    {
    	pb.setProgress(0);
    	
    	if (tableView.getSelectionModel().getSelectedItem() == null)
    	{
    		status.setText("No cars selected");
    	}
    	else
    	{
        	//Instantiates a car object based on the selected table value
        	car = tableView.getSelectionModel().getSelectedItem();
        	
        	//Use get property methods to print out car data
        	modelName = car.getYear() + " " + car.getMake() + " " + car.getModel();
        	
        	//Tell user that the search function is being called
        	status.setText(modelName + " selected\nSearching web for image...");
        	
        	searchTerm = modelName;
        	
        	String url = search(status);
        	
        	displayCar = new ImageView(new Image(url));
        	Label stats = new Label(modelName + "\n");
        	
        	VBox img = new VBox(displayCar, stats);
            
            tC.setContent(img);
            
        	System.out.println("You selected a " + modelName);
    	}
    }
    
    public static void createMainMenu(Tab t)
    {
    	//Label for instructions
    	Label instructions = new Label();
    	instructions.setText("Welcome to Vivek's car searcher!\nThis program will allow you to search through an expansive "
    			+ "list of cars and find a car make/model\nof your choice. Go to the next tab to proceed. You will find a "
    			+ "table that lists all of the cars in the\ndatabase. Use the search filter below the table to find a car. "
    			+ "Select the cell and click on the\nGet Car Button to get your car!\n\n(Note: I am basing the car data off of a "
    			+ "CSV file that I retrieved from the internet)\n\n");
        instructions.setStyle("    -fx-font-size: 11pt;\n" + 
        		"    -fx-font-family: \"Helvetica\";");
    	
    	//Exit button
    	Button exit = new Button("Exit");
        exit.setStyle("    -fx-font-size: 11pt;\n" + 
        		"    -fx-font-family: \"Helvetica\";");
    	exit.setOnAction(actionevent -> Platform.exit());
    	
    	//Container
    	VBox p = new VBox(instructions, exit);
    	p.setPadding(new Insets(40,40,40,40));
    	
    	t.setContent(p);
    }
 
    public static void main(String[] args) {
        launch(args);
    }
    
    public static String search(Label status)
    {
    	String resultURL = "";
    	
        if (subscriptionKey.length() != 32) 
        {
            System.out.println("Invalid Bing Search API subscription key!");
            System.out.println("Please paste yours into the source code.");
            System.exit(1);
        }

        try 
        {	
            status.setText("Searching the Web for: " + searchTerm + "...");
            SearchResults result = SearchImages(searchTerm);
            status.setText("Recieved successful response. Retrieving image...");
            
            //According to the documentation Bing API will return the image data in the form of a JSON data structure
            //A JSON parser will be used to parse the data and retrieve the url of the first image returned
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(result.jsonResponse).getAsJsonObject();
            
            //get the first image result from the JSON object, along with the total 
            //number of images returned by the Bing Image Search API. 
            String total = json.get("totalEstimatedMatches").getAsString();
            JsonArray results = json.getAsJsonArray("value");
            JsonObject first_result = (JsonObject)results.get(0);
            resultURL = first_result.get("thumbnailUrl").getAsString();

            Timeline timeline = new Timeline();

            KeyValue keyValue = new KeyValue(pb.progressProperty(), 1.00);
            KeyFrame keyFrame = new KeyFrame(new Duration(480), keyValue);
            timeline.getKeyFrames().add(keyFrame);

            timeline.play();
            
            status.setText("Total of " + total + " images were found. Retrieving first result\n");
            status.setText("Success!\nGo to the next tab");
            
        }
        catch (Exception e) 
        {
            e.printStackTrace(System.out);
            System.exit(1);
        }
        
        return resultURL;  
    }  
 
}

//Container class for search results encapsulates relevant headers and JSON data
class SearchResults
{
 HashMap<String, String> relevantHeaders;
 String jsonResponse;
 
 SearchResults(HashMap<String, String> headers, String json) 
 {
     relevantHeaders = headers;
     jsonResponse = json;
 }
}
