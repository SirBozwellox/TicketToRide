/**
* @author Jack Gloyens 201164129 <jackgloyens@gmail.com>
* The screen container used for the server program
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
* Main server screen class that extends the container class
*/
public class ServerScreen extends Container {

  public Game game;
  public Renderer renderView;
  public Point mousePos;
  public myMouseListener ml;

  public Container findServer;
  public Container inGame;
  public UIButton btnStartServer;
  public UIButton btnAddComputer;
  public UIButton btnSaveGame;
  public UIButton btnLoadGame;
  public TextField lblClientList;
  public TextField lblIpAddress;
  public TextField lblPassword;
  public UITextInput txtPassword;
  public ArrayList<UIButton> removeButtons = new ArrayList<UIButton>();

  /**
  * Default server screen constructor and initialiser
  */
  public ServerScreen() {
    game = mainGame.game;
    renderView = mainGame.renderView;

    // Create interface
    btnStartServer = new UIButton((renderView.wWidth / 2) - 480, 500, 320, 64, "Start game");
    btnSaveGame = new UIButton((renderView.wWidth / 2) - 160, 500, 320, 64, "Save game");
    btnLoadGame = new UIButton((renderView.wWidth / 2) - 160, 580, 320, 64, "Load game");
    btnAddComputer = new UIButton((renderView.wWidth / 2) + 160, 500, 320, 64, "Add AI");
    lblClientList = new TextField("Players:", (renderView.wWidth / 2), 200);
    lblClientList.anchorX = 0.5;
    lblClientList.fontSize = 40;
    lblIpAddress = new TextField("IP address: ", (renderView.wWidth / 2), 64);
    lblIpAddress.anchorX = 0.5;
    lblIpAddress.fontSize = 40;
    lblPassword = new TextField("Password: ", (renderView.wWidth / 2) - 320, 110);
    lblPassword.fontSize = 40;
    txtPassword = new UITextInput((renderView.wWidth / 2) - 120, 70, 480, 64, 20);
    txtPassword.text = "";
    for (int i = 0; i < 5; i++) {
      removeButtons.add(new UIButton((renderView.wWidth / 2) + 320, 190 + (i * 40), 40, 40, "X"));
      addChild(removeButtons.get(i));
    }
    addChild(btnStartServer);
    addChild(lblClientList);
    addChild(lblIpAddress);
    addChild(btnAddComputer);
    addChild(lblPassword);
    addChild(txtPassword);
    addChild(btnSaveGame);
    addChild(btnLoadGame);
  }

  /**
  * Function to process the server screen, called every tick of the main game loop
  */
  public void process() {
    if (visible) {

      ml = mainGame.ml;
      mousePos = mainGame.mousePos;

//      System.out.println(Server.gameManager.game.playerList.size());

      // Process interface
      btnStartServer.process();
      btnAddComputer.process();
      txtPassword.process();
      btnSaveGame.process();
      btnLoadGame.process();
      Server.password = txtPassword.text;

      // Process player remove buttons
      for (int i = 0; i < removeButtons.size(); i++) {
        if (i < Server.gameManager.game.playerList.size()) {
          removeButtons.get(i).visible = true;
        } else {
          removeButtons.get(i).visible = false;
        }
        removeButtons.get(i).process();
        if (removeButtons.get(i).ifClicked()) {
          if (Server.gameManager.game.playerList.get(i).type.equals("AI")) {
            Server.availableColours.add(0, Server.gameManager.game.playerList.get(i).colour);
            Server.gameManager.pushUpdate("removePlayer;colour(" + Server.gameManager.game.playerList.get(i).colour + ")");
          } else {
            for (int c = 0; c < Server.sessionList.size(); c++) {
              if (Server.sessionList.get(c).colour.equals(Server.gameManager.game.playerList.get(i).colour)) {
                Server.sessionList.get(c).closeConnection();
              }
            }
          }
        }
      }

      if (!Server.gameManager.game.inGame) {
        btnStartServer.t.text = "Start game";
      } else {
        if (Server.gameManager.game.paused) {
          btnStartServer.t.text = "Unpause game";
        } else {
          btnStartServer.t.text = "Pause game";
        }
      }

      // If the start button is pressed then generate a new randomisation seed and push the start game update to the game manager
      if (btnStartServer.ifClicked()) {
        if (Server.gameManager.gameState == 0) {
          Server.gameManager.pushUpdate("seed;value(" + Math.abs((int)(System.currentTimeMillis())) + ")");
          Server.gameManager.pushUpdate("startGame;");
        } else {
          if (Server.gameManager.game.paused) {
            Server.gameManager.game.paused = false;
            Server.gameManager.pushUpdate("unpause;");
          } else {
            Server.gameManager.game.paused = true;
            Server.gameManager.pushUpdate("pause;");
          }
        }
      }

      if (btnAddComputer.ifClicked() && Server.availableColours.size() > 0) {
        Server.gameManager.pushUpdate("addComputer;name(Comp " + (Server.gameManager.game.playerList.size() + 1) + ")colour(" + Server.availableColours.remove(0) + ")");
      }

      if (btnSaveGame.ifClicked()) {
        Server.gameManager.saveGame("save1");
      }

      if (btnLoadGame.ifClicked()) {
        Server.gameManager.loadGame("save1");
      }

      // List currently connected players
      lblClientList.text = "Players:\n";
      for (int i = 0; i < Server.gameManager.game.playerList.size(); i++) {
        lblClientList.text += Server.gameManager.game.playerList.get(i).name + " - " + Server.gameManager.game.playerList.get(i).colour + "\n";
      }

      lblIpAddress.text = "IP address: " + Server.IPAddress;

    }
  }

}
