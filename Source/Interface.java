/**
* @author Jack Gloyens 201164129 <jackgloyens@gmail.com>
* The interface class that implements buttons and text inputs
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Math.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.lang.*;

public class Interface {
  public Interface() {

  }
}

/**
* The button class that extends the container
*/
class UIButton extends Container {

  public Image upTex;
  public Image downTex;
  public Point mousePos;
  public myMouseListener ml;
  public String text;
  public TextField t;

  /**
  * Default button constructor
  */
  public UIButton() {

  }

  /**
  * Button constructor
  * @param x the x coordinate of the button
  * @param y the y coordinate of the button
  * @param width the width of the button
  * @param height the height of the button
  * @param upTex the texture when the mouse isn't over the button
  * @param downTex the texture when the mouse is over the button
  */
  public UIButton(double x, double y, double width, double height, Image upTex, Image downTex) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.upTex = upTex;
    this.downTex = downTex;
    texture = upTex;
    visible = true;
  }

  /**
  * Button constructor
  * @param x the x coordinate of the button
  * @param y the y coordinate of the button
  * @param width the width of the button
  * @param height the height of the button
  * @param text the text to be rendered on top of the button
  */
  public UIButton(double x, double y, double width, double height, String text) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.upTex = Renderer.loadImage("assets/textures/segment1.png");
    this.downTex = Renderer.loadImage("assets/textures/segment2.png");
    this.text = text;
    texture = upTex;
    visible = true;
    t = new TextField(text, x + (width / 2), y + (height * 0.95));
    t.anchorX = 0.5;
    t.fontSize = (int)(height * 0.6);
    t.visible = true;
    addChild(t);
  }

  /**
  * Check if the mouse has clicked the button
  * @return True if the button was clicked, false if not
  */
  public boolean ifClicked() {
    if (visible) {
      ml = mainGame.ml;
      mousePos = mainGame.mousePos;
      if (mouseOver() && ml.leftClick) {
        return true;
      }
    }
    return false;
  }

  /**
  * The buttons process function, called each tick of the main game loop
  */
  public void process() {
    if (visible) {
      ml = mainGame.ml;
      mousePos = mainGame.mousePos;
      if (mouseOver()) {
        texture = downTex;
      } else {
        texture = upTex;
      }
    }
  }

}

/**
* Text input class that extends the container class, allows the user to input text via the keyboard
*/
class UITextInput extends Container {

  public String text = "";
  public Point mousePos;
  public myMouseListener ml;
  public myKeyListener kl;
  public int maxLength;
  public TextField t;
  public boolean focus;

  /**
  * Default text input constructor
  */
  public UITextInput() {

  }

  /**
  * Function to initialise the text input
  */
  public void setup() {
    t = new TextField("", x + (height * 0.8), y + (height * 0.7));
    t.fontSize = (int)(height * 0.6);
    t.visible = true;
    addChild(t);
    focus = false;
    visible = true;
  }

  /**
  * Text input constructor
  * @param x the x coordinate of the text input
  * @param y the y coordinate of the text input
  * @param width the width of the text input
  * @param height the height of the text input
  * @param texture the texture to be rendered
  */
  public UITextInput(double x, double y, double width, double height, Image texture) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.texture = texture;
    setup();
  }

  /**
  * Text input constructor
  * @param x the x coordinate of the text input
  * @param y the y coordinate of the text input
  * @param width the width of the text input
  * @param height the height of the text input
  * @param texture the texture to be rendered
  * @param maxLength the maximum number of characters the text input can contain
  */
  public UITextInput(double x, double y, double width, double height, Image texture, int maxLength) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.texture = texture;
    this.maxLength = maxLength;
    setup();
  }

  /**
  * Text input constructor
  * @param x the x coordinate of the text input
  * @param y the y coordinate of the text input
  * @param width the width of the text input
  * @param height the height of the text input
  */
  public UITextInput(double x, double y, double width, double height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.texture = Renderer.loadImage("assets/textures/segment1.png");
    setup();
  }

  /**
  * Text input constructor
  * @param x the x coordinate of the text input
  * @param y the y coordinate of the text input
  * @param width the width of the text input
  * @param height the height of the text input
  * @param maxLength the maximum number of characters the text input can contain
  */
  public UITextInput(double x, double y, double width, double height, int maxLength) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.texture = Renderer.loadImage("assets/textures/segment1.png");
    this.maxLength = maxLength;
    setup();
  }

  /**
  * Function to process the text input, called for each tick of the main game loop
  */
  public void process() {
    // If the text input container is visible
    if (visible) {
      ml = mainGame.ml;
      kl = mainGame.kl;
      mousePos = mainGame.mousePos;
      if (ml.leftClick) {
        if (mouseOver()) {
          focus = true;
        } else {
          focus = false;
        }
      }

      // If the user has clicked on the text input to focus it
      if (focus) {
        if (kl.backSpace && text.length() > 0) {
          text = text.substring(0, text.length() - 1);
        }
        if (text.length() < maxLength) {
          text += kl.curChar;
        }
        t.text = text + "_";
      } else {
        t.text = text;
      }

    }
  }

}
