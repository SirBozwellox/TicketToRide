/**
* @author Jack Gloyens 201164129 <jackgloyens@gmail.com>
* The screen container used for the main game screen on the client side
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
* Main game screen class that extends the container class
*/
public class GameScreen extends Container {

  public int routeSelect = -1;
  public int citySelect = -1;
  public int tableCardSelect = -1;
  public Game game;
  public Container cityNameView;
  public TextField cityNameText;
  public Container routeInfoView;
  public TextField trainsLeftText;
  public PlayerHand playerHand;
  public TextField routeNameText;
  public TextField routeInfoText;
  public static TextField updateText;
  public Container destinationsView;
  public TextField destinationsText;
  public Container tableCards;
  public Container trainCardDeck;
  public Renderer renderView;
  public Point mousePos;
  public myMouseListener ml;
  public TextField scoreListText;
  public Container interfaceBackground;
  public Container routesButton;

  /**
  * Default game screen constructor
  */
  public GameScreen() {

    // Create new game and add game map to the screen
    interfaceBackground = new Container(0, 0, mainGame.wWidth, mainGame.wHeight, Renderer.loadImage("assets/textures/interface1.png"));
    addChild(interfaceBackground);

    game = mainGame.game;
    game.gameMap.name = "Game map";
    addChild(game.gameMap);

    // Create interface
    tableCards = new Container(mainGame.wWidth - 96, (mainGame.wHeight / 2) - (64 * 4), 96, 256, null);
    addChild(tableCards);

    trainsLeftText = new TextField("", 64, mainGame.wHeight + 16, 64);
    trainsLeftText.colour = new Color(0.6f, 0.6f, 0.6f, 1.0f);
    trainsLeftText.anchorX = 0.5;
    addChild(trainsLeftText);

    scoreListText = new TextField("", 8, 88, 20);
    addChild(scoreListText);

    destinationsView = new Container(mainGame.wWidth - 512, mainGame.wHeight - 224, 384, 80, Renderer.loadImage("assets/textures/textBack2.png"));
    destinationsText = new TextField("", 0, 0, 16);
    destinationsView.addChild(destinationsText);
    destinationsText.anchorX = 0.5;
    addChild(destinationsView);

    trainCardDeck = new Container(mainGame.wWidth - 96, mainGame.wHeight - 160, 192, 128, Renderer.loadImage("assets/textures/cardDeck.png"));
    addChild(trainCardDeck);

    routesButton = new Container(665, 526, 222, 109, Renderer.loadImage("assets/textures/routesBtn.png"));
    addChild(routesButton);

    cityNameView = new Container(0, 0, 256, 32, Renderer.loadImage("assets/textures/textBack1.png"));
    cityNameView.name = "City name view";
    cityNameText = new TextField("", 0, 0);
    cityNameText.anchorX = 0.5;
    cityNameText.visible = true;
    cityNameView.addChild(cityNameText);
    addChild(cityNameView);

    routeInfoView = new Container(0, 0, 256, 128, Renderer.loadImage("assets/textures/textBack2.png"));
    routeInfoView.name = "Route info view";
    routeNameText = new TextField("", 0, 0);
    routeNameText.anchorX = 0.5;
    routeNameText.visible = true;
    routeInfoView.addChild(routeNameText);
    routeInfoText = new TextField("", 0, 0);
    routeInfoText.anchorX = 0.5;
    routeInfoText.visible = true;
    routeInfoView.addChild(routeInfoText);
    addChild(routeInfoView);

    updateText = new TextField("", mainGame.wWidth / 2, 48);
    updateText.colour = new Color(0.8f, 0.8f, 0.8f, 1.0f);
    updateText.anchorX = 0.5;
    updateText.visible = true;
    addChild(updateText);

    playerHand = new PlayerHand();
    playerHand.name = "";

    renderView = mainGame.renderView;
  }

  /**
  * Function to process the game screen, called for each tick of the main game loop
  */
  public void process() {
    if (visible) {

      // If the player hand object hasn't been added to the interface then add it
      // Seems to cause initialisation error if called too early hence the try-catch
      if (playerHand.name.equals("")) {
        playerHand.visible = true;
        try {
          playerHand = game.getPlayer(mainGame.client.colour).hand;
          addChild(1, playerHand);
        } catch (Exception e) {
          e.printStackTrace();
          try {
            Thread.sleep(10);
          } catch (Exception c) {

          }
        }
      }

      int newCitySelect = game.getCityMouse();

      ml = mainGame.ml;
      mousePos = mainGame.mousePos;

      // Process interface

      updateText.text = mainGame.game.updateText;

      if (citySelect != newCitySelect) {
        if (citySelect != -1) {
          game.gameMap.cities.get(citySelect).texture = City.upTex;
        }
        if (newCitySelect != -1) {
          game.gameMap.cities.get(newCitySelect).texture = City.downTex;
        }
        citySelect = newCitySelect;
      }

      if (citySelect == -1) {
        cityNameView.visible = false;
      } else {
        cityNameView.visible = true;
        cityNameView.x = mousePos.x + 16;
        if (mousePos.x + 16 + 256 > renderView.wWidth) {
          cityNameView.x = mousePos.x - 16 - 256;
        }
        cityNameView.y = mousePos.y - 8;
        cityNameText.x = cityNameView.x + 128;
        cityNameText.y = cityNameView.y + 36;
        cityNameText.text = game.gameMap.cities.get(citySelect).name;
      }

      int newRouteSelect = game.getRouteMouse();
      if (citySelect != -1) {
        newRouteSelect = -1;
      }
      if (routeSelect != newRouteSelect) {
        if (routeSelect != -1) {
          game.gameMap.routes.get(routeSelect).changeTexture(game.gameMap.routes.get(routeSelect).upTex);
        }
        if (newRouteSelect != -1) {
          game.gameMap.routes.get(newRouteSelect).changeTexture(game.gameMap.routes.get(newRouteSelect).downTex);
        }
        routeSelect = newRouteSelect;
      }

      if (routeSelect == -1) {
        routeInfoView.visible = false;
      } else {
        routeInfoView.visible = true;
        routeInfoView.x = mousePos.x + 16;
        if (mousePos.x + 16 + 256 > renderView.wWidth) {
          routeInfoView.x = mousePos.x - 16 - 256;
        }
        routeInfoView.y = mousePos.y - 8;
        if (mousePos.y + 120 > renderView.wHeight) {
          routeInfoView.y = renderView.wHeight - 128;
        }
        routeNameText.x = routeInfoView.x + 128;
        routeNameText.y = routeInfoView.y + 36;
        Route route = game.gameMap.routes.get(routeSelect);
        if (route.cityA.name.length() > route.cityB.name.length()) {
          routeNameText.text = route.cityA.name + "\nto " + route.cityB.name;
        } else {
          routeNameText.text = route.cityA.name + " to\n" + route.cityB.name;
        }
        routeInfoText.x = routeInfoView.x + 128;
        routeInfoText.fontSize = 20;
        if (route.claimedBy.equals("null") || route.claimedBy == null) {
          routeInfoText.y = routeInfoView.y + 36 + 56;
          routeInfoText.text = "Unclaimed\nCost: " + route.length;
          if (route.colour.equals("any")) {
            routeInfoText.text += " of anything";
          } else {
            routeInfoText.text += " " + route.colour;
          }
        } else {
          routeInfoText.y = routeInfoView.y + 36 + 64;
          routeInfoText.text = "Claimed by: " + route.claimedBy;
        }
      }

      trainsLeftText.text = Integer.toString(mainGame.game.getPlayer(mainGame.client.colour).trainsLeft);

      if (routesButton.mouseOver()) {
        destinationsView.visible = true;
        destinationsText.moveToRelative(destinationsView.width / 2, (destinationsView.height / 2) - 16);
        destinationsText.text = "Routes to claim:\n";
        for (int i = 0; i < mainGame.game.getPlayer(mainGame.client.colour).routeCards.size(); i++) {
          RouteCard rc = mainGame.game.getPlayer(mainGame.client.colour).routeCards.get(i);
          destinationsText.text += rc.cityA + " to " + rc.cityB + "(" + rc.score + ")\n";
        }
      } else {
        destinationsView.visible = false;
      }

      scoreListText.text = "Players:\n";
      for (int i = 0; i < mainGame.game.playerList.size(); i++) {
        if (mainGame.game.curPlayer == i) {
          scoreListText.text += "*";
        } else {
          scoreListText.text += " ";
        }
        if (mainGame.game.playerList.get(i).connected) {
          if (mainGame.game.playerList.get(i).name.length() > 12) {
            scoreListText.text += mainGame.game.playerList.get(i).name.substring(0, 9) + "...";
          } else {
            scoreListText.text += mainGame.game.playerList.get(i).name;
          }
          scoreListText.text += "\n  " + mainGame.game.playerList.get(i).colour + " - " + mainGame.game.playerList.get(i).score + "\n";
        } else {
          scoreListText.text += mainGame.game.playerList.get(i).name + "\n  Disconnected\n";
        }
      }

      if (mainGame.game.playerList.get(mainGame.game.curPlayer).colour.equals(mainGame.client.colour)) {

        if (routeSelect != -1) {
          if (ml.leftClick && !mainGame.game.paused) {
            if (mainGame.game.checkRouteClaim(routeSelect) && mainGame.game.playerMoves == 2) {
              mainGame.client.sendServerMessage("claimRoute;index(" + routeSelect + ")colour(" + mainGame.client.colour + ")");
              mainGame.client.sendServerMessage("nextPlayer;");
              game.gameMap.routes.get(routeSelect).changeTexture(game.gameMap.routes.get(routeSelect).downTex);
            }
          }
        }

        tableCardSelect = -1;
        for (int i = 0; i < tableCards.children.size(); i++) {
          if (tableCards.children.get(i).mouseOver()) {
            tableCardSelect = i;
          }
        }
        if (tableCardSelect != -1) {
          if (ml.leftClick && !mainGame.game.paused) {
            if (mainGame.game.playerMoves == 1) {
              if (!mainGame.game.tableCards.get(tableCardSelect).colour.equals("multi")) {
                mainGame.client.sendServerMessage("takeTableCard;index(" + tableCardSelect + ")colour(" + mainGame.client.colour + ")");
                mainGame.client.sendServerMessage("nextPlayer;");
              }
            } else {
              mainGame.client.sendServerMessage("takeTableCard;index(" + tableCardSelect + ")colour(" + mainGame.client.colour + ")");
              if (mainGame.game.tableCards.get(tableCardSelect).colour.equals("multi")) {
                mainGame.client.sendServerMessage("nextPlayer;");
              } else {
                mainGame.game.playerMoves -= 1;
              }
            }
          }
        }

        if (trainCardDeck.mouseOver() && ml.leftClick && !mainGame.game.paused) {
          mainGame.client.sendServerMessage("takeDeckCard;colour(" + mainGame.client.colour + ")");
          if (mainGame.game.playerMoves == 2) {
            mainGame.game.playerMoves -= 1;
          } else {
            mainGame.client.sendServerMessage("nextPlayer;");
          }
        }

      }

    }
  }

}
