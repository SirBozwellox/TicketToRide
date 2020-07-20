/**
* @author Jack Gloyens 201164129 <jackgloyens@gmail.com>
* The game manager, this processes incoming commands from the server and/or client and updates the game state accordinly
*/

import java.io.*;
import java.net.*;
import java.util.*;

/**
* Game manager class, extends the thread class to make it a runnable object
*/
public class GameManager extends Thread {

  public ArrayList<String> updateList = new ArrayList<String>();
  public int lastUpdate = 0;
  public int turnTimerReset = 45;
  public int turnTimer = turnTimerReset;
  public int gameState = 0;

  public Game game;

  /**
  * Default game manager constructor
  */
  public GameManager() {
    game = mainGame.game;
  }

  /**
  * Runnable thread function, used to update the turn timer
  */
  public void run() {
    try {
      while (true) {
        sleep(1000);
        if (gameState == 0) {

        } else if (gameState == 1) {
//          pushUpdate("turnTimer;timer(" + (turnTimer - 1) + ")");
        }
      }
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  /**
  * Get a list of all the update commands since the last update
  * @return A list of update commands
  */
  public ArrayList<String> getLatestUpdates() {
    ArrayList<String> updates = new ArrayList<String>();
    for (int i = lastUpdate; i < updateList.size(); i++) {
      updates.add(updateList.get(i));
    }
    lastUpdate = updateList.size();
    return updates;
  }

  /**
  * Push an update to the game manager, also checks that the command is valid and only processes changes to the game state if the game isn't paused
  * @param update the update command and any extra relevant data
  */
  public void pushUpdate(String update) {
    if (checkValidCommand(update)) {
//      System.out.println("  " + getCommand(update));
      if (!game.paused) {
        switch (getCommand(update)) {
          case "startGame":
            // Start the game
            gameState = 1;
            mainGame.game.startGame();
            mainGame.gameScreen.visible = true;
            mainGame.joinScreen.visible = false;
            break;
          case "turnTimer":
            // Set the turn timer to a new value
            turnTimer = Integer.parseInt(getValue(update, "timer"));
            if (turnTimer <= 0) {
              game.nextPlayer();
            }
            break;
          case "seed":
            // Set the randomisation seed to a new value
            game.seed = Integer.parseInt(getValue(update, "value"));
            System.out.println("New seed: " + game.seed);
            break;
          case "nextPlayer":
            // Make it the next players turn
            game.nextPlayer();
            break;
          case "claimRoute":
            // Claim the route at the given index for the player of the given colour
            game.claimRoute(Integer.parseInt(getValue(update, "index")), getValue(update, "colour"));
            break;
          case "takeTableCard":
            // Give the table card at the given index to the player of the given colour
            game.updateText = game.playerList.get(game.curPlayer).name + " took a " + game.tableCards.get(Integer.parseInt(getValue(update, "index"))).colour + " card from the table";
            game.playerList.get(game.curPlayer).hand.addCard(game.takeTableCard(Integer.parseInt(getValue(update, "index"))));
            break;
          case "takeDeckCard":
            // Give the player of the given colour the card from the top of the train card deck
            game.getPlayer(getValue(update, "colour")).hand.addCard(game.drawTrainCard());
            game.updateText = game.getPlayer(getValue(update, "colour")).name + " took a card from the deck";
            break;
        }
      }
      switch (getCommand(update)) {
        case "pause":
          // Freeze the game (stop polling game state updates)
          game.paused = true;
          break;
        case "unpause":
          // Unfreeze the game
          game.paused = false;
          break;
        case "changeName":
          // Change the name for player of the given colour
          game.changeName(getValue(update, "colour"), getValue(update, "username"));
          break;
        case "addPlayer":
          // Add a new player to the game of the given name and colour
          game.addPlayer(getValue(update, "name"), getValue(update, "colour"));
          break;
        case "addComputer":
          // Add a new AI player to the game of the given name and colour
          game.addComputer(getValue(update, "name"), getValue(update, "colour"));
          break;
        case "removePlayer":
          // Remove the player of the given colour from the game
          game.removePlayer(getValue(update, "colour"));
          break;
        case "slowUpdate":
          // Change to slow update mode (AI players act more human)
          game.quickUpdate = false;
          break;
        case "quickUpdate":
          // Change to quick update mode (AI players turns are simulated instantly)
          game.quickUpdate = true;
          break;
      }
      // Add the update to the update list
      updateList.add(update);
    }
  }

  /**
  * Get the command from an update message
  * @param message the update message being parsed
  * @return The command that the update message starts with
  */
  public String getCommand(String message) {
    return message.substring(0, message.indexOf(";"));
  }

  /**
  * Get a value (as a string) from an update message
  * @param message the update message being parsed
  * @param key the key name of the value that needs to be obtained from the update message
  * @return The value of the key value pair obtained from the update message, blank if no such key exists in the message
  */
  public String getValue(String message, String key) {
    if (message.contains(key)) {
      return message.substring(message.indexOf(key) + key.length() + 1, message.indexOf(")", message.indexOf(key) + key.length() + 1));
    } else {
      return "";
    }
  }

  /**
  * Check if the update message is valid or not
  * @param message the update message being parsed
  * @return True if the message is valid, false if not
  */
  public boolean checkValidCommand(String message) {
    if (message.contains(";") && message.length() > 2) {
      return true;
    } else {
      return false;
    }
  }

  /**
  * Save the current game to the given file by writing the entire update list
  * @param filename the name of the file, excluding the .txt extension
  */
  public void saveGame(String filename) {
    try {
      FileWriter fw = new FileWriter(filename + ".txt", false);
      BufferedWriter bw = new BufferedWriter(fw);
      PrintWriter out = new PrintWriter(bw);
      for (int i = 0; i < updateList.size(); i++) {
        out.print(updateList.get(i) + "\n");
      }
      bw.close();
      System.out.println("Game saved as " + filename + ".txt");
    } catch (IOException e) {
      System.out.println("File output error, game not saved");
    }
  }

  /**
  * Load a save file by reading in the list of updates and simulating the game up to that point
  * @param filename the name of the file, excluding the .txt extension
  */
  public void loadGame(String filename) {
    updateList.clear();
    try (BufferedReader br = new BufferedReader(new FileReader(filename + ".txt"))) {
        // Switch to quick update mode
        pushUpdate("quickUpdate;");
        String line;
        while ((line = br.readLine()) != null) {
          System.out.println(line);
          // Ignore slow update commands but push all other update commands
          if (!getCommand(line).equals("slowUpdate")) {
            pushUpdate(line);
          }
        }
        // Pause the game and switch back to slow update mode
        pushUpdate("pause;");
        pushUpdate("slowUpdate;");
        System.out.println("Game loaded correctly");
        // Disconnect all human players and allow new players to reconnect to the loaded game
        for (int i = 0; i < game.playerList.size(); i++) {
          if (game.playerList.get(i).type.equals("human")) {
            pushUpdate("removePlayer;colour(" + game.playerList.get(i).colour + ")");
          } else {
            Server.availableColours.remove(Server.availableColours.indexOf(game.playerList.get(i).colour));
          }
        }
    } catch (IOException e) {
      System.out.println("File input error, game not loaded");
    }
  }

}
