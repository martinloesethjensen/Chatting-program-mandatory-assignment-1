import javax.xml.crypto.Data;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class TCP_Server {

	static final String CARRIAGE_RETURN_NEW_LINE = "\r\n";
	static InputStream inFromClient;
	static OutputStream outToClient;
	private static ArrayList<HashMap<String, Object>> users = new ArrayList<>();

	public static void main(String[] args) {
		System.out.println("=========SERVER=========");

		final int PORT_LISTEN = 5656;

		try {
			ServerSocket server = new ServerSocket(PORT_LISTEN);

			System.out.println("Starting server...\n");


			while (true) {
				Socket socket;
				socket = server.accept();
				System.out.println("Client connected");

				Socket finalSocket = socket;
				Thread thread = new Thread(() ->
					serviceTheClient(finalSocket)
				);
				thread.start();

				String list_of_users = "LIST ";
				for (HashMap user: users) {
					if (user.get("socket") != socket) {
						list_of_users += user.get("username") + " ";

					}
				}
				list_of_users += CARRIAGE_RETURN_NEW_LINE;
				System.out.println(list_of_users);
				outToClient.write(list_of_users.getBytes());

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void serviceTheClient(Socket socket) {
		Thread thread = new Thread(() -> {
			boolean verbose = true;
			try {

				String clientIp = socket.getInetAddress().getHostAddress();
				System.out.println("IP: " + clientIp);
				System.out.println("PORT: " + socket.getPort());


				inFromClient = socket.getInputStream();
				outToClient = socket.getOutputStream();

				while (verbose) {
					byte[] bytes = new byte[1024];
					inFromClient.read(bytes);
					String commandFromClient = new String(bytes);
					System.out.println(commandFromClient);

					StringTokenizer stringTokenizer = new StringTokenizer(commandFromClient);
					String command = stringTokenizer.nextToken();
					System.out.println(command);

					if (validateCommand(command)) {
						switch (command) {
							case "JOIN":
								handle_JOIN_Command(outToClient, stringTokenizer, socket);
								break;
							case "DATA":
								//handle_DATA_Command(outToClient, stringTokenizer);
								break;
							case "IAMV":
								handle_IAMV_Command(outToClient, stringTokenizer);
								break;
							case "QUIT":
								handle_QUIT_command(outToClient, inFromClient);
								verbose = false;
								break;
						}


						System.out.println();


					} else {

						String err_Unknown_Command = "J_ERR 500: UNKNOWN COMMAND." + CARRIAGE_RETURN_NEW_LINE;
						outToClient.write(err_Unknown_Command.getBytes());
						//socket.close(); //måske skal socket lukkes efter
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		thread.start();
	}

	private static void handle_IAMV_Command(OutputStream outToClient, StringTokenizer stringTokenizer) {
		// if server doesn't get IAMV command - remove user from list
	}

	private static void handle_QUIT_command(OutputStream outToClient,
	                                        InputStream inFromClient) {
		//delete user from active list

		//close socket and streams
		System.out.println("Client is closing down and leaving the group.");
		try {
			outToClient.close();
			inFromClient.close();
		} catch (IOException e) {
			System.err.println("Error closing socket and streams : " + e.getMessage());
		}
	}

	private static void handle_JOIN_Command(OutputStream outToClient, StringTokenizer stringTokenizer, Socket socket) {
		try {
			String[] splitUsernameOnComma = stringTokenizer.nextToken().split(",");
			String username = splitUsernameOnComma[0];
			System.out.println(username);
			if (!validateUsername(username)) {
				String j_err_username_not_ok = "J_ERR 500: Username not OK. " +
					"Username may only be max 12 chars long, only letters, digits, ‘-‘ and ‘_’ allowed." + CARRIAGE_RETURN_NEW_LINE;
				outToClient.write(j_err_username_not_ok.getBytes());

			} else {
				String[] splitAddressOnColon = stringTokenizer.nextToken().split(":");
				String server_ip = splitAddressOnColon[0];
				System.out.println(server_ip);
				String server_port = splitAddressOnColon[1];
				System.out.println(server_port);

				// save person<HashMap> to an arraylist with users.
				HashMap<String, Object> user = new HashMap<>();
				user.put("username", username);
				user.put("server_ip", server_ip);
				user.put("server_port", server_port);
				user.put("iamv", true);
				user.put("socket", socket);

				users.add(user);

				//J_OK  Client is accepted
				System.out.println("Client is accepted.");
				String j_ok_command = "J_OK" + CARRIAGE_RETURN_NEW_LINE;
				outToClient.write(j_ok_command.getBytes());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean validateUsername(String username) {
		return username.matches("^[a-zA-Z0-9_-]{1,12}$");
	}

	public static boolean validateCommand(String command) {
		return command.equals("JOIN") || command.equals("DATA") || command.equals("IAMV") || command.equals("QUIT");
	}

}
