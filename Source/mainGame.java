/**
* @author Jack Gloyens 201164129 <jackgloyens@gmail.com>
* The class that's initially run and processes the views and input managers
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Math.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.lang.*;

/**
* Main game class that initialises and runs everything
*/
public class mainGame {

  public static int wWidth = 1024;
  public static int wHeight = 640;
  public static Renderer renderView = new Renderer("Ticket to Ride", wWidth, wHeight);

  public static Point mousePos;
  public static Point windowPos;

  public static myMouseListener ml = new myMouseListener();
  public static myKeyListener kl = new myKeyListener();

  public static Game game = new Game();

  public static Interface systemUI;

  public static GameScreen gameScreen = new GameScreen();
  public static JoinScreen joinScreen = new JoinScreen();
  public static ServerScreen serverScreen;

  public static ClientInstance client = new ClientInstance();

  public static String username;
  public static String colour;

  /**
  * Update the mousePos variable with the appropriate mouse coordinates and correct for the window boundaries
  */
  public static void updateMouse() {
    mousePos = MouseInfo.getPointerInfo().getLocation();
    windowPos = renderView.frame.getLocationOnScreen();
    mousePos.x -= windowPos.x + renderView.frame.getInsets().left;
    mousePos.y -= windowPos.y + renderView.frame.getInsets().top;
    mousePos.x -= renderView.renderer.xOffset;
    mousePos.y -= renderView.renderer.yOffset;
    mousePos.x /= renderView.renderer.scaleFactor;
    mousePos.y /= renderView.renderer.scaleFactor;
  }

  /**
  * Main method that initialises everything and starts the main game loop
  * @param args a list of arguments provided by the console, not used during execution
  */
  public static void main(String args[]) {

    gameScreen = new GameScreen();
    joinScreen = new JoinScreen();

    gameScreen.visible = false;
    joinScreen.visible = true;

    renderView.addChild(gameScreen);
    renderView.addChild(joinScreen);

    renderView.frame.addMouseListener(ml);
    renderView.renderer.addKeyListener(kl);

    while (true) {
      try{
				Thread.sleep(10);
			} catch(InterruptedException ex){
				Thread.currentThread().interrupt();
			}

      updateMouse();

      if (gameScreen.visible) {
        gameScreen.process();
      }
      if (joinScreen.visible) {
        joinScreen.process();
      }
      ml.process();
      kl.process();
      renderView.render();
    }
  }

}

/**
* Mouse listener class that checks which mouse buttons are being clicked or held and sets the appropriate flags
*/
class myMouseListener implements MouseListener {

  public boolean leftClick = false;
  public boolean rightClick = false;
  public boolean centerClick = false;
  public boolean leftHold = false;
  public boolean rightHold = false;
  public boolean centerHold = false;

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }

  /**
  * Override function for if one of the mouse buttons is pressed down
  * @param e the mouse event for the action that occurred
  */
  @Override
  public void mousePressed(MouseEvent e) {
    switch (e.getButton()) {
      case 1:
        leftClick = true;
        leftHold = true;
        break;
      case 2:
        centerClick = true;
        centerHold = true;
        break;
      case 3:
        rightClick = true;
        rightHold = true;
        break;
    }
  }

  /**
  * Override function for if one of the mouse buttons is released
  * @param e the mouse event for the action that occurred
  */
  @Override
  public void mouseReleased(MouseEvent e) {
    switch (e.getButton()) {
      case 1:
        leftHold = false;
        break;
      case 2:
        centerHold = false;
        break;
      case 3:
        rightHold = false;
        break;
    }
  }

  /**
  * Function to process the mouse listener for each tick of the main game loop
  */
  public void process() {
    if (leftClick) {
      leftClick = false;
    }
    if (centerClick) {
      centerClick = false;
    }
    if (rightClick) {
      rightClick = false;
    }
  }

}

/**
* Keyboard listener class that checks what keys from the keyboard are being pressed. Has special cases for backspace and enter keys
*/
class myKeyListener implements KeyListener {

  public String curChar = "";
  public boolean backSpace = false;
  public boolean enter = false;

  /**
  * Override function for if a key is pressed
  * @param e the key event for the action that occurred
  */
  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
      backSpace = true;
    }
    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
      enter = true;
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }

  /**
  * Override function for if a key is pressed and released
  * @param e the key event for the action that occurred
  */
  @Override
  public void keyTyped(KeyEvent e) {
    if (e.getKeyChar() != '\n' && (int)e.getKeyChar() != 8) {
      curChar = "" + e.getKeyChar();
    } else {
      curChar = "";
    }
  }

  /**
  * Function to process the keyboard listener for each tick of the main game loop
  */
  public void process() {
    curChar = "";
    backSpace = false;
    enter = false;
  }

}
