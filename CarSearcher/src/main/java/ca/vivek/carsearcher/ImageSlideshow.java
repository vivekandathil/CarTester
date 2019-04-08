package ca.vivek.carsearcher;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ImageSlideshow 
{
	public ImageSlideshow()
	{
		
	}
	
	int index = 0;
	double orgCliskSceneX, orgReleaseSceneX;
	Button lbutton, rButton;
	ImageView imageView;

	public GridPane create(List<Image> images, String tag) {
		GridPane root = new GridPane();
		
		try {
			root.setAlignment(Pos.CENTER);

			lbutton = new Button("<");
			rButton = new Button(">");
			imageView = new ImageView(images.get(index));
			imageView.setCursor(Cursor.CLOSED_HAND);

			imageView.setOnMousePressed(circleOnMousePressedEventHandler);

			imageView.setOnMouseReleased(e -> {
				orgReleaseSceneX = e.getSceneX();
				if (orgCliskSceneX > orgReleaseSceneX) {
					lbutton.fire();
				} else {
					rButton.fire();
				}
			});

			rButton.setOnAction(e -> {
				index = index + 1;
				if (index == images.size()) {
					index = 0;
				}
				imageView.setImage(images.get(index));

			});
			lbutton.setOnAction(e -> {
				index = index - 1;
				if (index == 0 || index > images.size() + 1 || index == -1) {
					index = images.size() - 1;
				}
				imageView.setImage(images.get(index));

			});
			
	        imageView.setFitHeight(500);
	        imageView.setFitWidth(500);
	        
	        Label a = new Label("	TOP INSTAGRAM POSTS FOR " + tag);
	        a.setStyle("    -fx-font-size: 16pt;\n-fx-font-family: \"Helvetica\";");

			HBox hBox = new HBox();
			hBox.setSpacing(15);
			hBox.setAlignment(Pos.CENTER);
			hBox.getChildren().addAll(lbutton, imageView, rButton);
			
	        VBox vBox = new VBox(a, hBox);

			root.add(vBox, 1, 1);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return root;
	}

	EventHandler<MouseEvent> circleOnMousePressedEventHandler = new EventHandler<MouseEvent>() {

		@Override
		public void handle(MouseEvent t) {
			orgCliskSceneX = t.getSceneX();
		}
	};
}
