import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class ServiceTheClient {
	private final String CARRIAGE_RETURN_NEW_LINE = "\r\n";
	private InputStream inFromClient;
	private OutputStream outToClient;
	private ArrayList<HashMap<String, Object>> users = new ArrayList<>();

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
	}

	private void handle_IAMV_Command(OutputStream outToClient, StringTokenizer stringTokenizer) {
		// if server doesn't get IAMV command - remove user from list
	}

	private void handle_QUIT_command(OutputStream outToClient,
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

	private void handle_JOIN_Command(OutputStream outToClient, StringTokenizer stringTokenizer, Socket socket) {
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

	private boolean validateUsername(String username) {
		return username.matches("^[a-zA-Z0-9_-]{1,12}$");
	}

	public boolean validateCommand(String command) {
		return command.equals("JOIN") || command.equals("DATA") || command.equals("IAMV") || command.equals("QUIT");
	}

	public String getCARRIAGE_RETURN_NEW_LINE() {
		return CARRIAGE_RETURN_NEW_LINE;
	}

	public InputStream getInFromClient() {
		return inFromClient;
	}

	public void setInFromClient(InputStream inFromClient) {
		this.inFromClient = inFromClient;
	}

	public OutputStream getOutToClient() {
		return outToClient;
	}

	public void setOutToClient(OutputStream outToClient) {
		this.outToClient = outToClient;
	}

	public ArrayList<HashMap<String, Object>> getUsers() {
		return this.users;
	}

}
