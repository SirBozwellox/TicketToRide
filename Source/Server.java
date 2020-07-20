/**
* @author Jack Gloyens 201164129 <jackgloyens@gmail.com>
* The server and server socket class that control incoming and outgoing messages to and from clients connected via a socket
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.io.FileWriter;
import java.io.IOException;

/**
* Main server class that extends the thread class to make it a runnable object
*/
public class Server extends Thread {

  private int portNumber = 5555;
  private ServerSocket serverSocket;
  private PrintWriter clientWriter;
  public static ArrayList<Session> sessionList = new ArrayList<Session>(); // The list of session objects
  private static ArrayList<Socket> socketList = new ArrayList<Socket>(); // The list of client sockets
  private static ArrayList<Thread> threadList = new ArrayList<Thread>(); // The list of threads which run the sessions for each client
  public static ArrayList<String> availableColours = new ArrayList<String>();
  public static String IPAddress;
  public static String password = "";
  public int autoSaveCount = 6000;

  long serverStart; // The server start time

  public static GameManager gameManager = new GameManager();
  private static InputManager inputManager = new InputManager();
  public static Server server;

  /**
  * The server classes main function where everything's initialised
  * @param args a list of string arguments passed by the console, not used during execution
  * @throws IOException if the server unexpectedly closes a client connection
  */
  public static void main (String[] args) throws IOException{
    Collections.addAll(availableColours, "red", "green", "blue", "yellow", "black");
		server = new Server();
    server.start();
    gameManager.start();
    inputManager.start();
		server.startServer();
	}

  /**
  * Default server constructor
  */
  public Server() {
    mainGame.serverScreen = new ServerScreen();
    mainGame.serverScreen.visible = true;
    mainGame.renderView.addChild(mainGame.serverScreen);
    mainGame.renderView.frame.addMouseListener(mainGame.ml);
    mainGame.renderView.renderer.addKeyListener(mainGame.kl);
  }

  /**
  * Method that:
  * -Creates new server socket using the specified port port number
  * -Sets the server start time
  * -Accepts incoming connections then creates new sessions and threads and adds them to the appropriate lists
  * @throws IOException if a socket can't be correctly initalised then the server will safely shutdown
  */
  void startServer() throws IOException{
		serverSocket = new ServerSocket(portNumber);
    serverStart = System.currentTimeMillis() / 1000;
    IPAddress = InetAddress.getLocalHost().getHostAddress();
    System.out.println("Server started on " + IPAddress);
		try{
			while (true){
				socketList.add(serverSocket.accept()); // Add socket number to end of list
        sessionList.add(new Session(socketList.get(socketList.size() - 1))); // Create new session and add to list
				threadList.add(new Thread(sessionList.get(sessionList.size() - 1))); // Create new thread and add to list
				threadList.get(threadList.size() - 1).start(); // Start the thread
			}
		}
		catch (Exception e){
			System.out.println(e.getMessage());
		}
    finally{
      shutDown();
    }
  }

  /**
  * Runnable function that process update requests and sends them out to all the clients currently connected
  */
  public void run()
  {
    String message;
    ArrayList<String> updates = new ArrayList<String>();
    try {
      while(true) {
        sleep(10);
        updates = gameManager.getLatestUpdates();
        for (int i = 0; i < updates.size(); i++) {
          sendAll(updates.get(i));
        }
        mainGame.updateMouse();
        mainGame.serverScreen.process();
        mainGame.renderView.renderer.repaint();
        mainGame.ml.process();
        mainGame.kl.process();
        // Pause the game if there are no human players currently connected
        if (mainGame.game.checkAllDisconnected() && mainGame.game.inGame && !mainGame.game.paused) {
          gameManager.pushUpdate("pause;");
          System.out.println("No human players remaining, game paused");
        }
        autoSaveCount -= 1;
        if (autoSaveCount <= 0) {
          gameManager.saveGame("save1");
          autoSaveCount = 6000;
        }
      }
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  /**
  * Method that safely shuts the server down
  */
  public void shutDown(){
		try{
			serverSocket.close(); // Close server socket
			System.out.println("Server shut down correctly");
		}
		catch (Exception e){
      System.out.println("Shutdown error");
		}
	}

  /**
  * Input manager class that can process updates from the command line
  */
  static class InputManager extends Thread {

    String message;

    /**
    * Default input manager constructor
    */
    public InputManager() {

    }

    /**
    * Runnable function that reads inputs from the command line and pushes them to the game manager
    */
    public void run() {
      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
          message = reader.readLine();
          System.out.println("Server requested update:");
          gameManager.pushUpdate(message);
        }
      } catch (Exception e) {
        System.out.println(e);
      }
    }

  }

  /**
  * Class that handles the sessions for individual clients
  */
  class Session implements Runnable{

    private Socket socket; // The socket the client connected on
    BufferedReader in = null; // Input reader
    PrintWriter out = null; // Output writer

    String username = ""; // Clients username
    String colour = "";

    Long connectionTime; // Time the client connected

    Player playerObj;

    /**
    * Get the username of the client using this socket
    * @return The username the client is using
    */
    public String getUsername(){
      return username;
    }

    /**
    * Get the colour given to the client using this socket
    * @return The colour given to the client by the server
    */
    public String getColour(){
      return colour;
    }

    /**
    * Get the socket for this session
    * @return The socket the client connected on
    */
    public Socket getSocket(){
      return socket;
    }

    /**
    * Constructor for the session object, sets the socket and connection time for the client
    * @param socket the socket the client connected on
    */
    Session (Socket socket){
      this.socket = socket;
      connectionTime = System.currentTimeMillis() / 1000;
    }

    /**
    * Sends a message to the client computer
    * @param message the message to be sent
    */
    public void sendMessage(String message){
      out.println(message); out.flush();
    }

    /**
    * Main method that the thread runs
    */
    public void run(){
      try{
        createStreams();
        // If the game isn't full, otherwise close the incoming connection
        if (availableColours.size() > 0) {
          // Assign the player a colour from the list of unused colours
          colour = availableColours.remove(0);
          sendMessage("colour;" + colour + ")");
          playerObj = new Player();
          playerObj.colour = colour;
          // Switch to quick update mode and send the client the entire list of updates to simulate the game up to that point
          sendMessage("quickUpdate;");
          for (int i = 0; i < gameManager.updateList.size(); i++) {
            if (!gameManager.getCommand(gameManager.updateList.get(i)).equals("slowUpdate")) {
              sendMessage(gameManager.updateList.get(i));
            }
          }
          // Switch back to slow update mode
          sendMessage("slowUpdate;");
          // Add the player to the game
          gameManager.pushUpdate("addPlayer;name(" + colour + ")colour(" + colour + ")");
          getMessages();
        } else {
          closeConnection();
        }
      }
      catch (IOException e){
        System.out.println("User disconnected suddenly");
      }
      finally{
        closeConnection();
      }
    }

    /**
    * Create the input and output streams
    */
    private void createStreams() {
			try{
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				clientWriter = out;
				System.out.println("New connection is established");
			}
			catch (IOException e){
        System.out.println("Connection not successful");
			}
		}

    /**
    * Ask the client for the username they want to use
    */
		private void askUsername() {
			sendMessage("Please enter a username"); // Send request to user
      String tempUsername = "";
      String tempColour = "";
      do {
        try{
          tempUsername = in.readLine(); // Wait for user to send input
          if (checkUsername(tempUsername)) // Check if the username exists
          {
            sendMessage("That username is already taken");
          }
        }
  			catch (IOException e){
          System.out.println("Error reading client data");
  			}
      } while (checkUsername(tempUsername)); // Keep asking until the username they want isn't already in use
      try {
        sendMessage("Please enter a colour");
        colour = in.readLine(); // Wait for user to send input
      } catch (Exception e) {
        e.printStackTrace();
      }
      username = tempUsername;
			if (username == null) return;
			sendMessage("connected");
			System.out.println(username + " has entered the game as " + colour);
      sendAllFromUser(username, username + " has entered the game as " + colour); // Tell all other users that this user has joined
      for (int i = 0; i < gameManager.updateList.size(); i++) {
        sendMessage(gameManager.updateList.get(i));
      }
      gameManager.pushUpdate("addPlayer;name(" + username + ")colour(" + colour + ")");
		}

    /**
    * Method to get input from client
    * @throws IOException if the session unexpectedly disconnects and closes
    */
    private void getMessages() throws IOException {
      String line;
      while (true){
        line = in.readLine(); // Read input from clients computer
        if (line == null) {
          break;
        } else {
          if (gameManager.checkValidCommand(line)) {
            if (gameManager.getCommand(line).equals("changeColour")) {
              playerObj.colour = gameManager.getValue(line, "colour");
            } else if (gameManager.getCommand(line).equals("password")) {
              if (gameManager.getValue(line, "password").equals(Server.password)) {
                System.out.println("Password matches");
              } else {
                System.out.println("Invalid password");
                sendMessage("invalidPassword;");
                socket.close();
              }
            } else {
              System.out.println(username + " requested update:");
              System.out.println("  " + line);
              gameManager.pushUpdate(line);
              if (gameManager.getCommand(line).equals("changeName")) {
                if (gameManager.getValue(line, "colour").equals(colour)) {
                  playerObj.name = gameManager.getValue(line, "username");
                  username = playerObj.name;
                }
              }
            }
          } else {
            System.out.println(username + " sent invalid request:");
            System.out.println("  " + line);
          }
        }

      }
    }

    /**
    * Method to safely close the connection to the client computer
    */
    void closeConnection(){
      gameManager.pushUpdate("removePlayer;colour(" + colour + ")");
      try{
        socket.close(); // Close the socket
        System.out.println(username + " disconnected");
//        sendAllFromUser(username, username + " has left the chat");
        removeUser(colour); // Remove user from lists
        mainGame.game.removePlayer(playerObj);
      }
      catch(IOException e){
        System.out.println("User did not disconnect correctly");
      }
    }

  }

  /**
  * Method to send messages to every user connected to the server except the user who sent the message
  * @param user the username of the user who sent the message
  * @param message the message to be sent
  */
  public void sendAllFromUser(String user, String message)
  {
    for (int count = 0; count < sessionList.size(); count ++)
    {
      if (sessionList.get(count).getUsername() != user)
      sessionList.get(count).sendMessage(message);
    }
  }

  /**
  * Method to send messages to every user connected to the server
  * @param message the message to be sent
  */
  public void sendAll(String message) {
    for (int count = 0; count < sessionList.size(); count++) {
      sessionList.get(count).sendMessage(message);
    }
  }

  /**
  * Method to check if the username a client wants to use is already in use or not
  * @param username the username the client wants to use
  * @return True if the username is in use or false if not
  */
  public boolean checkUsername(String username)
  {
    boolean retval = false;
    for (int count = 0; count < sessionList.size(); count ++)
    {
      if (sessionList.get(count).getUsername().equals(username))
      {
        retval = true;
      }
    }
    return retval;
  }

  /**
  * Method to remove user from the session list, socket list and thread list
  * @param colour the unique colour of the client to be removed
  * @return True if the user was removed correctly, false if the user could not be found
  */
  public static boolean removeUser(String colour)
  {
    int index = -1;
    for (int count = 0; count < sessionList.size(); count ++)
    {
      if (sessionList.get(count).getColour().equals(colour))
      {
        index = count;
      }
    }
    if (index != -1)
    {
      System.out.println("Player session closed");
      availableColours.add(0, colour);
      sessionList.remove(index);
      socketList.remove(index);
      threadList.remove(index);
      return true;
    }
    return false;
  }

}
