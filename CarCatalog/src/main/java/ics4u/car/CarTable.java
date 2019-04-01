package ics4u.car;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;



public class CarTable extends Application {
 
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
 
    private final TableView<Car> tableView = new TableView<>();
 
    private final ObservableList<Car> dataList
            = FXCollections.observableArrayList();
    
    private Car car;
 
	@Override
    public void start(Stage primaryStage) 
	{
		//searchGoogle();
		
        primaryStage.setTitle("The Car Catalog!");
        
		//There will be a tab for selecting a car, and one for displaying it's properties.
		TabPane layout = new TabPane();
		Tab tA = new Tab("Main Menu");
		
		createMainMenu(tA);
		
		Tab tB = new Tab("Select a car");
		Tab tC = new Tab("Purchase");
 
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
        tableView.getColumns().addAll(
                columnF1, columnF2, columnF3);
        
        TextField filterField = new TextField();
        
        
        FilteredList<Car> filteredData = new FilteredList<>(dataList, p -> true);
        
        
        //**** SELECT A CAR FROM THE TABLE*****
        Button get = new Button("Get Car");
        
        //Event listener that gets a car object from the table cell that the User selects
        //For testing purposes I will be printing all the selected data in the console
        get.setOnAction((ActionEvent ae) -> 
        {
        	//Instantiates a car object based on the selected table value
        	car = tableView.getSelectionModel().getSelectedItem();
        	
        	//Use get property methods to print out car data
        	String modelName = car.getYear() + " " + car.getMake() + " " + car.getModel();
        	System.out.println("You selected a " + modelName);
        	
        	//Google images search
        	//Ebay search
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
        
        //\Add sorted (and filtered) data to the table.
        tableView.setItems(sortedData);
 
        //Vertical container to store the table
        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.getChildren().addAll(tableView, filterField, get);
 
        //Add to the group node
        root.getChildren().add(vBox);
        
        //Put all content into second tab
		tB.setContent(root);
		
		layout.getTabs().add(tA);
		layout.getTabs().add(tB);
		layout.getTabs().add(tC);
 
        primaryStage.setScene(new Scene(layout, 700, 850));
        primaryStage.show();
 
        readCSV();
        

    }
 
    private void readCSV() {
 
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
            Logger.getLogger(CarTable.class.getName()).log(Level.SEVERE, null, ex);
        }
        //General user check to catch other errors
        catch (IOException ex) 
        {
            Logger.getLogger(CarTable.class.getName()).log(Level.SEVERE, null, ex);
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
    	
    	//Exit button
    	Button exit = new Button("Exit");
    	exit.setOnAction(actionevent -> Platform.exit());
    	
    	//Container
    	VBox p = new VBox(instructions, exit);
    	p.setPadding(new Insets(40,40,40,40));
    	
    	t.setContent(p);
    }
 
    public static void main(String[] args) {
        launch(args);
    }
 
}