/**
* @author Jack Gloyens 201164129 <jackgloyens@gmail.com>
* The renderer that has the container class that many objects in the game extend, it manages drawing objects to the screen correctly and maintaining the state of visible objects
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.*;
import javax.swing.*;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.*;

/**
* The renderer class that containes informtion about the current rendering environment, manages drawing objects to the current window
*/
public class Renderer extends JPanel {

  public String wName = "";
  public int wHeight = 0;
  public int wWidth = 0;
  public int rHeight = 0;
  public int rWidth = 0;
  public Container screen = new Container();
  public Renderer renderer;
  public JFrame frame;

  public static int xOffset = 0;
  public static int yOffset = 0;
  public static double scaleFactor = 1.0;

  public static int borderWidth = 0;
  public static int borderHeight = 0;

  /**
  * Renderer constructor
  * @param width the width (in pixels) of the rendering environment
  * @param height the height (in pixels) of the rendering environment
  */
  public Renderer(int width, int height) {
    wWidth = width;
    wHeight = height;
    rWidth = width;
    rHeight = height;
//    KeyListener listener = new MyKeyListener();
//		addKeyListener(listener);
    screen = new Container(0, 0, rWidth, rHeight, null, "screen");

    setFocusable(true);
  }

  /**
  * Renderer constructor
  * @param name the name identifier of the rendering environment
  * @param width the width (in pixels) of the rendering environment
  * @param height the height (in pixels) of the rendering environment
  */
  public Renderer(String name, int width, int height) {
    wName = name;
    wWidth = width;
    wHeight = height;
    rWidth = width;
    rHeight = height;
    renderer = new Renderer(width, height);
    frame = new JFrame(wName);
    frame.add(renderer);
    frame.pack();
    Renderer.borderWidth = frame.getInsets().left + frame.getInsets().right;
    Renderer.borderHeight = frame.getInsets().top + frame.getInsets().bottom;
    frame.setSize(wWidth + Renderer.borderWidth, wHeight + Renderer.borderHeight);
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    frame.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent evt) {
        Component c = (Component)evt.getSource();
        resizeWindow(renderer.getWidth(), renderer.getHeight());
      }
    });

  }

  /**
  * Resize the window to a new width and height, set the scale factor and vertical and horizontal offsets accordingly
  * @param newWidth the new width of the rendering environment
  * @param newHeight the new height of the rendering environment
  */
  public void resizeWindow(int newWidth, int newHeight) {
    renderer.wWidth = newWidth;
    renderer.wHeight = newHeight;
    if ((double)renderer.wWidth / (double)renderer.wHeight == (double)renderer.rWidth / (double)renderer.rHeight) {
      renderer.xOffset = 0;
      renderer.yOffset = 0;
      renderer.scaleFactor = (double)renderer.wWidth / (double)renderer.rWidth;
    } else if ((double)renderer.wWidth / (double)renderer.wHeight < (double)renderer.rWidth / (double)renderer.rHeight) {
      renderer.xOffset = 0;
      renderer.scaleFactor = (double)renderer.wWidth / (double)renderer.rWidth;
      renderer.yOffset = (int)(((double)renderer.wHeight - ((double)renderer.rHeight * renderer.scaleFactor)) / 2.0);
    } else {
      renderer.yOffset = 0;
      renderer.scaleFactor = (double)renderer.wHeight / (double)renderer.rHeight;
      renderer.xOffset = (int)(((double)renderer.wWidth - ((double)renderer.rWidth * renderer.scaleFactor)) / 2.0);
    }
  }

  /**
  * Load an image from the given filename
  * @param filename the directory and filename where the required image is stored
  * @return A usable image object of the given file
  */
  public static Image loadImage(String filename) {
    return Toolkit.getDefaultToolkit().getImage(filename);
  }

  /**
  * Draw the current rendering environment to the associated window
  */
  public void render() {
    renderer.repaint();
  }

  /**
  * Get the screen container (at the top of the container hierarchy) of the renderer
  * @return A container object of the screen
  */
  public Container getScreen() {
    return renderer.screen;
  }

  /**
  * Add a child container to the screen container
  * @param c the container to be added
  */
  public void addChild(Container c) {
    renderer.screen.addChild(c);
  }

  /**
  * Add multiple child containers to the screen container
  * @param cList a list of containers to be added
  */
  public void addChildren(ArrayList<Container> cList) {
    renderer.screen.addChildren(cList);
  }

  /**
  * Remove the child container at the given index in the child list
  * @param i the index of the container to be removed
  */
  public void removeChild(int i) {
    renderer.screen.children.remove(i);
  }

  /**
  * Remove the child container with the given name
  * @param fName the name identifier of the container to be removed
  */
  public void removeChild(String fName) {
    for (int i = 0; i < renderer.screen.children.size(); i++) {
      if (fName.equals(renderer.screen.children.get(i).name)) {
        removeChild(i);
        return;
      }
    }
  }

  /**
  * Remove a child container that matches the given container
  * @param c the container to compare the children containers to
  */
  public void removeChild(Container c) {
    for (int i = 0; i < renderer.screen.children.size(); i++) {
      if (renderer.screen.children.get(i).equals(c)) {
        removeChild(i);
        return;
      }
    }
  }

  /**
  * Render the current environment to the screen
  * @param g the graphics environment being used, provided by Java's 2D graphics framework
  */
  public void paint(Graphics g)
  {
    Graphics2D g2d = (Graphics2D) g;
    RenderingHints hints = new RenderingHints(
		    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
		);
		g2d.setRenderingHints(hints);
    g.setColor(new Color(0.0f, 0.0f, 0.0f));
    g.fillRect(0, 0, wWidth, wHeight);
    g.setColor(new Color(0.8f, 0.7f, 0.4f));
		g.fillRect(Renderer.xOffset, Renderer.yOffset, (int)(rWidth * scaleFactor), (int)(rHeight * scaleFactor));
    screen.draw(g, this);
    g.setColor(new Color(0.0f, 0.0f, 0.0f));
    g.fillRect(0, 0, Renderer.xOffset, wHeight);
    g.fillRect(wWidth - Renderer.xOffset, 0, Renderer.xOffset + (borderWidth / 2), wHeight);
    g.fillRect(0, 0, wWidth, Renderer.yOffset);
    g.fillRect(0, wHeight - Renderer.yOffset, wWidth, Renderer.yOffset + (borderWidth / 2));
  }

}

/**
* The container class that many objects extend, it contains variables for postion, rotation, achoring and texturing as well as a list of child containers that are connected to it
*/
class Container {

  public double x;
  public double y;
  public double width;
  public double height;
  public Image texture;
  public boolean visible;
  public String name;
  public double rotation;
  public double anchorX;
  public double anchorY;
  public double parentX = 0.0;
  public double parentY = 0.0;
  public Color colour = new Color(1.0f, 1.0f, 1.0f, 0.0f);

  public ArrayList<Container> children = new ArrayList<Container>();

  /**
  * Default container constructor
  */
  public Container() {
    x = 0.0;
    y = 0.0;
    width = 0.0;
    height = 0.0;
    visible = false;
    rotation = 0.0;
    anchorX = 0.0;
    anchorY = 0.0;
    name = "";
  }

  /**
  * Container constructor that deep copies a provided container that already exists
  * @param c the container to be copied
  */
  public Container(Container c) {
    this.x = c.x;
    this.y = c.y;
    this.width = c.width;
    this.height = c.height;
    this.name = c.name;
    this.texture = c.texture;
    this.rotation = c.rotation;
    this.visible = c.visible;
  }

  /**
  * Container constructor
  * @param x the x coordinate of the container
  * @param y the y coordinate of the container
  * @param width the width of the container
  * @param height the height of the container
  */
  public Container(double x, double y, double width, double height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    texture = null;
    rotation = 0.0;
    visible = true;
    name = "";
  }

  /**
  * Container constructor
  * @param x the x coordinate of the container
  * @param y the y coordinate of the container
  * @param width the width of the container
  * @param height the height of the container
  * @param texture the image object to texture the container with
  */
  public Container(double x, double y, double width, double height, Image texture) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.texture = texture;
    rotation = 0.0;
    visible = true;
    name = "";
  }

  /**
  * Container constructor
  * @param x the x coordinate of the container
  * @param y the y coordinate of the container
  * @param width the width of the container
  * @param height the height of the container
  * @param texture the image object to texture the container with
  * @param name a name identifier for the container
  */
  public Container(double x, double y, double width, double height, Image texture, String name) {
    this(x, y, width, height, texture);
    this.name = name;
  }

  /**
  * Container constructor
  * @param x the x coordinate of the container
  * @param y the y coordinate of the container
  * @param width the width of the container
  * @param height the height of the container
  * @param rotation the rotation around the anchor point (in radians) of the container
  * @param texture the image object to texture the container with
  */
  public Container(double x, double y, double width, double height, double rotation, Image texture) {
    this(x, y, width, height, texture);
    this.rotation = rotation;
    anchorX = 0.5;
    anchorY = 0.5;
  }

  /**
  * Container constructor
  * @param x the x coordinate of the container
  * @param y the y coordinate of the container
  * @param width the width of the container
  * @param height the height of the container
  * @param rotation the rotation around the anchor point (in radians) of the container
  * @param texture the image object to texture the container with
  * @param name a name identifier for the container
  */
  public Container(double x, double y, double width, double height, double rotation, Image texture, String name) {
    this(x, y, width, height, texture, name);
    this.rotation = rotation;
    anchorX = 0.5;
    anchorY = 0.5;
  }

  /**
  * Check if the mouse is contained within the container, accounts for anchoring and rotation
  * @return True if the mouse is inside the container, false if not
  */
  public boolean mouseOver() {
    double mx = mainGame.mousePos.x;
    double my = mainGame.mousePos.y;
    mx -= x;
    my -= y;
    double oldmx = mx;
    double r = -rotation;
    mx = (mx * Math.cos(r)) - (my * Math.sin(r));
    my = (oldmx * Math.sin(r)) + (my * Math.cos(r));
    mx += anchorX * width;
    my += anchorY * height;
    if (mx >= 0 && mx <= width) {
      if (my >= 0 && my <= height) {
        return true;
      }
    }
    return false;
  }

  /**
  * Check if the container is equivalent to another container object
  * @param obj the container obj to be compared to
  * @return True if they are equivalent, false if not
  */
  public boolean equals(Container obj) {
          return (this == obj);
  }

  /**
  * Add a child container
  * @param c the container to be added as a child
  */
  public void addChild(Container c) {
    c.parentX = x;
    c.parentY = y;
    children.add(c);
  }

  /**
  * Add a child container at the given index in the child list
  * @param c the container to be added as a child
  * @param i the index to add the container at
  */
  public void addChild(int i, Container c) {
    c.parentX = x;
    c.parentY = y;
    children.add(i, c);
  }

  /**
  * Add a list of child containers to the child list
  * @param cList a list of containers to be added as children
  */
  public void addChildren(ArrayList<Container> cList) {
    for (int i = 0; i < cList.size(); i++) {
      cList.get(i).parentX = x;
      cList.get(i).parentY = y;
      children.add(cList.get(i));
    }
  }

  /**
  * Remove the child container at the given index in the child list
  * @param i the index of the container to be removed
  */
  public void removeChild(int i) {
    children.remove(i);
  }

  /**
  * Remove the child container with the given name
  * @param fName the name identifier of the container to be removed
  */
  public void removeChild(String fName) {
    for (int i = 0; i < children.size(); i++) {
      if (fName.equals(children.get(i).name)) {
        removeChild(i);
        return;
      }
    }
  }

  /**
  * Remove a child container that matches the given container
  * @param c the container to compare the children containers to
  */
  public void removeChild(Container c) {
    for (int i = 0; i < children.size(); i++) {
      if (children.get(i).equals(c)) {
        removeChild(i);
        return;
      }
    }
  }

  /**
  * Move the container to the given coordinates and move all the containers children relative to the new position
  * @param x the new x coordinate of the container
  * @param y the new y coordinate of the container
  */
  public void moveTo(double x, double y) {
    double dx = x - this.x;
    double dy = y - this.y;
    this.x = x;
    this.y = y;
    for (int i = 0; i < children.size(); i++) {
      children.get(i).parentX = this.x;
      children.get(i).parentY = this.y;
      children.get(i).moveBy(dx, dy);
    }
  }

  /**
  * Move the container to the given coordinates relative to it's parents coordinates
  * @param x the new x coordinate of the container
  * @param y the new y coordinate of the container
  */
  public void moveToRelative(double x, double y) {
    this.x = x + parentX;
    this.y = y + parentY;
  }

  /**
  * Get the current position of the container
  * @return A two element array containing the x and y coordinate in that order
  */
  public double[] getPos() {
    double[] retval = {x, y};
    return retval;
  }

  /**
  * Move the container by the given amount and move all the containers children relative to the new position
  * @param x the distance to move on the x axis
  * @param y the distance to move on the y axis
  */
  public void moveBy(double x, double y) {
    this.x += x;
    this.y += y;
    for (int i = 0; i < children.size(); i++) {
      children.get(i).parentX = this.x;
      children.get(i).parentY = this.y;
      children.get(i).moveBy(x, y);
    }
  }

  /**
  * Scale the containers width and height by the given scale factor, also scale and move the containers children relative to the scale factor
  * @param sf the scale factor to scale the container by
  */
  public void scaleBy(double sf) {
//    double oldW = width;
//    double oldH = height;
    width *= sf;
    height *= sf;
    for (int i = 0; i < children.size(); i++) {
      Container c = children.get(i);
      c.x = x + ((c.x - x) * sf);
      c.y = y + ((c.y - x) * sf);
      c.scaleBy(sf);
    }
  }

  /**
  * Scale the container to the given width and scale the container's children relative to the new width
  * @param w the new width to scale the container to
  */
  public void scaleToWidth(double w) {
    double sf = w / width;
    scaleBy(sf);
  }

  /**
  * Scale the container to the given height and scale the container's children relative to the new height
  * @param h the new height to scale the container to
  */
  public void scaleToHeight(double h) {
    double sf = h / height;
    scaleBy(sf);
  }

  /**
  * Draw the container to the screen and then draw all it's children
  * @param g the graphics environment provided by Java's 2D graphics framework
  * @param r the rendering environment, provides information about the window
  */
  public void draw(Graphics g, Renderer r) {
    if (visible) {
      if (texture != null) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(0, 0);
    		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.rotate(rotation, r.xOffset + (x * r.scaleFactor), r.yOffset + (y * r.scaleFactor));
    		g2d.drawImage(texture, r.xOffset + (int)((x - (anchorX * width)) * r.scaleFactor), r.yOffset + (int)((y - (anchorX * height)) * r.scaleFactor), (int)(width * r.scaleFactor), (int)(height * r.scaleFactor), r);
        g2d.setColor(colour);
        g2d.fillRect(r.xOffset + (int)((x - (anchorX * width)) * r.scaleFactor), r.yOffset + (int)((y - (anchorX * height)) * r.scaleFactor), (int)(width * r.scaleFactor), (int)(height * r.scaleFactor));
        g2d.rotate(-rotation, r.xOffset + (x * r.scaleFactor), r.yOffset + (y * r.scaleFactor));
      }
      for (int i = 0; i < children.size(); i++) {
        children.get(i).draw(g, r);
      }
    }
  }

}

/**
* A basic text rendering class that extends the container class
*/
class TextField extends Container {

  String text;
  String font = "Courier";
  int fontSize = 24;

  /**
  * Text field contstructor
  * @param text the text to be rendered
  * @param x the x coordinate of the text
  * @param y the y coordinate of the text
  */
  public TextField (String text, double x, double y) {
    this.text = text;
    this.x = x;
    this.y = y;
    colour = new Color(0.0f, 0.0f, 0.0f, 1.0f);
    visible = true;
  }

  /**
  * Text field contstructor
  * @param text the text to be rendered
  * @param x the x coordinate of the text
  * @param y the y coordinate of the text
  * @param font the name of the font to be used
  */
  public TextField(String text, double x, double y, String font) {
    this(text, x, y);
    this.font = font;
  }

  /**
  * Text field contstructor
  * @param text the text to be rendered
  * @param x the x coordinate of the text
  * @param y the y coordinate of the text
  * @param font the name of the font family to be used
  * @param fontSize the size of the font
  */
  public TextField(String text, double x, double y, String font, int fontSize) {
    this(text, x, y, font);
    this.fontSize = fontSize;
  }

  /**
  * Text field contstructor
  * @param text the text to be rendered
  * @param x the x coordinate of the text
  * @param y the y coordinate of the text
  * @param fontSize the size of the font
  */
  public TextField(String text, double x, double y, int fontSize) {
    this(text, x, y);
    this.fontSize = fontSize;
  }

  /**
  * Draw the text to the screen
  * @param g the graphics environment provided by Java's 2D graphics framework
  * @param r the rendering environment, provides information on the window
  */
  public void draw(Graphics g, Renderer r) {
    drawString(g, r, text, x, y);
    for (int i = 0; i < children.size(); i++) {
      children.get(i).draw(g, r);
    }
  }

  /**
  * Draw the given string to the screen at the given coordinates
  * @param g the graphics environment provided by Java's 2D graphics framework
  * @param r the rendering environment, provides information on the window
  * @param text the text to be rendered
  * @param x the x coordinate of the text
  * @param y the y coordinate of the text
  */
  public void drawString(Graphics g, Renderer r, String text, double x, double y) {
    FontMetrics metrics = g.getFontMetrics(new Font(font, Font.PLAIN, (int)(fontSize)));
    g.setFont(new Font(font, Font.PLAIN, (int)(fontSize * r.scaleFactor)));
    g.setColor(colour);
    if (text.contains("\n")) {
      for (String line : text.split("\n")) {
        g.drawString(line, r.xOffset + (int)((x - (anchorX * metrics.stringWidth(line))) * r.scaleFactor), r.yOffset + (int)((y - (anchorX * metrics.getHeight())) * r.scaleFactor));
        y += g.getFontMetrics().getHeight() / r.scaleFactor;
      }
    } else {
      g.drawString(text, r.xOffset + (int)((x - (anchorX * metrics.stringWidth(text))) * r.scaleFactor), r.yOffset + (int)((y - (anchorX * metrics.getHeight())) * r.scaleFactor));
    }
  }

}
