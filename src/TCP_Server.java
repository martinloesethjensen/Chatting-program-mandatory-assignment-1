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
	private static ArrayList<HashMap<String, Object>> users = new ArrayList<>();

	public static void main(String[] args) {
		System.out.println("=========SERVER=========");

		final int PORT_LISTEN = 5656;

		try {
			ServerSocket server = new ServerSocket(PORT_LISTEN);

			System.out.println("Starting server...\n");

			Socket socket;

			while (true) {
				socket = server.accept();
				System.out.println("Client connected");


				serviceTheClient(socket);

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void serviceTheClient(Socket socket) {
		Thread thread = new Thread(() -> {
			boolean verbose = true;
			try {
				InputStream inFromClient;
				OutputStream outToClient;

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
								handle_JOIN_Command(outToClient, stringTokenizer);
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

	private static void handle_JOIN_Command(OutputStream outToClient, StringTokenizer stringTokenizer) {
		try {
			String[] splitUsernameOnComma = stringTokenizer.nextToken().split(",");
			String username = splitUsernameOnComma[0];
			System.out.println(username);
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

			users.add(user);

			//J_OK  Client is accepted
			System.out.println("Client is accepted.");
			String j_ok_command = "J_OK" + CARRIAGE_RETURN_NEW_LINE;
			outToClient.write(j_ok_command.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static boolean validateCommand(String command) {
		return command.equals("JOIN") || command.equals("DATA") || command.equals("IAMV") || command.equals("QUIT");
	}

}
