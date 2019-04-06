package ics4u.car;

import javafx.beans.property.SimpleStringProperty;

//I decided to just put a separate class for the car to make it easier to read while coding
public class Car 
{
    private SimpleStringProperty year, make, model, price, url, ebayName, imageUrl;

    public String getYear() {
        return year.get();
    }

    public String getMake() {
        return make.get();
    }

    public String getModel() {
        return model.get();
    }
    
    public String getPrice() {
        return price.get();
    }

    public String getUrl() {
        return url.get();
    }
    
    public String getSale() {
        return ebayName.get();
    }
    
    public String getImg() {
        return imageUrl.get();
    }

    //constructor for the table
    Car(String f1, String f2, String f3) 
    {
        this.year = new SimpleStringProperty(f1);
        this.make = new SimpleStringProperty(f2);
        this.model = new SimpleStringProperty(f3);
    }
    
    Car(String p, String u, String e, String i)
    {
        this.price = new SimpleStringProperty(p);
        this.url = new SimpleStringProperty(u);
        this.ebayName = new SimpleStringProperty(e);
        this.imageUrl = new SimpleStringProperty(i);
    }
}
