import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.StringTokenizer;

public class ServiceTheClient {
	private final String CARRIAGE_RETURN_NEW_LINE = "\r\n";
	private InputStream inFromClient;
	private OutputStream outToClient;

	public ServiceTheClient() {}

	/*
	Handle all the logic
	 */
	public void serviceTheClient(Socket socket) {
		boolean verbose = true;
		try {

			String clientIp = socket.getInetAddress().getHostAddress();
			System.out.println("IP: " + clientIp);
			System.out.println("PORT: " + socket.getPort());

			inFromClient = socket.getInputStream();
			outToClient = socket.getOutputStream();

			while (verbose) {
				String messageFromClient = receiveMsgFromClient(inFromClient).trim();
				StringTokenizer stringTokenizer = new StringTokenizer(messageFromClient);
				String command = stringTokenizer.nextToken();

				String username;
//				control_IMAV_Command_On_Active_Clients();
				if (validateCommand(command)) {
					switch (command) {
						case "JOIN":
							username = stringTokenizer.nextToken(":");
							handle_JOIN_Command(outToClient, stringTokenizer, socket, username);
							break;
						case "DATA":
							//If message is over 250 characters send an J_ER
							username = stringTokenizer.nextToken(":");
							if (!checkUsernameForSocket(username, socket)) {
								serverSendMessage(socket, "J_ER 500: Not your username." + CARRIAGE_RETURN_NEW_LINE);
								break;
							}
							if (messageFromClient.length() > (250 + command.length() + username.length())) {
								serverSendMessage(socket, "J_ER 500: Message too long." + CARRIAGE_RETURN_NEW_LINE);
								break;
							}
							handle_DATA_Command(stringTokenizer, username);
							break;
						case "IMAV":
//							handle_IMAV_Command(socket);
							break;
						case "QUIT":
							handle_QUIT_command(outToClient, inFromClient, socket);
							verbose = false;
							break;
					}

//					delete_Clients_not_alive();

				} else {

					serverSendMessage(socket, "J_ER 500: UNKNOWN COMMAND." + CARRIAGE_RETURN_NEW_LINE);

				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	Method that checks if the username is the same as the client initially started with.
	So he can't trick the server by writing: "DATA ->not_initial_username<-: text he want to send"
	 */
	private boolean checkUsernameForSocket(String username, Socket socket) {
		for (HashMap client : TCP_Server.clients) {
			if (client.get("username").equals(username.substring(1))) {
				return true;
			}
		}
		return false;
	}

	/*
	Builds a string according to the protocol.
	Then sends to all clients.
	 */
	private void handle_DATA_Command(StringTokenizer stringTokenizer, String username) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("DATA ");

		stringBuilder.append(username.substring(1) + ": ");

		while (stringTokenizer.hasMoreTokens()) {
			stringBuilder.append(stringTokenizer.nextToken() + " ");
		}

		System.out.println(stringBuilder);

		String messageBackToClient = new String(stringBuilder);

		sendMsgToAll(messageBackToClient);
	}

	/*
	Iterates through every client in the list.
	For each client it creates a new socket and set it as the clients socket.
	 */
	private void sendMsgToAll(String messageBackToClient) {
		for (HashMap client : TCP_Server.clients) {
			Socket socket = (Socket) client.get("socket");
			serverSendMessage(socket, messageBackToClient);
		}
	}

	/*
	Create a outputstream for the socket. The string from the parameter is being generated as a byte[]
	 */
	private void serverSendMessage(Socket socket, String messageBackToClient) {
		try {
			//OutputStream outputStream = socket.getOutputStream();
			socket.getOutputStream().write(messageBackToClient.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	Receives the message from the client.
	 */
	private String receiveMsgFromClient(InputStream inFromClient) {
		try {
			byte[] bytes = new byte[1024];

			inFromClient.read(bytes);

			return new String(bytes);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Could not read data from client...";
	}

	/*
	Iterates through the list to find the client that wants to quit and removes the client from the list.
	 */
	private void handle_QUIT_command(OutputStream outToClient, InputStream inFromClient, Socket socket) {
		//delete client from active list
		for (HashMap client : TCP_Server.clients) {
			if (client.get("socket").equals(socket)) {
				//send message to all that the person left the chat room
				TCP_Server.clients.remove(client);

				sendMsgToAll("IN [SERVER]: [" + client.get("username") + "] left the chat room.");
				break;
			}
		}
	}

	/*
	Method checks if the username is valid.
	The it splits the JOIN message the client sent and saves client in an list.
	When the client is saved, a list of all active clients is sent out to all clients.
	 */
	private void handle_JOIN_Command(OutputStream outToClient, StringTokenizer stringTokenizer, Socket socket, String username) {
		String[] splitUsernameOnComma = username.split(",");
		String usernameAlteredForJoinCommand = splitUsernameOnComma[0].substring(1);

		//sends message back to client if username isn't valid.
		if (!validateUsername(usernameAlteredForJoinCommand)) {
			serverSendMessage(socket, "J_ER 500: Username not OK. " +
				"Username may only be max 12 chars long, only letters, digits, ‘-‘ and ‘_’ allowed." + CARRIAGE_RETURN_NEW_LINE);
		}
		if (containUsername(usernameAlteredForJoinCommand)) {
			serverSendMessage(socket, "J_ER 500: Username already in use." + CARRIAGE_RETURN_NEW_LINE);
		} else {

			String[] splitAddressOnColon = stringTokenizer.nextToken().split(":");
			String server_ip = splitUsernameOnComma[1];

			String server_port = splitAddressOnColon[0];

			// save person<HashMap> to an arraylist with clients.
			HashMap<String, Object> client = new HashMap<>();
			client.put("username", usernameAlteredForJoinCommand);
			client.put("server_ip", server_ip);
			client.put("server_port", server_port);
			client.put("imav", true);
			client.put("socket", socket);

			// add client to arraylist of active clients
			TCP_Server.clients.add(client);

			//J_OK  Client is accepted
			System.out.println("\n[" + usernameAlteredForJoinCommand + "] joined the chat room");
			serverSendMessage(socket, "J_OK" + CARRIAGE_RETURN_NEW_LINE);

			send_List_To_Other_Clients();
		}
	}

	/*
	Iterate trough all the hashmaps in the arraylist and check if username all ready exists.
	If it does then send an error message back to client.
	 */
	private boolean containUsername(String username) {
		for (HashMap client : TCP_Server.clients) {
			if (client.get("username").equals(username)) return true;
		}
		return false;
	}

	/*
	Concatenate all usernames to a string - then send list to all clients.
	 */
	private void send_List_To_Other_Clients() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("LIST");
		for (HashMap client : TCP_Server.clients) {
			stringBuilder.append(" [" + client.get("username") + "]");
		}
		stringBuilder.append(CARRIAGE_RETURN_NEW_LINE);
		String listOfClients = new String(stringBuilder);

		sendMsgToAll(listOfClients);
	}

	/*
	Validates username.
	 */
	private boolean validateUsername(String username) {
		return username.matches("^[a-zA-Z0-9_-]{1,12}$");
	}

	/*
	Validates command
	 */
	private boolean validateCommand(String command) {
		return command.equals("JOIN") || command.equals("DATA") || command.equals("IMAV") || command.equals("QUIT");
	}

	/*
	For IMAV command.
	 */
//	private void handle_IMAV_Command(Socket socket) {
//		for (HashMap client : TCP_Server.clients) {
//			if (client.get("socket") == socket) {
//				client.put("imav", true);
//			}
//		}
//	}
//
//		private void control_IMAV_Command_On_Active_Clients() {
//		Thread thread = new Thread(() -> {
//			while (true) {
//				try {
//					Thread.sleep(55000);
//					for (HashMap client : TCP_Server.clients) {
//						System.out.println(client);
//						System.out.println(client.get("imav"));
//
//						client.put("imav", false);
//					}
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//		});
//		thread.start();
//	}
//
//	private void delete_Clients_not_alive() {
//		for (HashMap client : TCP_Server.clients) {
//			if (!(boolean) client.get("imav")) {
//				TCP_Server.clients.remove(client);
//
//				String username = (String) client.get("username");
//				//send message to all that the person left the chat room
//
//				sendMsgToAll("IN [SERVER]: [" + username + "] was kicked out of the chat room.");
//				break;
//			}
//		}
//		System.out.println(TCP_Server.clients);
//	}
}
