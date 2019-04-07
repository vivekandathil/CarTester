package ca.vivek.carsearcher;


import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;

import com.github.axet.vget.VGet;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;


//TODO:
//Amazon/ebay/kellybluebook API
// Add locatedin parameter

public class CarTable extends Application {
	
	//***** CLASS VARIABLES *****
    //API Key (expires in 7 days, I need to reactivate)
    final static String subscriptionKey = "a39875d05fe041daace25c8153bc4c46";

    //URI for API endpoint
    final static String host = "https://api.cognitive.microsoft.com";
    final static String path = "/bing/v7.0/images/search";
    static String searchTerm = "";
    static String videoSearch = "";
    static String modelName = "";
    static String colour = "";
    static String city = "", region = "", country = "", currency = "";
    static ArrayList<String> urlList = new ArrayList<>();
    static double maxPricing = 500000.00;
    
    static ProgressBar pb = new ProgressBar(0);
    static ImageView displayCar = new ImageView();
    static Label status;
    static TabPane layout;
    static Tab tE, tD, tC, tB;
    static ComboBox<String> history;
    static Stage stage;
    static CheckBox download = new CheckBox("Download Video Review");
    static CheckBox group, currencyConvert;

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
 
    //Other class variables that use the Car object
    private static final TableView<Car> tableView = new TableView<>();
    private static final ObservableList<Car> dataList = FXCollections.observableArrayList();
    private static Car car;
 
	@Override
    public void start(Stage primaryStage) throws IOException, JSONException 
	{
		this.stage = primaryStage;
		
        primaryStage.setTitle("The Car Catalog!");
        
		//There will be a tab for selecting a car, and one for displaying it's properties.
		layout = new TabPane();
		
		//**** FORMAT TABS ****
		layout.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		Tab tA = new Tab("Main Menu");
		Tab tB = new Tab("Select a car");
		tC = new Tab("Image");
		tD = new Tab("Car Reviews");
		tE = new Tab("Purchase");
		createMainMenu(tA);
		
		//This resets all components when the tab is entered
	    tB.setOnSelectionChanged(event -> {
	        if (tB.isSelected()) {
	        	pb.setProgress(0);
	        	status.setText("Awaiting selection\nPress button or double click cell once finished.\n\n"
	        			+ "COLOUR SELECTION\n(Optional, may not always return desired colour)");
	        }
	    });
	    
	    // LOCATE USER
	    displayLocation();
 
        Group root = new Group();
        
        pb.setStyle("-fx-accent: SpringGreen;");
 
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
        tableView.setStyle("    -fx-font-size: 10pt;\n" + "    -fx-font-family: \"Helvetica\";");
        
        tableView.setRowFactory( tv -> {
            TableRow<Car> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                	try {
						buttonAction();
					} catch (IOException | JSONException e) {
						e.printStackTrace();
					}
                }
            });
            return row ;
        });
        
        TextField filterField = new TextField();
        
        status = new Label("Awaiting selection\nPress button or double click cell once finished.\n\nCOLOUR SELECTION\n"
        		+ "(Optional, may not always return desired colour)");
        status.setStyle("    -fx-font-size: 10pt;\n" + "    -fx-font-family: \"Helvetica\";");
        
        FilteredList<Car> filteredData = new FilteredList<>(dataList, p -> true);
        
        //*** ALLOW USER TO FILTER PRICE FOR EBAY API SEARCH
        final Label max = new Label("Set A Max Price Range");
        final Slider maxPriceSlider = new Slider();
        maxPriceSlider.setMax(500000.00);
        maxPriceSlider.setValue(250000.00);
        
        maxPriceSlider.valueProperty().addListener(new ChangeListener<Object>() 
        {
            @Override
            public void changed(ObservableValue arg0, Object arg1, Object arg2) 
            {
            	maxPricing = maxPriceSlider.getValue();
            	max.textProperty().setValue(String.valueOf(NumberFormat.getCurrencyInstance(new Locale("en", "US")).format(maxPricing)));

            }
        });
        
        //**** SELECT A CAR FROM THE TABLE*****
        Button get = new Button("Get Car");
        get.setStyle("    -fx-font-size: 10pt;\n" + "    -fx-font-family: \"Helvetica\";");
        HBox startIt = new HBox(get, pb);
        HBox setIt = new HBox(maxPriceSlider, max);
        startIt.setSpacing(24);
        
        //Event listener that gets a car object from the table cell that the User selects
        //For testing purposes I will be printing all the selected data in the console
        get.setOnAction((ActionEvent ae) -> 
        {
        	try {
				buttonAction();
			} catch (IOException | JSONException e) {
				e.printStackTrace();
			}
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
        
        //**** SELECTING A COLOUR ****
        final String[][] palette = {
        		{"BLACK", "#000000"},
                {"BLUE", "#0000ff"},
                {"WHITE", "#ffffff"},
        		{"RED", "#dc143c"},
        		{"GREEN", "#008000"},
        		{"ORANGE", "#ffa500"},
        		{"PINK", "#ffc0cb"},
        		{"SILVER", "#c0c0c0"},
        		{"YELLOW", "#ffff00"}
        };
        
        Label suggestion = new Label("Can't find your car?\nSuggest one below:");
        suggestion.setStyle("    -fx-font-size: 10pt;\n" + "    -fx-font-family: \"Helvetica\";");
        
        history = new ComboBox<>();
        history.setPromptText("Search History");
	    history.setStyle("    -fx-text-fill        : #006464;\n" + 
	    		"    -fx-background-color : SpringGreen;\n" + 
	    		"    -fx-border-radius    : 20;\n" + 
	    		"    -fx-background-radius: 20;\n" + 
	    		"    -fx-font-family: \"Helvetica\";\n" + 
	    		"-fx-padding : 5;");
        
        //I imported a colour chooser object from github to allow the user to specify the car colour
        final ColorChooser colorChooser = new ColorChooser(palette);
        
        // monitor the color chooser's chosen color and respond to it.
        colorChooser.chosenColorProperty().addListener(new ChangeListener<Color>() {
        	@Override public void changed(ObservableValue<? extends Color> observableValue, Color oldColor, Color newColor) 
        	{
        		//Store colour string in colour variable, this will be used in the search query
        		colour = colorChooser.getChosenColorName();
        	}
        });
        
        // **** Ask user if he/she wants to filter by their region ****
        Label locationInformation = new Label("You are located in " + city + ", " + region + ", " + country + "\nFilter your ebay searches to nearby sellers?");
        locationInformation.setStyle("    -fx-font-size: 11pt;\n" + "    -fx-font-family: \"Helvetica\";");
        group = new CheckBox("Nearby Sellers Only");
        currencyConvert = new CheckBox("Convert Currency to " + currency + "?");
        group.setUserData(Color.LIGHTGREEN);
        group.setStyle("    -fx-font-size: 10pt;\n-fx-base: SpringGreen;");
        currencyConvert.setUserData(Color.LIGHTGREEN);
        currencyConvert.setStyle("    -fx-font-size: 10pt;\n-fx-base: SpringGreen;");
        VBox locationVbox = new VBox(locationInformation, group, currencyConvert);
        locationVbox.setSpacing(14);
 
        //Vertical container to store the table
        VBox vbox1 = new VBox(filterField, startIt, setIt, status, colorChooser, locationVbox);
        vbox1.setSpacing(10);
        VBox vbox2 = new VBox(tableView, suggestion, addACar(), history);
        vbox2.setSpacing(10);
        
        HBox vBox = new HBox();
        vBox.setPadding(new Insets(28,28,28,28));
        vBox.setSpacing(29);
        vBox.getChildren().addAll(vbox2, vbox1);
 
        //Add to the group node
        root.getChildren().add(vBox);
        
        //Put all content into second tab
		tB.setContent(root);
		
		layout.getTabs().addAll(tA, tB, tC, tD, tE);
 
        primaryStage.setScene(new Scene(layout, 700, 600));
        primaryStage.show();
 
        readCSV();
    }
	
	private void displayLocation() throws IOException, JSONException
	{
        // Find public IP address 
        String systemipaddress = "";
        
        try
        { 
            URL url_name = new URL("http://bot.whatismyipaddress.com"); 
  
            BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream())); 
  
            // reads system IPAddress 
            systemipaddress = sc.readLine().trim();
        } 
        catch (Exception e) 
        { 
            System.out.println("Cannot Execute Properly");
        }
        
        System.out.println("Your Public IP Address is: " + systemipaddress);
        
    	CityLookup locationfinder = new CityLookup();
    	locationfinder.findLocation(systemipaddress);
    	
    	// Set all the region variables
    	city = locationfinder.getCity();
    	country = locationfinder.getCountry();
    	region = locationfinder.getRegion();
    	currency = locationfinder.getCurrency();
	}
 
    private void readCSV() 
    {
        String CsvFile = "src/main/java/car.csv";
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
 
                Car record = new Car(fields[0], fields[1], fields[2]);
                
                //Add to the data list
                dataList.add(record);
            }
 
        } 
        //User check for missing csv
        catch (FileNotFoundException ex) 
        {
            Logger.getLogger(CarTable.class.getName()).log(Level.SEVERE, null, ex);
        }
        //General user check to catch other errors
        catch (IOException ex) 
        {
            Logger.getLogger(CarTable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void buttonAction() throws IOException, JSONException
    {
    	pb.setProgress(0);
    	
    	if (tableView.getSelectionModel().getSelectedItem() == null)
    	{
    		status.setText("No cars selected\n\n");
    	}
    	else
    	{
        	//Instantiates a car object based on the selected table value
        	car = tableView.getSelectionModel().getSelectedItem();
        	
        	//Use get property methods to print out car data
        	modelName = car.getYear() + " " + car.getMake() + " " + car.getModel();
        	
        	//Tell user that the search function is being called
        	status.setText(modelName + " selected\nSearching web for image...");
        	
        	history.getItems().add(modelName);
        	
        	//Include the colour strings in the search
        	searchTerm = colour + " " + modelName;
        	
        	// Exclude the colour name for the video search
        	videoSearch = modelName;
        	
        	String url = search(status);
        	
        	Image output = new Image(url);
        	
        	Button save = new Button("Save Image");
        	save.setOnAction(e -> saveImages(output));
        	
        	//**** FORMATTING IMAGE OUTPUT ****
        	displayCar = new ImageView(output);
            displayCar.setFitWidth(600);
            displayCar.setFitHeight(400);
            displayCar.setPreserveRatio(true);
            displayCar.setSmooth(true);
            displayCar.setCache(true);
        	
        	VBox img = new VBox(displayCar);
        	img.setStyle("-fx-border-color: black; -fx-border-width: 10;");
        	
        	//Call separate format output function and use returned vbox
        	VBox tab = formatOut(img, save);
            
            tC.setContent(tab);
            
            try
            {
            	getVideo();
            }
            catch (org.jsoup.HttpStatusException e)
            {
            	System.out.println("403 error for youtube api, skipping");
            }
            
            
        	System.out.println("You selected a " + modelName + "\n--------------\n");
        	
        	status.setText("Success! Switching to next tab....\n\n");
        	
        	// Put current thread to sleep to delay the switch to the image tab
        	// This will make the switch less sudden and allow for a warning message
            Task<Void> sleeper = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                    	//Put current thread to sleep for one second
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    return null;
                }
            };
            sleeper.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) 
                {
                	layout.getSelectionModel().select(tC);
                }
            });
            new Thread(sleeper).start();
    	}
    	
    	searchEbay();
    }
    
    private static void getVideo() throws IOException, JSONException
    {
    	//Search for a car review using car name and year
		String keyword = videoSearch + " review";
		
		System.out.println("Searching youtube for " + keyword);
		
		//Ensure search string is valid without spaces
		keyword = keyword.replace(" ", "+");
		 
		// The url for the API call
		String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=1&order=rating&q=" + keyword + "&key=AIzaSyAL0xSUWCnUTe6HWjx72AfoAwsTB5AF6i4";
		 
		//Get JSON object
		Document doc = Jsoup.connect(url).timeout(10 * 1000).ignoreContentType(true).get();
		String getJson = doc.text();
		JSONObject jsonObject = (JSONObject) new JSONTokener(getJson ).nextValue();
		 
		//Parse JSON and retrieve the video ID (this is appended to the youtube url)
		String videoID = (String) ((JSONObject) ((JSONObject) jsonObject.getJSONArray("items").get(0)).get("id")).get("videoId");
		String videoName = (String) ((JSONObject) ((JSONObject) jsonObject.getJSONArray("items").get(0)).get("snippet")).get("title");
		String videoURL = "https://www.youtube.com/watch?v=" + videoID;
		System.out.println("Video found at " + videoURL);
		System.out.println(videoName + "\n-----------");
		
		if (download.isSelected())
		{
	        try {
	            String path = "/Users/vivekkandathil/Documents/";
	            VGet v = new VGet(new URL(videoURL), new File(path));
	            v.download();   
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        }
		}
		else
		{
		    WebView webview = new WebView();
		    webview.getEngine().load(
		      videoURL
		    );
		    
		    tD.setContent(webview);
		}
    }
    
    private static VBox formatOut(VBox image, Button save)
    {
    	VBox output = new VBox();
    	output.setPadding(new Insets(20,20,20,20));
    	output.setSpacing(10);
    	
    	Label title = new Label(searchTerm);
    	title.setStyle("    -fx-font-size: 29pt;\n" + "    -fx-font-family: \"Helvetica\";");
    	
    	Button startAgain = new Button("Search for a new Car");
    	startAgain.setOnAction((ActionEvent ae) -> {
    		layout.getSelectionModel().select(tB);
    	});
    	
    	HBox components = new HBox(startAgain, save);
    	components.setPadding(new Insets(20,20,20,20));
    	components.setSpacing(10);
    	
    	output.getChildren().addAll(title, image, components);
    	
    	return output;
    }
    
    //Car suggestion
    private static HBox addACar()
    {
        final HBox hBox = new HBox();
        hBox.setSpacing(5);

        final TextField yearTextField = new TextField("year");
        yearTextField.setPrefWidth(80);
        final TextField makeTextField = new TextField("make");
        makeTextField.setPrefWidth(80);
        final TextField modelTextField = new TextField("model");
        modelTextField.setPrefWidth(80);
        
        Button saveButton = new Button("ADD");

        saveButton.setOnAction((ActionEvent ae) -> {
        	
        	//User check
        	if (yearTextField.getText().equals(null) || makeTextField.getText().equals(null) || modelTextField.getText().equals(null))
        	{
        		status.setText("Error: One of the fields are null");
        	}
        	else
        	{
            	//Concatenate a comma separated string to add to the suggestions file
            	String car = yearTextField.getText() + "," + makeTextField.getText() + "," + modelTextField.getText() + ",";
            	
            	//Include a user check before writing to file
            	try {
    				save(car);
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
        	}
        });

        hBox.getChildren().addAll(saveButton, yearTextField, makeTextField, modelTextField);
        
        return hBox;
    }
    
    //Add the suggested car to the suggestions text file
    private static void save(String car) throws IOException {	 
    	Files.write(Paths.get("src/main/java/suggestions.txt"), car.getBytes(), StandardOpenOption.APPEND);
    }
    
    //This is used to save the output image
    private static void saveImages(Image image) {
    	
    	//replace the spaces with underscores to create a proper name
    	String filename = searchTerm.replaceAll(" ", "_");
        
        DirectoryChooser directoryChooser = new DirectoryChooser();
        
        File selectedDirectory = directoryChooser.showDialog(stage);

        if(selectedDirectory == null)
        {
             //No Directory selected
        }else
        {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "jpg", new File(selectedDirectory.getAbsolutePath() + "/" + filename + ".jpg"));
                System.out.println("Saved successfully!");
            } catch (IOException e) {
            }
        }
    }
    
    // Creates and adds content for main menu
    public static void createMainMenu(Tab t)
    {
    	//Label for instructions
    	Label instructions = new Label();
    	instructions.setText("Welcome to the car searcher!\nThis program will allow you to search through an expansive "
    			+ "list of cars and find a car make\nand model of your choice. Go to the next tab to proceed. You will find a "
    			+ "table that lists all\nof the cars in the database. Use the search filter below the table to find a car. "
    			+ "\n\nSelect the cell, a colour (optional) and price range. Double click on the car in the table or\nclick on the Get Car Button to view an image of your car.\nAdditionally, the program will"
    			+ "automatically generate a youtube review for your car\n(Warning: it may not always show the right results)\n"
    			+ "Finally, the last tab will load information on your selected car if it can be found on ebay.\nPricing information, location"
    			+ ", and payment methods will be displayed, along with a button\nfor you to view the car on ebay.\n\n(Note: I am basing the car data off of a "
    			+ "CSV file that I retrieved from the internet)\n\n");
        instructions.setStyle("    -fx-font-size: 11pt;\n" + 
        		"    -fx-font-family: \"Helvetica\";");
    	
    	//Exit button
    	Button exit = new Button("Exit");
        exit.setStyle("    -fx-font-size: 11pt;\n" + 
        		"    -fx-font-family: \"Helvetica\";");
    	exit.setOnAction(actionevent -> {
    		Alert alert = new Alert(AlertType.CONFIRMATION, "Exit?", ButtonType.YES, ButtonType.CANCEL);
    		alert.showAndWait();

    		if (alert.getResult() == ButtonType.YES) {
    		    Platform.exit();
    		}
    	});
    	
    	Button proceed = new Button("Start!");
        proceed.setStyle("    -fx-font-size: 11pt;\n" + 
        		"    -fx-font-family: \"Helvetica\";");
    	proceed.setOnAction((ActionEvent a) -> {
    		layout.getSelectionModel().select(tB);
    	});
    	
    	//Container
    	VBox p = new VBox(instructions, proceed, exit);
    	p.setPadding(new Insets(40,40,40,40));
    	
    	t.setContent(p);
    }
    
    public static void searchEbay() throws IOException, JSONException
    {
    	//I created a separate class to use the ebay product finding API
    	EbaySearcher ebaySearcher = new EbaySearcher();
    	Map<String, String> results = ebaySearcher.get_response(videoSearch, maxPricing);
    	
    	
    	Label l1 = new Label(""),l2 = new Label(""),l3 = new Label("");
    	
    	try
    	{
    	
    		l1 = new Label((results.get("car") == null) ? "No cars found on ebay\n" : "Your car was found on eBay!\n");
    		l1.setStyle("    -fx-font-size: 29pt;\n-fx-font-family: \"Helvetica\";");

    		l2 = new Label(results.get("car") + "\nDepartment: " + results.get("id") + "\nLocated in " + results.get("location"));
    		l2.setStyle("    -fx-font-size: 16pt;\n-fx-font-family: \"Helvetica\";");

    		l3 = new Label("Current Price: " + NumberFormat.getCurrencyInstance(new Locale("en", "US")).format(Double.parseDouble(results.get("price"))));
    		l3.setStyle("    -fx-font-size: 29pt;\n-fx-font-family: \"Helvetica\";");
    	}
    	catch (NullPointerException e)
    	{
    		System.out.println("Tags not available");
    	}

    	
	    WebView webview = new WebView();
	    webview.getEngine().load(
	      results.get("imageURL")
	    );
	    
	    ObservableList<String> p = FXCollections.observableArrayList(results.get("paymentMethod").replaceAll("\"", "").split(","));
	    
	    ComboBox<String> paymentMethods = new ComboBox<>(p);
	    paymentMethods.setPromptText("Payment Methods");
	    paymentMethods.setStyle("    -fx-text-fill        : #006464;\n" + 
	    		"    -fx-background-color : SpringGreen;\n" + 
	    		"    -fx-border-radius    : 20;\n" + 
	    		"    -fx-background-radius: 20;\n" + 
	    		"    -fx-font-family: \"Helvetica\";\n" + 
	    		"-fx-padding : 5;");
	    
	    Button searchEbay = new Button("View on Ebay.com");
	    searchEbay.setStyle("    -fx-text-fill    : black;\n" + 
	    		"    -fx-background-color : white;\n" + 
	    		"    -fx-border-color : green;\n" + 
	    		"    -fx-border-radius: 5;\n" + 
	    		"    -fx-font-family: \"Helvetica\";\n" +
	    		"-fx-padding : 3 6 6 6;");
	    
	    searchEbay.setOnAction((ActionEvent ae) -> {
	        try {
	            Desktop.getDesktop().browse(new URL(results.get("itemURL")).toURI());
	        } 
	        // Catch io exception
	        catch (IOException e) {
	            e.printStackTrace();
	        }
	        //User check for url format
	        catch (URISyntaxException e) {
	            e.printStackTrace();
	        }
	    });
    	
    	HBox nameLocation = new HBox(webview);
    	nameLocation.setPrefWidth(200);
    	nameLocation.setPrefHeight(200);
    	
    	VBox vbox = new VBox(l1, nameLocation, l2, l3, paymentMethods, searchEbay);
    	vbox.setPadding(new Insets(10,10,20,20));
    	vbox.setSpacing(10);
    	
    	tE.setContent(vbox);
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
            System.out.println("Searching the Web for: " + searchTerm + "...");
            SearchResults result = SearchImages(searchTerm);
            System.out.println("Recieved successful response. Retrieving image...");
            
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
            
            System.out.println("Total of " + total + " images were found. Retrieving first result\n");
            status.setText("Success!\nGo to the next tab");
            
        }
        catch (Exception e) 
        {
            e.printStackTrace(System.out);
            System.exit(1);
        }
        
        return resultURL;  
    }  
    
    public static void main(String[] args) {
        launch(args);
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