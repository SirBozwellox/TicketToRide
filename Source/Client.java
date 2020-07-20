/**
* @author Jack Gloyens 201164129 <jackgloyens@gmail.com>
* A Client and ClientInstance class that can send and receive data to and from the server via a socket
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

/**
* The main client class
*/
public class Client {
	/**
	* The client classes main function which creates and runs a client instance
	* @param args arguments passed via the console, not used during execution
	* @throws Exception When the program can't establish a connection to the server
	*/
	public static void main(String[] args) throws Exception {
		ClientInstance client = new ClientInstance();
		client.run();
	}
}

/**
* The client instance class which initialises and manages the socket connection to the server
*/
class ClientInstance {
  private int portNumber = 5555;
  private Socket socket = null;
	private BufferedReader in;
	private PrintWriter out;

  private boolean isAllowedToChat = false;
	public boolean isServerConnected = false;
	public String username;
	public String colour;

	private static final String ipPattern =
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	public GameManager gameManager;

	/**
	* The client instance constructor
	* @param username the name of the player that's displayed to other users while in a game
	* @param colour the unique colour identifier of the user, provided by the server upon connection
	*/
	public ClientInstance(String username, String colour) {
		this.username = username;
		this.colour = colour;
	}

	/**
	* The default constructor of the client instance object
	*/
	public ClientInstance() {
		username = "";
		colour = "";
	}

  /**
  * Main runnable method for the thread
  */
  public void run(){
		gameManager = new GameManager();
		gameManager.start();
		System.out.println("Game manager initialised");
//		outMessage();
		inMessage();
	}



  /**
  * Method to connect client to server
	* @param serverAddress the IP address and port of the server that the program wants to connect to
	* @param password the password the user has provided, this will be checked against the server password upon connection
  */
	public void connect(String serverAddress, String password) {
		Pattern pattern = Pattern.compile(ipPattern);
		if (pattern.matcher(serverAddress).matches()) {
			mainGame.joinScreen.lblJoinServer.text = "Attempting to connect";
			mainGame.renderView.render();
			System.out.println("Attempting to connect to " + serverAddress);
			try{
	      socket = new Socket(serverAddress, portNumber); // Create new socket and input and output streams
				System.out.println("Socket created");
	      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//	      out = new PrintWriter(socket.getOutputStream(), true);
				sendServerMessage("password;" + password + ")");
	      isServerConnected = true;
				System.out.println("Server connection established");
	    }
	    catch (IOException e){
				System.out.println("Server connection failure");
				mainGame.joinScreen.lblJoinServer.text = "Failed to connect to server";
	      e.printStackTrace();
	    }
		} else {
			System.out.println("Invalid server address");
			mainGame.joinScreen.lblJoinServer.text = "Invalid IP address";
		}
	}

	/**
	* Send a single message to the server
	* @param msg the string message to be sent to the server
	*/
	public void sendServerMessage(String msg) {
		System.out.println("Send server message");
		out.println(msg);
	}

  /**
  * Send outgoing messages to the server that the user types into the command console
  */
  private void outMessage(){
    Thread sender = new Thread(new Runnable(){
      public void run(){
        while (isServerConnected){
					System.out.println("Out message");
          out.println(getInput(null));
        }
      }
    });
    sender.start();
		System.out.println("Output manager disconnected");
  }

  /**
  * Receive incoming messages from the server
  */
  private void inMessage(){
    Thread listener = new Thread(new Runnable(){
      public void run(){
        while (isServerConnected){
          String line = null;
          try{
            line = in.readLine(); // Get input from the server
            if(line == null){
              isServerConnected = false;
              System.out.println("You disconnected from the server");
              disconnect();
              break;
            }
						System.out.println("Server: " + line);
						if (gameManager.getCommand(line).equals("colour")) {
							colour = gameManager.getValue(line, "colour");
							System.out.println("Assigned colour " + colour);
						} else if (gameManager.getCommand(line).equals("invalidPassword")) {
							disconnect();
						} else {
							gameManager.pushUpdate(line);
						}
          }
          catch (IOException e){
            isServerConnected = false;
            System.out.println("Server disconnected suddenly");
						mainGame.gameScreen.visible = false;
						mainGame.joinScreen.visible = false;
						try {
							Thread.sleep(50);
						} catch (Exception se) {
							e.printStackTrace();
						}
						mainGame.game = new Game();
						gameManager = new GameManager();
						mainGame.renderView.removeChild(mainGame.gameScreen);
						mainGame.gameScreen = new GameScreen();
						mainGame.renderView.addChild(mainGame.gameScreen);
						mainGame.gameScreen.visible = false;
						mainGame.joinScreen.visible = true;
						mainGame.joinScreen.findServer.visible = true;
						mainGame.joinScreen.inGame.visible = false;
						disconnect();
						isServerConnected = false;
						mainGame.client = new ClientInstance();
						mainGame.joinScreen.lblJoinServer.text = "Server disconnected suddenly";
						System.out.println("Game reset");
            break;
          }
        }
      }
    });
    listener.start();
		System.out.println("Input manager disconnected");
  }

  /**
  * Method to safely disconnect from the server
  */
  void disconnect(){
    try{
      socket.close();
      System.out.println("Disconnect successful");
//      System.exit(0);
    }
    catch (IOException e){
      e.printStackTrace();
    }
  }

  /**
  * Method to get input from the user
  * @param prompt the prompt for what information the user needs to enter
  * @return The input the user entered
  */
  private String getInput(String prompt){
    String message = null;
    try{
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      if (prompt != null){
        System.out.println(prompt);
      }
      message = reader.readLine();
    }
    catch (IOException e){
      e.printStackTrace();
    }
    return message;
  }

}
