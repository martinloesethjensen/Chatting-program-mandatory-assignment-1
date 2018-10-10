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

	public ServiceTheClient() {
	}

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
//				String username = stringTokenizer.nextToken(":");

				//Fix issue with command and username regarding one token commands like IAMV

				System.out.println(command);
				String username;
				control_IAMV_Command_On_Active_Users();
				if (validateCommand(command)) {
					switch (command) {
						case "JOIN":
							username = stringTokenizer.nextToken(":");
							handle_JOIN_Command(outToClient, stringTokenizer, socket, username);
							break;
						case "DATA":
							//If message is over 250 characters send an J_ERR
							username = stringTokenizer.nextToken(":");
							if (messageFromClient.length() > (250 + command.length() + username.length())) {
								serverSendMessage(socket, "J_ERR 500: Message too long." + CARRIAGE_RETURN_NEW_LINE);
								break;
							}
							handle_DATA_Command(stringTokenizer, username);
							break;
						case "IAMV":
							handle_IAMV_Command(socket);
							break;
						case "QUIT":
							handle_QUIT_command(outToClient, inFromClient, socket);
							verbose = false;
							break;
					}

				} else {

					serverSendMessage(socket, "J_ERR 500: UNKNOWN COMMAND." + CARRIAGE_RETURN_NEW_LINE);

				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void control_IAMV_Command_On_Active_Users() {
		Thread thread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(61100);
					for (HashMap user : TCP_Server.users) {
						System.out.println(user);
						System.out.println(user.get("iamv"));
						if ((boolean) user.get("iamv")) {
							String username = (String) user.get("username");
							//send message to all that the person left the chat room
							TCP_Server.users.remove(user);
							System.out.println(TCP_Server.users);

							sendMsgToAll("IN [SERVER]: [" + username + "] was kicked out of the chat room.");
							break;
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();

	}

	private void handle_DATA_Command(StringTokenizer stringTokenizer, String username) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("DATA ");

		stringBuilder.append(username + ": ");

		while (stringTokenizer.hasMoreTokens()) {
			stringBuilder.append(stringTokenizer.nextToken() + " ");
		}

		System.out.println(stringBuilder);

		String messageBackToClient = new String(stringBuilder);

		sendMsgToAll(messageBackToClient);
	}

	private void sendMsgToAll(String messageBackToClient) {
		for (HashMap user : TCP_Server.users) {
			Socket socket = (Socket) user.get("socket");

			serverSendMessage(socket, messageBackToClient);
		}
	}

	private void serverSendMessage(Socket socket, String messageBackToClient) {
		try {
			OutputStream outputStream = socket.getOutputStream();
			byte[] dataToSend = messageBackToClient.getBytes();
			outputStream.write(dataToSend);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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

	private void handle_IAMV_Command(Socket socket) {
		for (HashMap user: TCP_Server.users) {
			if(user.get("socket") == socket){
				user.put("iamv", true);
				System.out.println("Fundet socket: " + user.get("socket"));
				System.out.println("Socket vi arbejder med: "+ socket);
				//
			}
		}
	}

	private void handle_QUIT_command(OutputStream outToClient, InputStream inFromClient, Socket socket) {
		//delete user from active list
		for (HashMap user : TCP_Server.users) {
			if (user.get("socket").equals(socket)) {
				//send message to all that the person left the chat room
				TCP_Server.users.remove(user);

				sendMsgToAll("IN [SERVER]: [" + user.get("username") + "] left the chat room.");
				break;
			}
		}

		try {
			outToClient.close();
			inFromClient.close();
		} catch (IOException e) {
			System.err.println("Error closing socket and streams : " + e.getMessage());
		}
	}

	private void handle_JOIN_Command(OutputStream outToClient, StringTokenizer stringTokenizer, Socket socket, String username) {
		String[] splitUsernameOnComma = username.split(",");
		String usernameAlteredForJoinCommand = splitUsernameOnComma[0].substring(1);

		//sends message back to client if username isn't valid.
		if (!validateUsername(usernameAlteredForJoinCommand)) {
			serverSendMessage(socket, "J_ERR 500: Username not OK. " +
				"Username may only be max 12 chars long, only letters, digits, ‘-‘ and ‘_’ allowed." + CARRIAGE_RETURN_NEW_LINE);
		}
		if (containUsername(usernameAlteredForJoinCommand)) {
			serverSendMessage(socket, "J_ER 500: Username already in use." + CARRIAGE_RETURN_NEW_LINE);
		} else {

			String[] splitAddressOnColon = stringTokenizer.nextToken().split(":");
			String server_ip = splitUsernameOnComma[1];

			String server_port = splitAddressOnColon[0];

			// save person<HashMap> to an arraylist with users.
			HashMap<String, Object> user = new HashMap<>();
			user.put("username", usernameAlteredForJoinCommand);
			user.put("server_ip", server_ip);
			user.put("server_port", server_port);
			user.put("iamv", true);
			user.put("socket", socket);

			// add user to arraylist of active users
			TCP_Server.users.add(user);

			//J_OK  Client is accepted
			System.out.println("Client is accepted. \n\n[" + usernameAlteredForJoinCommand + "] joined the chat room");
			serverSendMessage(socket, "J_OK" + CARRIAGE_RETURN_NEW_LINE);

			send_List_To_Other_Users();
		}
	}

	// iterate trough all the hashmaps in the arraylist and check if username all ready exists.
	// if it does then send an error message back to client.
	private boolean containUsername(String username) {
		for (HashMap user : TCP_Server.users) {
			if (user.get("username").equals(username)) return true;
		}
		return false;
	}

	private void send_List_To_Other_Users() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("LIST");
		for (HashMap user : TCP_Server.users) {
			stringBuilder.append(" [" + user.get("username") + "]");
		}
		stringBuilder.append(CARRIAGE_RETURN_NEW_LINE);
		String listOfUsers = new String(stringBuilder);

		sendMsgToAll(listOfUsers);
	}

	private boolean validateUsername(String username) {
		return username.matches("^[a-zA-Z0-9_-]{1,12}$");
	}

	private boolean validateCommand(String command) {
		return command.equals("JOIN") || command.equals("DATA") || command.equals("IAMV") || command.equals("QUIT");
	}
}
