/**
* @author Jack Gloyens 201164129 <jackgloyens@gmail.com>
* The screen container used for the client side join game screen
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
* Main join screen class that extends the container class
*/
public class JoinScreen extends Container {

  public Game game;
  public Renderer renderView;
  public Point mousePos;
  public myMouseListener ml;

  public Container findServer;
  public Container inGame;
  public UIButton btnJoinServer;
  public UITextInput txtServerIP;
  public UITextInput txtPassword;
  public UITextInput txtUsername;
  public UIButton btnUpdateInfo;
  public TextField lblJoinServer;
  public TextField lblAddress;
  public TextField lblPassword;
  public TextField lblUsername;
  public TextField lblClientList;

  /**
  * Default join screen constructor
  */
  public JoinScreen() {
    game = mainGame.game;
    renderView = mainGame.renderView;

    // Create interface
    findServer = new Container(0, 0, renderView.wWidth, renderView.wHeight);
    inGame = new Container(0, 0, renderView.wWidth, renderView.wHeight);

    btnJoinServer = new UIButton((renderView.wWidth / 2) - 160, 500, 320, 64, "Join Server");
    txtServerIP = new UITextInput((renderView.wWidth / 2) - 200, 180, 640, 64, 20);
    txtPassword = new UITextInput((renderView.wWidth / 2) - 200, 240, 640, 64, 20);
    findServer.addChild(btnJoinServer);
    findServer.addChild(txtServerIP);
    findServer.addChild(txtPassword);
    lblJoinServer = new TextField("Join a game", (renderView.wWidth / 2), 128);
    lblAddress = new TextField("IP address: ", (renderView.wWidth / 2) - 160, 260);
    lblPassword = new TextField("Password: ", (renderView.wWidth / 2) - 160, 320);
    lblJoinServer.anchorX = 0.5;
    lblJoinServer.fontSize = 40;
    lblAddress.fontSize = 40;
    lblPassword.fontSize = 40;
    lblAddress.anchorX = 1.0;
    lblPassword.anchorX = 1.0;
    findServer.addChild(lblJoinServer);
    findServer.addChild(lblAddress);
    findServer.addChild(lblPassword);
    findServer.visible = true;
    inGame.visible = false;

    lblUsername = new TextField("Enter a username:", (renderView.wWidth / 2), 132);
    txtUsername = new UITextInput(32, 164, 640, 64, 20);
    btnUpdateInfo = new UIButton(renderView.wWidth - 320 - 32, 164, 320, 64, "Update");
    lblClientList = new TextField("Players:", (renderView.wWidth / 2), 320);
    lblUsername.anchorX = 0.5;
    lblUsername.fontSize = 40;
    lblClientList.anchorX = 0.5;
    lblClientList.fontSize = 40;
    inGame.addChild(lblUsername);
    inGame.addChild(txtUsername);
    inGame.addChild(btnUpdateInfo);
    inGame.addChild(lblClientList);

    addChild(findServer);
    addChild(inGame);
  }

  /**
  * Function to process the join screen, called for each tick of the main game loop
  */
  public void process() {
    if (visible) {

      ml = mainGame.ml;
      mousePos = mainGame.mousePos;

      // Process interface
      if (findServer.visible) {
        btnJoinServer.process();
        txtServerIP.process();
        txtPassword.process();

        if (btnJoinServer.ifClicked()) {
          mainGame.client.connect(txtServerIP.text, txtPassword.text);
        }

        if (mainGame.client.isServerConnected) {
          findServer.visible = false;
          inGame.visible = true;
          mainGame.client.run();
        }
      }

      if (inGame.visible) {
        btnUpdateInfo.process();
        txtUsername.process();

        // If the update button is clicked then send the new username to the server
        if (btnUpdateInfo.ifClicked()) {
          mainGame.client.username = txtUsername.text;
//          mainGame.client.gameManager.pushUpdate("changeUsername;colour(" + mainGame.client.colour + ")username(" + mainGame.client.username + ")");
          mainGame.client.sendServerMessage("changeName;colour(" + mainGame.client.colour + ")username(" + mainGame.client.username + ")");
        }

        // List players currently connected to the server
        lblClientList.text = "Player:\n";
        for (int i = 0; i < mainGame.game.playerList.size(); i++) {
          lblClientList.text += mainGame.game.playerList.get(i).name + " - " + mainGame.game.playerList.get(i).colour + "\n";
        }

      }

    }
  }

}
