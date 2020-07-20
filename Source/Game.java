/**
* @author Jack Gloyens 201164129 <jackgloyens@gmail.com>
* The main game class and all the classes for routes, cities, players etc.
*/

import java.io.*;
import java.net.*;
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
import java.util.HashMap;

public class Game {

  public Map gameMap;
  public ArrayList<TrainCard> trainCardDeck = new ArrayList<TrainCard>();
  public ArrayList<Player> playerList = new ArrayList<Player>();
  public ArrayList<RouteCard> routeCardDeck = new ArrayList<RouteCard>();
  public ArrayList<TrainCard> discardPile = new ArrayList<TrainCard>();
  public ArrayList<TrainCard> tableCards = new ArrayList<TrainCard>();
  public int curPlayer;
  public int seed = 2000;
  public boolean inGame = false;
  public int playerMoves = 2;
  public boolean quickUpdate = false;

  public static String updateText = "Game started";

  public static String colourList[] = {"red", "orange", "yellow", "green", "blue", "purple", "black", "white", "multi"};
  public static int routeScoring[] = {1, 2, 4, 7, 10, 15, 18};

  public static int segmentSize = 60;
  public static int segmentHeight = 21;

  public static boolean paused = false;

  public static HashMap<String, Image> trainCardImages = new HashMap<>();

  /**
  * Game constructor
  */
  public Game() {
    curPlayer = 0;

    // Load textures for train cards
    trainCardImages.put("red", Renderer.loadImage("assets/textures/cardRed.png"));
    trainCardImages.put("orange", Renderer.loadImage("assets/textures/cardOrange.png"));
    trainCardImages.put("yellow", Renderer.loadImage("assets/textures/cardYellow.png"));
    trainCardImages.put("green", Renderer.loadImage("assets/textures/cardGreen.png"));
    trainCardImages.put("blue", Renderer.loadImage("assets/textures/cardBlue.png"));
    trainCardImages.put("purple", Renderer.loadImage("assets/textures/cardPurple.png"));
    trainCardImages.put("black", Renderer.loadImage("assets/textures/cardBlack.png"));
    trainCardImages.put("white", Renderer.loadImage("assets/textures/cardWhite.png"));
    trainCardImages.put("multi", Renderer.loadImage("assets/textures/cardMulti.png"));

    // Create map of america, calculate positions/rotations of route segments and add objects to the map
    createAmericaMap();
    gameMap.visible = true;
    for (int i = 0; i < gameMap.routes.size(); i++) {
      gameMap.routes.get(i).calcSegments();
    }
    for (int i = 0; i < gameMap.routes.size(); i++) {
      gameMap.addChildren(gameMap.routes.get(i).segmentList);
    }
    for (int i = 0; i < gameMap.cities.size(); i++) {
      City c = gameMap.cities.get(i);
      gameMap.addChild(c);
      gameMap.addChild(new TextField(c.name.substring(0, 1), c.x - (c.width / 4), c.y + (c.height / 4)));
    }
    gameMap.scaleToWidth((int)(mainGame.wWidth * 0.7));
    gameMap.moveBy((int)(mainGame.wWidth * 0.25 * 0.5) + 64, (int)(mainGame.wHeight * 0.1));

    // Create and shuffle the various decks
    createTrainDeck();
    shuffleTrainDeck();
    createRouteDeck();
//    printTrainDeck();
  }

  /**
  * Deal cards to all the players and start the game
  */
  public void startGame() {
    System.out.println("Game started");
    shuffleTrainDeck();
    dealTrainCards();
    shuffleRouteDeck();
    dealRouteCards();
    printPlayerInfo();
    inGame = true;
    curPlayer = -1;
    nextPlayer();
  }

  /**
  * @return Return the index of the route that the mouse is currently over, -1 if no routes are being moused over
  */
  public int getRouteMouse() {
    int retval = -1;
    for (int i = 0; i < gameMap.routes.size(); i++) {
      if (gameMap.routes.get(i).checkMouse()) {
        retval = i;
      }
    }
    return retval;
  }

  /**
  * @return Return the index of the city that the mouse is currently over, -1 if no cities are being moused over
  */
  public int getCityMouse() {
    int retval = -1;
    for (int i = 0; i < gameMap.cities.size(); i++) {
      if (gameMap.cities.get(i).mouseOver()) {
        retval = i;
      }
    }
    return retval;
  }

  /**
  * Add an AI player to the game
  * @param name the name of the player
  * @param colour the unique train colour of the player
  */
  public void addComputer(String name, String colour) {
    playerList.add(new AIPlayer(name, colour, this));
  }

  /**
  * Add a human player to the game
  * @param name the name of the player
  * @param colour the unique train colour of the player
  */
  public void addPlayer(String name, String colour) {
    addPlayer(new Player(name, colour));
  }

  /**
  * Add a human player to the game
  * @param player the player object of the player to add
  */
  public void addPlayer(Player player) {
    System.out.println("Adding new player!");
    for (int i = 0; i < playerList.size(); i++) {
      if (!playerList.get(i).connected) {
        playerList.get(i).connected = true;
        System.out.println("Connected player to colour " + playerList.get(i).colour);
        return;
      }
    }
    playerList.add(player);
    if (inGame) {
      for (int i = 0; i < 3; i++) {
        player.hand.addCard(drawTrainCard());
        player.routeCards.add(drawRouteCard());
      }
    }
  }

  /**
  * Remove player from the game
  * @param player the player object to be removed
  */
  public void removePlayer(Player player) {
    removePlayer(player.colour);
  }

  /**
  * Remove player from the game. If already in a game then simply disconnect the player otherwise completely remove them
  * @param colour the unique train colour of the player to be removed
  */
  public void removePlayer(String colour) {
    for (int i = 0; i < playerList.size(); i++) {
      if (playerList.get(i).colour.equals(colour)) {
        if (!inGame) {
          playerList.remove(i);
        } else {
          playerList.get(i).connected = false;
          if (curPlayer == i) {
            nextPlayer();
          }
        }
        System.out.println("User removed correctly");
        if (checkAllDisconnected() && inGame && !paused) {
          Server.gameManager.pushUpdate("pause;");
          System.out.println("No human players remaining, game paused");
        }
        return;
      }
    }
    System.out.println("User " + colour + " not found");
  }

  /**
  * Change the name of a player in the game
  * @param colour the unique train colour of the player
  * @param name the new name of the player
  */
  public void changeName(String colour, String name) {
    for (int i = 0; i < playerList.size(); i++) {
      if (playerList.get(i).colour.equals(colour)) {
        playerList.get(i).name = name;
        return;
      }
    }
  }

  /**
  * Check if all the human players have disconnected from the game
  * @return True if there are no human players remaining, false if there are
  */
  public boolean checkAllDisconnected() {
    for (int i = 0; i < playerList.size(); i++) {
      if (playerList.get(i).connected && playerList.get(i).type.equals("human")) {
        return false;
      }
    }
    return true;
  }

  /**
  * Make it the next players turn, if it's an AI player then simulate a turn
  */
  public void nextPlayer() {
    if (!paused) {
      curPlayer += 1;
      if (curPlayer >= playerList.size()) {
        curPlayer = 0;
      }
      while (!playerList.get(curPlayer).connected) {
        curPlayer += 1;
        if (curPlayer >= playerList.size()) {
          curPlayer = 0;
        }
      }
      System.out.println("It is " + playerList.get(curPlayer).name + "'s turn");
//      updateText = "It is " + playerList.get(curPlayer).name + "'s turn";
      if (playerList.get(curPlayer).type.equals("AI")) {
        if (quickUpdate) {
          playerList.get(curPlayer).quickRun();
        } else {
          playerList.get(curPlayer).process();
        }
      }
      playerMoves = 2;
    }
  }

  /**
  * Get player object from unique colour identifier
  * @param colour the unique colour of the player
  * @return The player object, null if the player can't be found
  */
  public Player getPlayer(String colour) {
    for (int i = 0; i < playerList.size(); i++) {
      if (playerList.get(i).colour.equals(colour)) {
        return playerList.get(i);
      }
    }
    return null;
  }

  /**
  * Get the player index from their unique colour identifier
  * @param colour the unique colour of the player
  * @return The index of the player
  */
  public int getPlayerIndex(String colour) {
    for (int i = 0; i < playerList.size(); i++) {
      if (playerList.get(i).colour.equals(colour)) {
        return i;
      }
    }
    return -1;
  }

  /**
  * Draw the game to the screen
  * @param g the graphics environment, used by Java's 2D graphics framework
  * @param r the rendering object which describes the screen space
  */
  public void draw(Graphics g, Renderer r) {
    gameMap.draw(g, r);
  }

  /**
  * Check if the game has ended
  * @return True if the game has finished, false if not
  */
  public boolean checkEndGame() {
    int lowestTrains = 100;
    for (int i = 0; i < playerList.size(); i++) {
      if (playerList.get(i).trainsLeft < lowestTrains) {
        lowestTrains = playerList.get(i).trainsLeft;
      }
    }
    int lowestLength = 100;
    for (int i = 0; i < gameMap.routes.size(); i++) {
      if (gameMap.routes.get(i).claimedBy.equals("null") && gameMap.routes.get(i).length < lowestLength) {
        lowestLength = gameMap.routes.get(i).length;
      }
    }
    if (lowestTrains < lowestLength) {
      return true;
    }
    return false;
  }

  /**
  * Get the player with the highest score
  * @return The player object of the highest scoring player
  */
  public Player getLeader() {
    Player leader = playerList.get(0);
    for (int i = 1; i < playerList.size(); i++) {
      if (playerList.get(i).score > leader.score) {
        leader = playerList.get(i);
      }
    }
    return leader;
  }

  /**
  * Get the shortest route between the two cities on a route card
  * @param rc the route card that specifies the two cities to go between
  * @param colour the colour of the player to check routes against
  * @return A list of routes between neighbouring cities that connect the two cities on the route card
  */
  public ArrayList<Route> getShortestRoute(RouteCard rc, String colour) {
    return getShortestRoute(rc.cityA, rc.cityB, colour);
  }

  /**
  * Get the shortest route between two cities, implemented using Djikstra's algorithm
  * @param cityA the name of the starting city
  * @param cityB the name of the ending city
  * @param colour the colour of the player to check routes against
  * @return A list of routes between neighbouring cities that connect the two cities on the route card
  */
  public ArrayList<Route> getShortestRoute(String cityA, String cityB, String colour) {
    // Create a list of all cities on the map and an empty list of cities that have been visited
    CityQueue cityQueue = new CityQueue(gameMap.cities, cityA);
    CityQueue visited = new CityQueue();

    // While there are still cities left to check
    while (cityQueue.size() > 0) {
      // Get the city with the smallest current distance from the start and put it in the visted list
      CityQueueObj currentCity = cityQueue.getSmallest();
      cityQueue.remove(currentCity.city);
      visited.add(currentCity);
      // If the city can't be reached then exit the loop
      if (currentCity.distance == -1) {
//        System.out.println("  City can't be reached");
        break;
      }
      // If the ending city has been reached then exit the loop
      if (currentCity.city.name.equals(cityB)) {
        break;
      }
      // Get a list of neighbouring cities and find the shortest route to them using unclaimed routes
      ArrayList<City> neighbours = getNeighbours(currentCity.city);
      for (int i = 0; i < neighbours.size(); i++) {
        if (cityQueue.contains(neighbours.get(i))) {
          if (gameMap.getRoute(currentCity.city, neighbours.get(i)).claimedBy.equals("null") || gameMap.getRoute(currentCity.city, neighbours.get(i)).claimedBy.equals(colour)) {
            int alt = currentCity.distance + gameMap.getRoute(currentCity.city, neighbours.get(i)).length;
            if (alt < cityQueue.get(neighbours.get(i)).distance || cityQueue.get(neighbours.get(i)).distance == -1) {
              cityQueue.get(neighbours.get(i)).distance = alt;
              cityQueue.get(neighbours.get(i)).previous = currentCity.city;
            }
          }
        }
      }

    }

    ArrayList<Route> retval = new ArrayList<Route>();
    String output = "";
    City u = gameMap.getCity(cityB);
    // If the city can't be reached then return an empty list
    if (!visited.contains(u)) {
      return retval;
    }
    // Work back from the ending city to create a list for the shortest route
    while (visited.get(u).previous != null) {
      output = ", " + u.name + output;
      retval.add(gameMap.getRoute(u, visited.get(visited.get(u).previous).city));
      u = visited.get(visited.get(u).previous).city;
    }
    output = cityA + output;

//    System.out.println("Getting shortest path between " + cityA + " and " + cityB);
//    System.out.println("  Route: " + output);

    return retval;

  }

  /**
  * A CityQueue class that implements a priority queue for the cities to be used for the path finding algorithm above
  */
  class CityQueue {

    ArrayList<CityQueueObj> cityQueue = new ArrayList<CityQueueObj>();

    /**
    * City Queue constructor
    * @param cityList the list of cities to be added to the queue
    * @param cityA the starting city, it's distance will be set to 0
    */
    public CityQueue(ArrayList<City> cityList, String cityA) {
      cityQueue = new ArrayList<CityQueueObj>();
      for (int i = 0; i < cityList.size(); i++) {
        cityQueue.add(new CityQueueObj(cityList.get(i)));
        if (cityList.get(i).name.equals(cityA)) {
          cityQueue.get(cityQueue.size() - 1).distance = 0;
        }
      }
    }

    /**
    * The default constructor for the city queue
    */
    public CityQueue() {
      cityQueue = new ArrayList<CityQueueObj>();
    }

    /**
    * Get the number of cities in the city queue
    * @return The length of the city queue
    */
    public int size() {
      return cityQueue.size();
    }

    /**
    * Add the given city queue object to the city queue
    * @param c the city queue objec to be added to the queue
    */
    public void add(CityQueueObj c) {
      cityQueue.add(c);
    }

    /**
    * Remove the city queue object that relates to the given city from the queue
    * @param c the city for the city queue object that relates to it
    */
    public void remove(City c) {
      for (int i = 0; i < cityQueue.size(); i++) {
        if (cityQueue.get(i).city.equals(c)) {
          cityQueue.remove(i);
          return;
        }
      }
    }

    /**
    * Check if the queue contains a city queue object with the given city
    * @param c the city to be checked
    * @return True if the city is contained in the queue, false if not
    */
    public boolean contains(City c) {
      for (int i = 0; i < cityQueue.size(); i++) {
        if (cityQueue.get(i).city.equals(c)) {
          return true;
        }
      }
      return false;
    }

    /**
    * Get the city queue object with the smallest positive distance
    * @return The city queue object with the smallest positive distance
    */
    public CityQueueObj getSmallest() {
      CityQueueObj retval = cityQueue.get(0);
      for (int i = 1; i < cityQueue.size(); i++) {
        if (cityQueue.get(i).distance != -1 && cityQueue.get(i).distance < retval.distance) {
          retval = cityQueue.get(i);
        }
        if (retval.distance == -1 && cityQueue.get(i).distance >= 0) {
          retval = cityQueue.get(i);
        }
      }
      return retval;
    }

    /**
    * Get the city queue object that contains the given city in the queue
    * @param c the city to be checked in the queue
    * @return The city queue object that contains the given city
    */
    public CityQueueObj get(City c) {
      for (int i = 0; i < cityQueue.size(); i++) {
        if (cityQueue.get(i).city.equals(c)) {
          return cityQueue.get(i);
        }
      }
      return null;
    }

  }

  /**
  * City queue object that stores the city, it's distance from the starting city and the previous city it's connected to in the shortest route
  */
  class CityQueueObj {
    City city;
    int distance;
    City previous;

    /**
    * The city queue object constructor, distance and previous city are set to -1 and null respectively
    * @param city the city that the city queue object relates to
    */
    CityQueueObj(City city) {
      this.city = city;
      distance = -1;
      previous = null;
    }
  }

  /**
  * Get a list of neighbours for the given city
  * @param city the city object to get neighbours for
  * @return A list of neighbouring cities
  */
  public ArrayList<City> getNeighbours(City city) {
    ArrayList<City> retval = new ArrayList<City>();
    for (int i = 0; i < gameMap.routes.size(); i++) {
      if (gameMap.routes.get(i).cityA.name.equals(city.name) && !retval.contains(gameMap.routes.get(i).cityB)) {
        retval.add(gameMap.routes.get(i).cityB);
      }
      if (gameMap.routes.get(i).cityB.name.equals(city.name) && !retval.contains(gameMap.routes.get(i).cityA)) {
        retval.add(gameMap.routes.get(i).cityA);
      }
    }
    return retval;
  }

  /**
  * Check if the routes for a particular player have been completed, if so remove them and draw a new route card
  * @param p the player object to check the routes for
  */
  public void checkPlayerRoutes(Player p) {
    for (int i = 0; i < p.routeCards.size(); i++) {
      if (checkDestination(p.routeCards.get(i), p.colour)) {
        System.out.println("Player " + p.name + " finished route from " + p.routeCards.get(i).cityA + " to " + p.routeCards.get(i).cityB);
        updateText = p.name + " finished route from " + p.routeCards.get(i).cityA + " to " + p.routeCards.get(i).cityB;
        p.score += p.routeCards.get(i).score;
        p.routeCards.remove(i);
        i -= 1;
      }
    }
    if (p.routeCards.size() == 0) {
      p.routeCards.add(drawRouteCard());
      checkPlayerRoutes(p);
    }
  }

  /**
  * Check if the two cities in the given route card are connected by routes claimed by the given colour
  * @param rc the route card to check the cities for
  * @param colour the unique train colour of the player to check for
  * @return True if the two cities are connected by the given colour, false if not
  */
  public boolean checkDestination(RouteCard rc, String colour) {
    String start = rc.cityA;
    String end = rc.cityB;
    int indexA = -1;
    int indexB = -1;
    for (int i = 0; i < gameMap.cities.size(); i++) {
      if (gameMap.cities.get(i).name.equals(start)) {
        indexA = i;
      }
      if (gameMap.cities.get(i).name.equals(end)) {
        indexB = i;
      }
    }

    if (indexA != -1 && indexB != -1) {
      ArrayList<Route> possibleRoutes = new ArrayList<Route>();
      for (int i = 0; i < gameMap.routes.size(); i++) {
        if (gameMap.routes.get(i).claimedBy.equals(colour)) {
          if (gameMap.routes.get(i).cityA.name.equals(start) || gameMap.routes.get(i).cityB.name.equals(start)) {
            System.out.println("  " + gameMap.routes.get(i).cityA.name + ", " + gameMap.routes.get(i).cityB.name);
            possibleRoutes.add(gameMap.routes.get(i));
          }
        }
      }

      boolean endFlag = false;
      while (!endFlag) {
        endFlag = true;
        for (int i = 0; i < possibleRoutes.size(); i++) {
          for (int r = 0; r < gameMap.routes.size(); r++) {
            if (gameMap.routes.get(r).claimedBy.equals(colour)) {
              if (gameMap.routes.get(r).cityA.name.equals(possibleRoutes.get(i).cityA.name)
              || gameMap.routes.get(r).cityA.name.equals(possibleRoutes.get(i).cityB.name)
              || gameMap.routes.get(r).cityB.name.equals(possibleRoutes.get(i).cityA.name)
              || gameMap.routes.get(r).cityB.name.equals(possibleRoutes.get(i).cityB.name)) {
                if (!possibleRoutes.contains(gameMap.routes.get(r))) {
                  if (gameMap.routes.get(r).cityA.name.equals(end) || gameMap.routes.get(r).cityB.name.equals(end)) {
                    return true;
                  }
                  possibleRoutes.add(gameMap.routes.get(r));
                  endFlag = false;
                }
              }
            }
          }
        }
      }
    }

    return false;
  }

  /**
  * Check if the given route can be claimed by the current player
  * @param routeIndex the index of the route that needs to be checked
  * @return True if the route can be claimed, false if not
  */
  public boolean checkRouteClaim(int routeIndex) {
    if (playerList.get(curPlayer).trainsLeft >= gameMap.routes.get(routeIndex).length) {
      if (gameMap.routes.get(routeIndex).claimedBy.equals("null")) {
        if (gameMap.routes.get(routeIndex).colour.equals("any")) {
          if (playerList.get(curPlayer).countColour(playerList.get(curPlayer).highestColour()) >= gameMap.routes.get(routeIndex).length) {
            return true;
          }
        } else {
          if (playerList.get(curPlayer).countColour(gameMap.routes.get(routeIndex).colour) >= gameMap.routes.get(routeIndex).length) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
  * Claim a route for the specified colour
  * @param r the route to claim
  * @param colour the unique colour of the player claiming the route
  */
  public void claimRoute(Route r, String colour) {
    for (int i = 0; i < gameMap.routes.size(); i++) {
      if (gameMap.routes.get(i).equals(r)) {
        claimRoute(i, colour);
      }
    }
  }


  /**
  * Claim a route for the specified colour
  * @param routeIndex the index of the route to claim
  * @param colour the unique colour of the player claiming the route
  */
  public void claimRoute(int routeIndex, String colour) {
    int playerIndex = 0;
    for (int i = 0; i < playerList.size(); i++) {
      if (playerList.get(i).colour.equals(colour)) {
        playerIndex = i;
      }
    }
    playerList.get(playerIndex).removeCards(gameMap.routes.get(routeIndex).colour, gameMap.routes.get(routeIndex).length);
    playerList.get(playerIndex).trainsLeft -= gameMap.routes.get(routeIndex).length;
    playerList.get(playerIndex).score += routeScoring[gameMap.routes.get(routeIndex).length - 1];
    gameMap.claimRoute(routeIndex, colour);
    updateText = playerList.get(playerIndex).name + " claimed the route between " + gameMap.routes.get(routeIndex).cityA.name + " and " + gameMap.routes.get(routeIndex).cityB.name;
    checkPlayerRoutes(getPlayer(colour));
    if (checkEndGame()) {
      System.out.println(getLeader().name + " wins!");
      updateText = getLeader().name + " wins!";
      paused = true;
    }
  }

  /**
  * Create a deck of cards representing all the routes and their various scores once completed
  */
  public void createRouteDeck() {
    routeCardDeck.add(new RouteCard("Los Angeles", "New York", 21));
    routeCardDeck.add(new RouteCard("Deluth", "Houston", 8));
    routeCardDeck.add(new RouteCard("Sault St. Marie", "Nashville", 8));
    routeCardDeck.add(new RouteCard("New York", "Atlanta", 6));
    routeCardDeck.add(new RouteCard("Portland", "Nashville", 17));
    routeCardDeck.add(new RouteCard("Vancouver", "Montreal", 20));
    routeCardDeck.add(new RouteCard("Deluth", "El Paso", 10));
    routeCardDeck.add(new RouteCard("Toronto", "Miami", 10));
    routeCardDeck.add(new RouteCard("Portland", "Phoenix", 11));
    routeCardDeck.add(new RouteCard("Dallas", "New York", 11));
    routeCardDeck.add(new RouteCard("Calgary", "Salt Lake City", 7));
    routeCardDeck.add(new RouteCard("Calgary", "Phoenix", 13));
    routeCardDeck.add(new RouteCard("Los Angeles", "Miami", 20));
    routeCardDeck.add(new RouteCard("Winnipeg", "Little Rock", 11));
    routeCardDeck.add(new RouteCard("San Francisco", "Atlanta", 17));
    routeCardDeck.add(new RouteCard("Kansas City", "Houston", 5));
    routeCardDeck.add(new RouteCard("Los Angeles", "Chicago", 16));
    routeCardDeck.add(new RouteCard("Denver", "Pittsburgh", 11));
    routeCardDeck.add(new RouteCard("Chicago", "Santa Fe", 9));
    routeCardDeck.add(new RouteCard("Vancouver", "Santa Fe", 13));
    routeCardDeck.add(new RouteCard("Boston", "Miami", 12));
    routeCardDeck.add(new RouteCard("Chicago", "New Orleans", 7));
    routeCardDeck.add(new RouteCard("Montreal", "Atlanta", 9));
    routeCardDeck.add(new RouteCard("Seattle", "New York", 22));
    routeCardDeck.add(new RouteCard("Denver", "El Paso", 4));
    routeCardDeck.add(new RouteCard("Helena", "Los Angeles", 8));
    routeCardDeck.add(new RouteCard("Winnipeg", "Houston", 12));
    routeCardDeck.add(new RouteCard("Montreal", "New Orleans", 13));
    routeCardDeck.add(new RouteCard("Sault St. Marie", "Oklahoma City", 9));
    routeCardDeck.add(new RouteCard("Seattle", "Los Angeles", 9));
  }

  /**
  * Shuffle the deck of route cards using the game's random seed
  */
  public void shuffleRouteDeck() {
    Collections.shuffle(routeCardDeck, new Random(seed));
  }

  /**
  * Remove a route card from the top of the deck and return it
  * @return The top route card
  */
  public RouteCard drawRouteCard() {
    RouteCard rc = routeCardDeck.get(0);
    routeCardDeck.remove(0);
    return rc;
  }

  /**
  * Deal 3 route cards to all players currently in the game
  */
  public void dealRouteCards() {
    for (int i = 0; i < playerList.size(); i++) {
      for (int c = 0; c < 3; c++) {
        playerList.get(i).routeCards.add(drawRouteCard());
      }
    }
  }

  /**
  * Create a deck of train cards
  */
  public void createTrainDeck() {
    for (int i = 0; i < 12; i++) {
      trainCardDeck.add(new TrainCard("red"));
      trainCardDeck.add(new TrainCard("orange"));
      trainCardDeck.add(new TrainCard("yellow"));
      trainCardDeck.add(new TrainCard("green"));
      trainCardDeck.add(new TrainCard("blue"));
      trainCardDeck.add(new TrainCard("purple"));
      trainCardDeck.add(new TrainCard("black"));
      trainCardDeck.add(new TrainCard("white"));
    }
    for (int i = 0; i < 14; i++) {
      trainCardDeck.add(new TrainCard("multi"));
    }
  }

  /**
  * Shuffle the deck of train cards using the game's random seed
  */
  public void shuffleTrainDeck() {
    Collections.shuffle(trainCardDeck, new Random(seed));
  }

  /**
  * Add all the cards in the discard pile back into the train card deck and then shuffle
  */
  public void reshuffleTrainDeck() {
    System.out.println("Deck re-shuffled");
    for (int i = 0; i < discardPile.size(); i++) {
      trainCardDeck.add(discardPile.get(i));
    }
    discardPile.clear();
    shuffleTrainDeck();
  }

  /**
  * Output the current contents of the train card deck to the console
  */
  public void printTrainDeck() {
    for (int i = 0; i < trainCardDeck.size(); i++) {
      System.out.println(trainCardDeck.get(i).colour);
    }
  }

  /**
  * Deal 3 train cards to all players currently in the game and put 5 train cards on the table
  */
  public void dealTrainCards() {
    for (int i = 0; i < playerList.size(); i++) {
      for (int c = 0; c < 3; c++) {
        playerList.get(i).hand.addCard(drawTrainCard());
      }
    }
    for (int i = 0; i < 5; i++) {
      Container c = new Container(mainGame.wWidth - 96, mainGame.wHeight / 2, 192, 128);
      TrainCard t = drawTrainCard();
      c.name = t.colour;
      c.texture = Game.trainCardImages.get(t.colour);
      c.y += i * 64;
      c.visible = true;
      mainGame.gameScreen.tableCards.addChild(c);
      c.moveToRelative(0, i * 64);
      tableCards.add(t);
    }
  }

  /**
  * Take a train card from the table at the given index
  * @param i the index of the train card to take
  * @return The train card object
  */
  public TrainCard takeTableCard(int i) {
    if (i < 0 || i >= 5) {
      return null;
    } else {
      TrainCard retval = tableCards.get(i);
      tableCards.remove(i);
      TrainCard newCard = drawTrainCard();
      tableCards.add(i, newCard);
      mainGame.gameScreen.tableCards.children.get(i).name = newCard.colour;
      mainGame.gameScreen.tableCards.children.get(i).texture = Game.trainCardImages.get(newCard.colour);
      return retval;
    }
  }

  /**
  * Output information about all the players to the console (used for debugging)
  */
  public void printPlayerInfo() {
    for (int i = 0; i < playerList.size(); i++) {
      System.out.println(playerList.get(i).name + " - " + playerList.get(i).colour + ":");
      System.out.println("  Score: " + playerList.get(i).score);
      System.out.println("  Trains left: " + playerList.get(i).trainsLeft);
      playerList.get(i).hand.printHand();
      System.out.println("  Routes:");
      for (int c = 0; c < playerList.get(i).routeCards.size(); c++) {
        RouteCard rc = playerList.get(i).routeCards.get(c);
        System.out.println("   " + rc.cityA + " to " + rc.cityB);
      }
    }
  }

  /**
  * Draw the top train card from the deck, if there are no cards left then reshuffle the discard pile back into the deck
  * @return The train card object
  */
  public TrainCard drawTrainCard() {
    if (trainCardDeck.size() == 0) {
      reshuffleTrainDeck();
    }
    return trainCardDeck.remove(0);
  }

  /**
  * Discard a train card from a players hand
  * @param t the train card object to be discarded
  */
  public void discardTrainCard(TrainCard t) {
    discardPile.add(t);
  }

  /**
  * Create map of America
  */
  public void createAmericaMap() {
    gameMap = new Map("America");

    System.out.println("Generating map");

    // Add cities with names at various coordinates
    gameMap.addCity(new City("Vancouver", 122, 105));
    gameMap.addCity(new City("Seattle", 113, 191));
    gameMap.addCity(new City("Portland", 84, 268));
    gameMap.addCity(new City("San Francisco", 62, 565));
    gameMap.addCity(new City("Los Angeles", 176, 715));
    gameMap.addCity(new City("Calgary", 320, 79));
    gameMap.addCity(new City("Salt Lake City", 358, 469));
    gameMap.addCity(new City("Las Vegas", 273, 648));
    gameMap.addCity(new City("Phoenix", 363, 713));
    gameMap.addCity(new City("Helena", 486, 288));
    gameMap.addCity(new City("Denver", 559, 515));
    gameMap.addCity(new City("Santa Fe", 549, 656));
    gameMap.addCity(new City("El Paso", 542, 791));
    gameMap.addCity(new City("Winnipeg", 661, 100));
    gameMap.addCity(new City("Deluth", 848, 270));
    gameMap.addCity(new City("Omaha", 782, 413));
    gameMap.addCity(new City("Kansas City", 817, 485));
    gameMap.addCity(new City("Oklahoma City", 784, 621));
    gameMap.addCity(new City("Dallas", 827, 740));
    gameMap.addCity(new City("Sault St. Marie", 1024, 176));
    gameMap.addCity(new City("Chicago", 1017, 365));
    gameMap.addCity(new City("Saint Louis", 948, 485));
    gameMap.addCity(new City("Little Rock", 924, 630));
    gameMap.addCity(new City("Houston", 878, 815));
    gameMap.addCity(new City("New Orleans", 1024, 797));
    gameMap.addCity(new City("Nashville", 1086, 551));
    gameMap.addCity(new City("Toronto", 1190, 207));
    gameMap.addCity(new City("Montreal", 1311, 69));
    gameMap.addCity(new City("Boston", 1418, 165));
    gameMap.addCity(new City("Pittsburgh", 1212, 342));
    gameMap.addCity(new City("New York", 1344, 273));
    gameMap.addCity(new City("Washington", 1349, 414));
    gameMap.addCity(new City("Raleigh", 1263, 513));
    gameMap.addCity(new City("Atlanta", 1168, 600));
    gameMap.addCity(new City("Charleston", 1309, 613));
    gameMap.addCity(new City("Miami", 1360, 856));

    // Add connections between cities with specified number of segments, colour and lateral offset
    gameMap.addRoute("Vancouver", "Seattle", 1, "any", 12.0);
    gameMap.addRoute("Vancouver", "Seattle", 1, "any", -12.0);
    gameMap.addRoute("Seattle", "Portland", 1, "any", 12.0);
    gameMap.addRoute("Seattle", "Portland", 1, "any", -12.0);
    gameMap.addRoute("Portland", "San Francisco", 5, "green", 12.0);
    gameMap.addRoute("Portland", "San Francisco", 5, "purple", -12.0);
    gameMap.addRoute("San Francisco", "Los Angeles", 3, "yellow", 20.0);
    gameMap.addRoute("San Francisco", "Los Angeles", 3, "purple", -4.0);
    gameMap.addRoute("Vancouver", "Calgary", 3, "any");
    gameMap.addRoute("Seattle", "Calgary", 4, "any");
    gameMap.addRoute("Seattle", "Helena", 6, "yellow", 8.0);
    gameMap.addRoute("Portland", "Salt Lake City", 6, "blue", true);
    gameMap.addRoute("San Francisco", "Salt Lake City", 5, "orange", true, 12.0);
    gameMap.addRoute("San Francisco", "Salt Lake City", 5, "white", true, -12.0);
    gameMap.addRoute("Los Angeles", "Las Vegas", 2, "any", true);
    gameMap.addRoute("Los Angeles", "Phoenix", 3, "any", true);
    gameMap.addRoute("Los Angeles", "El Paso", 7, "black", 16.0);
    gameMap.addRoute("Calgary", "Helena", 4, "any");
    gameMap.addRoute("Salt Lake City", "Helena", 3, "purple");
    gameMap.addRoute("Salt Lake City", "Las Vegas", 3, "orange", true);
    gameMap.addRoute("Salt Lake City", "Denver", 3, "red", 0.0);
    gameMap.addRoute("Phoenix", "Denver", 5, "white", true, 4.0);
    gameMap.addRoute("Phoenix", "Santa Fe", 3, "any");
    gameMap.addRoute("Phoenix", "El Paso", 3, "any");
    gameMap.addRoute("Calgary", "Winnipeg", 6, "white", true);
    gameMap.addRoute("Helena", "Winnipeg", 4, "blue");
    gameMap.addRoute("Helena", "Denver", 4, "green");
    gameMap.addRoute("Santa Fe", "Denver", 2, "any");
    gameMap.addRoute("Santa Fe", "El Paso", 2, "any");
    gameMap.addRoute("Winnipeg", "Sault St. Marie", 6, "any", true, -4.0);
    gameMap.addRoute("Winnipeg", "Deluth", 4, "black");
    gameMap.addRoute("Helena", "Deluth", 5, "orange", true);
    gameMap.addRoute("Helena", "Omaha", 5, "red");
    gameMap.addRoute("Denver", "Omaha", 4, "purple", true);
    gameMap.addRoute("Denver", "Kansas City", 4, "black");
    gameMap.addRoute("Denver", "Oklahoma City", 4, "red");
    gameMap.addRoute("Santa Fe", "Oklahoma City", 3, "blue");
    gameMap.addRoute("El Paso", "Oklahoma City", 5, "yellow", -6.0);
    gameMap.addRoute("El Paso", "Dallas", 5, "red", 4.0);
    gameMap.addRoute("El Paso", "Houston", 6, "green", 16.0);
    gameMap.addRoute("Deluth", "Omaha", 2, "any", 12.0);
    gameMap.addRoute("Deluth", "Omaha", 2, "any", -12.0);
    gameMap.addRoute("Omaha", "Kansas City", 1, "any", 12.0);
    gameMap.addRoute("Omaha", "Kansas City", 1, "any", -12.0);
    gameMap.addRoute("Kansas City", "Oklahoma City", 2, "any", 12.0);
    gameMap.addRoute("Kansas City", "Oklahoma City", 2, "any", -12.0);
    gameMap.addRoute("Oklahoma City", "Dallas", 2, "any", 12.0);
    gameMap.addRoute("Oklahoma City", "Dallas", 2, "any", -12.0);
    gameMap.addRoute("Dallas", "Houston", 1, "any", 12.0);
    gameMap.addRoute("Dallas", "Houston", 1, "any", -12.0);
    gameMap.addRoute("Deluth", "Sault St. Marie", 3, "any", -4.0);
    gameMap.addRoute("Sault St. Marie", "Toronto", 2, "ny", true);
    gameMap.addRoute("Deluth", "Toronto", 5, "purple", 4.0);
    gameMap.addRoute("Deluth", "Chicago", 3, "red", 4.0);
    gameMap.addRoute("Chicago", "Toronto", 3, "white", true);
    gameMap.addRoute("Omaha", "Chicago", 4, "blue");
    gameMap.addRoute("Kansas City", "Saint Louis", 2, "blue", 12.0);
    gameMap.addRoute("Kansas City", "Saint Louis", 2, "purple", -12.0);
    gameMap.addRoute("Oklahoma City", "Little Rock", 2, "any");
    gameMap.addRoute("Dallas", "Little Rock", 2, "any");
    gameMap.addRoute("Little Rock", "New Orleans", 3, "green");
    gameMap.addRoute("Houston", "New Orleans", 2, "any");
    gameMap.addRoute("Toronto", "Pittsburgh", 2, "any");
    gameMap.addRoute("Chicago", "Pittsburgh", 3, "orange", 0.0);
    gameMap.addRoute("Saint Louis", "Chicago", 2, "green", 0.0);
    gameMap.addRoute("Saint Louis", "Pittsburgh", 5, "green", 4.0);
    gameMap.addRoute("Saint Louis", "Nashville", 2, "any");
    gameMap.addRoute("Saint Louis", "Little Rock", 2, "any");
    gameMap.addRoute("Little Rock", "Nashville", 3, "white");
    gameMap.addRoute("Nashville", "Atlanta", 1, "any");
    gameMap.addRoute("New Orleans", "Atlanta", 4, "yellow", 12.0);
    gameMap.addRoute("New Orleans", "Atlanta", 4, "orange", -12.0);
    gameMap.addRoute("Sault St. Marie", "Montreal", 5, "black", true, -6.0);
    gameMap.addRoute("Montreal", "Boston", 2, "any", 12.0);
    gameMap.addRoute("Montreal", "Boston", 2, "any", -12.0);
    gameMap.addRoute("Toronto", "Montreal", 3, "any", true);
    gameMap.addRoute("Montreal", "New York", 3, "blue", 8.0);
    gameMap.addRoute("New York", "Boston", 2, "yellow", 16.0);
    gameMap.addRoute("New York", "Boston", 2, "red", -8.0);
    gameMap.addRoute("Pittsburgh", "New York", 2, "white", 12.0);
    gameMap.addRoute("Pittsburgh", "New York", 2, "green", -12.0);
    gameMap.addRoute("New York", "Washington", 2, "orange", 12.0);
    gameMap.addRoute("New York", "Washington", 2, "black", -12.0);
    gameMap.addRoute("Pittsburgh", "Washington", 2, "any");
    gameMap.addRoute("Pittsburgh", "Raleigh", 2, "any");
    gameMap.addRoute("Nashville", "Raleigh", 3, "black", true);
    gameMap.addRoute("Raleigh", "Washington", 2, "any", 12.0);
    gameMap.addRoute("Raleigh", "Washington", 2, "any", -12.0);
    gameMap.addRoute("Atlanta", "Raleigh", 2, "any", 4.0);
    gameMap.addRoute("Raleigh", "Charleston", 2, "any", true);
    gameMap.addRoute("Atlanta", "Charleston", 2, "any");
    gameMap.addRoute("Charleston", "Miami", 4, "purple", true);
    gameMap.addRoute("Atlanta", "Miami", 5, "blue");
    gameMap.addRoute("New Orleans", "Miami", 6, "red");

    System.out.println("Map loaded correctly");
  }

}

/**
* The map class that contains data on the locations of cites and the connections between them
*/
class Map extends Container {

  // Lists of cities and routes and the name of the map
  public ArrayList<City> cities = new ArrayList<City>();
  public ArrayList<Route> routes = new ArrayList<Route>();
  public String name;

  /**
  * Constructor for the map
  * @param name the name of the map
  */
  public Map(String name) {
    this.name = name;
    width = 1465;
    height = 942;
//    addChild(new Container(0, 0, width, height, Renderer.loadImage("assets/textures/background1.png")));
  }

  /**
  * Add a city to the map
  * @param city the city object to be added
  */
  public void addCity(City city) {
    cities.add(city);
//    addChild(city);
  }

  /**
  * Get the city object with the specified name
  * @param c the name of the city
  * @return The city object with the given name, null if no city is found
  */
  public City getCity(String c) {
    for (int i = 0; i < cities.size(); i++) {
      if (cities.get(i).name.equals(c)) {
        return cities.get(i);
      }
    }
    return null;
  }

  /**
  * Add a route to the map between two cities
  * @param route the route to be added containing the two cities, the colour, the number of segments and the lateral offset
  */
  public void addRoute(Route route) {
    routes.add(route);
//    addChild(route);
  }

  /**
  * Get the route object that contains the two given city objects
  * @param a the first city
  * @param b the second city
  * @return The route object that connects city a to city b, null if no such route exists on the map
  */
  public Route getRoute(City a, City b) {
    if (a.equals(b)) {
      return null;
    }
    for (int i = 0; i < routes.size(); i++) {
      if (routes.get(i).cityA.equals(a) && routes.get(i).cityB.equals(b)) {
        return routes.get(i);
      }
      if (routes.get(i).cityB.equals(a) && routes.get(i).cityA.equals(b)) {
        return routes.get(i);
      }
    }
    return null;
  }

  /**
  * Get a new route object that contains the cities corresponding to the city names given
  * @param cityA the name of the first city
  * @param cityB the name of the second city
  * @return A new route object that connects city a to city b, null if either of the cities can't be found on the map
  */
  public Route getRoute(String cityA, String cityB) {
    City a = null;
    City b = null;
    for (int i = 0; i < cities.size(); i++) {
      if (cities.get(i).name.equals(cityA)) {
        a = cities.get(i);
      }
      if (cities.get(i).name.equals(cityB)) {
        b = cities.get(i);
      }
    }
    if (a != null && b != null) {
      Route r = new Route(a, b, 0, "");
      return r;
    } else {
      System.out.println("Can't build route: " + cityA + " to " + cityB);
      return null;
    }
  }

  /**
  * Add a route to the map that connects city A to city B by the given number of segments and colour
  * @param cityA the name of the first city
  * @param cityB the name of the second city
  * @param length the number of segments that make up the route
  * @param colour the colour of the route to be added
  * @return True if the route was correctly added to the map, false if not
  */
  public boolean addRoute(String cityA, String cityB, int length, String colour) {
    Route r = getRoute(cityA, cityB);
    if (r != null) {
      r.length = length;
      r.colour = colour;
      addRoute(r);
      addChild(r);
      return true;
    } else {
      System.out.println("Can't build route: " + cityA + " to " + cityB);
      return false;
    }
  }

  /**
  * Add a route to the map that connects city A to city B by the given number of segments and colour and flip the arc of the route if the flip flag is enabled
  * @param cityA the name of the first city
  * @param cityB the name of the second city
  * @param length the number of segments that make up the route
  * @param colour the colour of the route to be added
  * @param flip whether the route should be flipped or not
  * @return True if the route was correctly added to the map, false if not
  */
  public boolean addRoute(String cityA, String cityB, int length, String colour, boolean flip) {
    Route r = getRoute(cityA, cityB);
    if (r != null) {
      r.length = length;
      r.colour = colour;
      r.flip = flip;
      addRoute(r);
      addChild(r);
      return true;
    } else {
      System.out.println("Can't build route: " + cityA + " to " + cityB);
      return false;
    }
  }

  /**
  * Add a route to the map that connects city A to city B by the given number of segments and colour, and flip the arc of the route if the flip flag is enabled, and move laterally by the specified offset
  * @param cityA the name of the first city
  * @param cityB the name of the second city
  * @param length the number of segments that make up the route
  * @param colour the colour of the route to be added
  * @param flip whether the route should be flipped or not
  * @param offset the lateral offset of the route (used if there are two routes that connect the same cities or if routes overlap)
  * @return True if the route was correctly added to the map, false if not
  */
  public boolean addRoute(String cityA, String cityB, int length, String colour, boolean flip, double offset) {
    Route r = getRoute(cityA, cityB);
    if (r != null) {
      r.length = length;
      r.colour = colour;
      r.flip = flip;
      r.offset = offset;
      addRoute(r);
      addChild(r);
      return true;
    } else {
      System.out.println("Can't build route: " + cityA + " to " + cityB);
      return false;
    }
  }

  /**
  * Add a route to the map that connects city A to city B by the given number of segments and colour, and move laterally by the specified offset
  * @param cityA the name of the first city
  * @param cityB the name of the second city
  * @param length the number of segments that make up the route
  * @param colour the colour of the route to be added
  * @param offset the lateral offset of the route (used if there are two routes that connect the same cities or if routes overlap)
  * @return True if the route was correctly added to the map, false if not
  */
  public boolean addRoute(String cityA, String cityB, int length, String colour, double offset) {
    Route r = getRoute(cityA, cityB);
    if (r != null) {
      r.length = length;
      r.colour = colour;
      r.offset = offset;
      addRoute(r);
      addChild(r);
      return true;
    } else {
      System.out.println("Can't build route: " + cityA + " to " + cityB);
      return false;
    }
  }

  /**
  * Claim the route at the given index for the given player colour
  * @param route the index of the route to claim
  * @param colour the unique colour of the player who's claiming the route
  */
  public void claimRoute(int route, String colour) {
    routes.get(route).claimRoute(colour);
  }

}

/**
* Simple class for train cards that specifies the colour of the train card
*/
class TrainCard {

  String colour;

  /**
  * Constructor for the train card object
  * @param colour the colour of the train card
  */
  public TrainCard(String colour) {
    this.colour = colour;
  }

}

/**
* A route card object that stores two city names and the score gained for connecting those two cities
*/
class RouteCard {

  String cityA;
  String cityB;
  int score;
  int mouseOverOffset = 0;

  /**
  * The route card constructor
  * @param cityA the name of the first city
  * @param cityB the name of the second city
  * @param score the score a player can gain by completing this route
  */
  public RouteCard(String cityA, String cityB, int score) {
    this.cityA = cityA;
    this.cityB = cityB;
    this.score = score;
  }

}

/**
* Route class that stores:
* -The two city objects to be connected
* -The number of segments in the route
* -The colour of the route
* -Who the route is claimed by, null by default
* -Whether the route arc should be flipped
* -The lateral offset of the route
*/
class Route extends Container{

  City cityA;
  City cityB;
  int length;
  String colour;
  String claimedBy;
  boolean flip = false;
  double offset = 0.0;
  ArrayList<Container> segmentList = new ArrayList<Container>();
  static Image upTexUnclaimed = Renderer.loadImage("assets/textures/segment1.png");
  static Image downTexUnclaimed = Renderer.loadImage("assets/textures/segment2.png");
  static Image upTexClaimed = Renderer.loadImage("assets/textures/segment1_claimed.png");
  static Image downTexClaimed = Renderer.loadImage("assets/textures/segment2_claimed.png");
  Image upTex = upTexUnclaimed;
  Image downTex = downTexUnclaimed;
  Image texture = upTex;

  /**
  * Constructor for route object
  * @param cityA the first city object
  * @param cityB the second city object
  */
  public Route(City cityA, City cityB) {
    this.cityA = cityA;
    this.cityB = cityB;
    visible = false;
  }

  /**
  * Constructor for route object
  * @param cityA the first city object
  * @param cityB the second city object
  * @param length the number of segments in the connection
  * @param colour the colour of the route
  */
  public Route(City cityA, City cityB, int length, String colour) {
    this.cityA = cityA;
    this.cityB = cityB;
    this.length = length;
    this.colour = colour;
    claimedBy = "null";
    visible = true;
  }

  /**
  * Constructor for route object
  * @param cityA the first city object
  * @param cityB the second city object
  * @param length the number of segments in the connection
  * @param colour the colour of the route
  * @param flip whether the route arc should be flipped or not
  */
  public Route(City cityA, City cityB, int length, String colour, boolean flip) {
    this(cityA, cityB, length, colour);
    this.flip = flip;
  }

  /**
  * Claim the route for the given colour
  * @param claimer the unique colour of the player claiming the route
  */
  public void claimRoute(String claimer) {
    upTex = upTexClaimed;
    downTex = downTexClaimed;
    texture = upTex;
    claimedBy = claimer;
    colour = claimer;
    changeTexture(texture);
  }

  /**
  * Check whether the mouse is currently contained inside any of the segments that make up the route
  * @return True if the mouse is contained, false if not
  */
  public boolean checkMouse() {
    for (int i = 0; i < segmentList.size(); i++) {
      if (segmentList.get(i).mouseOver()) {
        return true;
      }
    }
    return false;
  }

  /**
  * Change the texture of all the segments in the route to the specified image
  * @param tex the new texture to be used
  */
  public void changeTexture(Image tex) {
    for (int i = 0; i < segmentList.size(); i++) {
      segmentList.get(i).texture = tex;
      if (colour.equals("red")) {
        segmentList.get(i).colour = new Color(1.0f, 0.0f, 0.0f, 0.5f);
      } else if (colour.equals("blue")) {
        segmentList.get(i).colour = new Color(0.0f, 0.2f, 0.8f, 0.5f);
      } else if (colour.equals("green")) {
        segmentList.get(i).colour = new Color(0.0f, 1.0f, 0.0f, 0.5f);
      } else if (colour.equals("orange")) {
        segmentList.get(i).colour = new Color(0.9f, 0.4f, 0.0f, 0.5f);
      } else if (colour.equals("yellow")) {
        segmentList.get(i).colour = new Color(1.0f, 1.0f, 0.0f, 0.5f);
      } else if (colour.equals("purple")) {
        segmentList.get(i).colour = new Color(0.5f, 0.2f, 0.7f, 0.5f);
      } else if (colour.equals("black")) {
        segmentList.get(i).colour = new Color(0.0f, 0.0f, 0.0f, 0.7f);
      } else if (colour.equals("white")) {
        segmentList.get(i).colour = new Color(1.0f, 1.0f, 1.0f, 0.5f);
      } else if (colour.equals("any")) {
        segmentList.get(i).colour = new Color(0.3f, 0.3f, 0.3f, 0.5f);
      } else {
        segmentList.get(i).visible = false;
      }
    }
  }

  /**
  * Calculate the position and rotation of the individual segments that make up the route
  */
  public void calcSegments() {
    double difx = Math.abs(cityA.x - cityB.x);
    double dify = Math.abs(cityA.y - cityB.y);
    double size = Game.segmentSize;
    double n = length;
    double dist = Math.sqrt(Math.pow(difx, 2) + Math.pow(dify, 2));
    double ratio = (double)(n * size) / dist;
    double initialAngle = 0.0;
    double spacingX = 0.0;
    double spacingY = 0.0;
    double rAngle = Math.asin(dify / dist);
    if (ratio > 1.0) {
      initialAngle = Math.pow(n - 1, 0.285) * Math.acos(1.0 / ratio);
    }
    double changeAngle = 0.0;
    if (n > 1) {
      changeAngle = ((((n - 1) * Math.PI) - (2 * initialAngle)) / (n - 1));
    }
    if (flip) {
      initialAngle = -initialAngle;
      changeAngle = -changeAngle;
    }
    if (cityA.x - cityB.x < 0.0) {
      if (cityA.y - cityB.y > 0.0) {
        rAngle = -rAngle;
      }
    } else {
      if (cityA.y - cityB.y > 0.0) {
        rAngle = -(Math.PI - rAngle);
      } else {
        rAngle = Math.PI - rAngle;
      }
    }
    initialAngle += rAngle;
    double offsetx = 0.0;
    double offsety = 0.0;
    if (offset != 0.0) {
      offsetx = offset * Math.cos((Math.PI / 2) + rAngle);
      offsety = offset * Math.sin((Math.PI / 2) + rAngle);
    }
    if (ratio <= 1.0) {
      double spacing = ((dist - (n * size)) / n) / 2;
      spacingX = spacing * Math.cos(initialAngle);
      spacingY = spacing * Math.sin(initialAngle);
    }
    double dx = 0.0;
    double dy = 0.0;
    double x = 0.0;
    double y = 0.0;
    double r = 0.0;
    for (int i = 0; i < n; i++) {
      r = initialAngle + (changeAngle * i);
      if (i % 2 == 1) {
        r += Math.PI;
      }
      while (r > Math.PI) {
        r -= 2 * Math.PI;
      }
      Container c = new Container(0, 0, Game.segmentSize, Game.segmentHeight, r, texture, "segment " + i);
      dx = size * Math.cos(r);
      dy = size * Math.sin(r);
      c.x = (cityA.x) + x + (0.5 * dx) + (spacingX * i) + (spacingX) + offsetx;
      c.y = (cityA.y) + y + (0.5 * dy) + (spacingY * i) + (spacingY) + offsety;
      x += dx;
      y += dy;
      if (colour.equals("red")) {
        c.colour = new Color(1.0f, 0.0f, 0.0f, 0.5f);
      } else if (colour.equals("blue")) {
        c.colour = new Color(0.0f, 0.2f, 0.8f, 0.5f);
      } else if (colour.equals("green")) {
        c.colour = new Color(0.0f, 1.0f, 0.0f, 0.5f);
      } else if (colour.equals("orange")) {
        c.colour = new Color(0.9f, 0.4f, 0.0f, 0.5f);
      } else if (colour.equals("yellow")) {
        c.colour = new Color(1.0f, 1.0f, 0.0f, 0.5f);
      } else if (colour.equals("purple")) {
        c.colour = new Color(0.5f, 0.2f, 0.7f, 0.5f);
      } else if (colour.equals("black")) {
        c.colour = new Color(0.0f, 0.0f, 0.0f, 0.7f);
      } else if (colour.equals("white")) {
        c.colour = new Color(1.0f, 1.0f, 1.0f, 0.5f);
      } else if (colour.equals("any")) {
        c.colour = new Color(0.3f, 0.3f, 0.3f, 0.5f);
      } else {
        c.visible = false;
      }
      segmentList.add(c);
//      super.addChild(c);
    }
  }

}

/**
* Player class that contains information on the players name, colour, score, hand, routes, the number of trains they have, whether they're connected and whether they're human or AI
*/
class Player {

  String name;
  String colour;
  int score;
  PlayerHand hand = new PlayerHand();
  ArrayList<RouteCard> routeCards = new ArrayList<RouteCard>();
  int trainsLeft;
  boolean connected;
  String type = "human";

  /**
  * The player constructor
  */
  public Player() {
    this("", "");
    connected = false;
    type = "human";
  }

  /**
  * The player constructor
  * @param name the name of the player
  * @param colour the unique colour of the player
  */
  public Player(String name, String colour) {
    this.name = name;
    this.colour = colour;
    score = 0;
    trainsLeft = 48;
    hand = new PlayerHand();
    connected = true;
    type = "human";
  }

  /**
  * Process the players turn (only used for AI players)
  */
  public void process() {
    System.out.println("Invalid process command on " + name);
  }

  /**
  * Process the players turn without sleep commands (only used for AI players)
  */
  public void quickRun() {
    System.out.println("Invalid compute command on " + name);
  }

  /**
  * Remove a number of cards of the given colour from the players hand
  * @param colour the colour of the cards to be removed
  * @param num the number of cards to be removed
  */
  public void removeCards(String colour, int num) {
    System.out.println("Removing " + num + " of " + colour);
    // If the colour is "any" then find the smallest number of cards of the same colour that are greater than or equal to the number of cards to be removed, this includes rainbow cards
    if (colour.equals("any")) {
      int lowest = hand.hand.size() + 1;
      String lowestColour = "";
      for (int i = 0; i < 8; i++) {
        if (countRawColour(Game.colourList[i]) >= num && countRawColour(Game.colourList[i]) < lowest && countRawColour(Game.colourList[i]) > 0) {
          lowest = countRawColour(Game.colourList[i]);
          lowestColour = Game.colourList[i];
        }
      }
      if (lowestColour.equals("")) {
        for (int i = 0; i < 8; i++) {
          if (countColour(Game.colourList[i]) >= num && countColour(Game.colourList[i]) < lowest && countRawColour(Game.colourList[i]) > 0) {
            lowest = countColour(Game.colourList[i]);
            lowestColour = Game.colourList[i];
          }
        }
      }
      removeCards(lowestColour, num);
    } else {
      for (int i = 0; i < hand.hand.size(); i++) {
        if (hand.hand.get(i).colour.equals(colour) && num > 0) {
          mainGame.game.discardTrainCard(hand.hand.get(i));
          hand.hand.remove(i);
          num -= 1;
          i -= 1;
        }
      }
      int i = 0;
      while (num > 0 && hand.hand.size() > i) {
        if (hand.hand.get(i).colour.equals("multi")) {
          mainGame.game.discardTrainCard(hand.hand.get(i));
          hand.hand.remove(i);
          i -= 1;
          num -= 1;
        }
        i += 1;
      }
    }
  }

  /**
  * Count the number of cards in the players hand of the given colour, including rainbow cards
  * @param colour the colour of the cards to count
  * @return The number of cards of the given colour, including rainbow cards
  */
  public int countColour(String colour) {
    int count = 0;
    for (int i = 0; i < hand.hand.size(); i++) {
      if (hand.hand.get(i).colour.equals(colour) || hand.hand.get(i).colour.equals("multi")) {
        count += 1;
      }
    }
    return count;
  }

  /**
  * Count the number of cards in the players hand of the given colour, NOT including rainbow cards
  * @param colour the colour of the cards to count
  * @return The number of cards of the given colour, NOT including rainbow cards
  */
  public int countRawColour(String colour) {
    int count = 0;
    for (int i = 0; i < hand.hand.size(); i++) {
      if (hand.hand.get(i).colour.equals(colour)) {
        count += 1;
      }
    }
    return count;
  }

  /**
  * Get the most frequent colour in the players hand
  * @return The most frequent colour
  */
  public String highestColour() {
    int highest = 0;
    String highestColour = "";
    for (int i = 0; i < 8; i++) {
      if (countColour(Game.colourList[i]) > highest) {
        highest = countColour(Game.colourList[i]);
        highestColour = Game.colourList[i];
      }
    }
    return highestColour;
  }

}

/**
* Player hand class that stores a list of train cards as well as a function to correctly render the players hand to the game screen
*/
class PlayerHand extends Container {

  ArrayList<TrainCard> hand = new ArrayList<TrainCard>();
  Container card = new Container();
  TextField cardText;

  /**
  * Player hand constructor
  */
  public PlayerHand() {
    name = "Player hand";
    hand.clear();
    card.visible = true;
    card.width = 192;
    card.height = 128;
    card.anchorX = 0.5;
    card.anchorY = 0.5;
    card.rotation = Math.PI / 2.0;
    cardText = new TextField("", 0, 0, 16);
    cardText.visible = false;
    card.addChild(cardText);
    this.addChild(card);
    this.visible = true;
  }

  /**
  * Output the contents of the hand to the console
  */
  public void printHand() {
    System.out.println("  Hand:");
    for (int i = 0; i < hand.size(); i++) {
      System.out.println("    " + hand.get(i).colour);
    }
  }

  /**
  * Add a card of the given colour to the hand
  * @param colour the colour of the card to add
  */
  public void addCard(String colour) {
    hand.add(new TrainCard(colour));
  }

  /**
  * Add a card object to the hand
  * @param card the train card object to be added
  */
  public void addCard(TrainCard card) {
    hand.add(card);
  }

  /**
  * Draw the player hand by moving and rendering the card object for each card in the hand
  * @param g the graphics environment, used by Java's 2D graphics framework
  * @param r the rendering object to be used, specifies information about the rendering environment
  */
  public void draw(Graphics g, Renderer r) {
    double spacing = 64.0;
    if (hand.size() >= 6) {
      spacing = 64.0 * (6.0 / hand.size());
    }
    moveTo((((mainGame.wWidth) - (hand.size() * spacing)) / 2) - 112, mainGame.wHeight - 16);
    for (int i = 0; i < hand.size(); i++) {
      card.texture = Game.trainCardImages.get(hand.get(i).colour);
      card.moveToRelative(0 + (i * spacing), 0);
      card.draw(g, r);
    }
  }

}

/**
* An AI player class that extends the regular player class
*/
class AIPlayer extends Player implements Runnable {

  Game game;

  Random rand;

  /**
  * AI player constructor
  * @param name the name of the player
  * @param colour the colour of the player
  * @param game the game object that the player is currently connected to
  */
  public AIPlayer(String name, String colour, Game game) {
    super(name, colour);
    this.game = game;
    rand = new Random(game.seed);
    type = "AI";
  }

  /**
  * Process the turn for the AI player
  */
  public void process() {
    try {
      Thread thread = new Thread(this);
      thread.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
  * The runnable function that simulates the AI players turn, it sleeps for a random amount of time to appear more human
  */
  public void run() {
    try {
      Thread.sleep((rand.nextInt(3) + 2) * 1000);
      computeTurn();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
  * Simulate the AI players turn without sleeping, used for loading a game where the whole game has to be simulated at once but not in real time
  */
  public void quickRun() {
    rand.nextInt(3);
    computeTurn();
  }

  /**
  * Simulate the AI players turn.
  * This function finds the shortest routes between all the cities on the route cards the player has and figures out if any of the city connections can be claimed using the train cards the player currently has. If no routes can be claimed then the function will figure out what colour cards the player needs most and pick those cards from the table. If that colour isn't present on the table then the player will draw cards randomly either from the table or the deck.
  */
  public void computeTurn() {
    ArrayList<Route> possibleRoutes = new ArrayList<Route>();
    String[] colours = {"red", "orange", "blue", "yellow", "purple", "green", "black", "white"};
    int[] colourFreq = {0, 0, 0, 0, 0, 0, 0, 0};
    for (int i = 0; i < routeCards.size(); i++) {
      ArrayList<Route> newRoutes = game.getShortestRoute(routeCards.get(i), colour);
//        System.out.println("  =" + newRoutes.size());
      for (int r = 0; r < newRoutes.size(); r++) {
        if (!possibleRoutes.contains(newRoutes.get(r)) && newRoutes.get(r).claimedBy.equals("null")) {
          if (!newRoutes.get(r).colour.equals("any")) {
            int index = Arrays.asList(colours).indexOf(newRoutes.get(r).colour);
            if (index >= 0 && index < 8) {
              colourFreq[index] += newRoutes.get(r).length;
            }
          }
          if (countColour(newRoutes.get(r).colour) >= newRoutes.get(r).length) {
            possibleRoutes.add(newRoutes.get(r));
          }
        }
      }
    }

    if (possibleRoutes.size() > 0) {
      game.claimRoute(possibleRoutes.get(0), colour);
    } else {
      String bestColour = colours[0];
      int bestColourVal = colourFreq[0];
      for (int i = 1; i < 8; i++) {
        if (colourFreq[i] > bestColourVal) {
          bestColour = colours[i];
          bestColourVal = colourFreq[i];
        }
      }
      takeCard(bestColour);
      takeCard(bestColour);
    }

    game.nextPlayer();
  }

  /**
  * Take a card of the given colour from the table. If that card doesn't exist than randomly draw cards either from the table or the deck.
  * @param colour the colour of the train card to be taken
  */
  public void takeCard(String colour) {
    for (int i = 0; i < game.tableCards.size(); i++) {
      if (game.tableCards.get(i).colour.equals(colour)) {
        game.updateText = game.playerList.get(game.curPlayer).name + " took a " + game.tableCards.get(i).colour + " card from the table";
        hand.addCard(game.takeTableCard(i));
        return;
      }
    }
    if (rand.nextInt(2) == 0) {
      for (int i = 0; i < game.tableCards.size(); i++) {
        if (game.tableCards.get(i).colour.equals("multi")) {
          game.updateText = game.playerList.get(game.curPlayer).name + " took a " + game.tableCards.get(i).colour + " card from the table";
          hand.addCard(game.takeTableCard(i));
          return;
        }
      }
      game.updateText = game.playerList.get(game.curPlayer).name + " took a card from the deck";
      hand.addCard(game.drawTrainCard());
      return;
    } else {
      game.updateText = game.playerList.get(game.curPlayer).name + " took a card from the deck";
      hand.addCard(game.drawTrainCard());
      return;
    }
  }

}

/**
* City class that specifies the name of the city as well as aspcets of the container object used for rendering and interface
*/
class City extends Container {

  String name;
  static Image upTex = Renderer.loadImage("assets/textures/testTex1.png");
  static Image downTex = Renderer.loadImage("assets/textures/testTex2.png");

  /**
  * Constructor for the city object
  * @param name the name of the city
  */
  public City(String name) {
    this.name = name;
    width = 48;
    height = 48;
    anchorX = 0.5;
    anchorY = 0.5;
    visible = true;
    texture = upTex;
  }

  /**
  * Constructor for the city object
  * @param name the name of the city
  * @param x the x coordinate of the city on the map
  * @param y the y coordinate of the city on the map
  */
  public City(String name, int x, int y) {
    this(name);
    this.x = x;
    this.y = y;
  }

}
