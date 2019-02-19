import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class TCP_Client2 {
	static final String CARRIAGE_RETURN_NEW_LINE = "\r\n";
	static boolean verbose = true;

	public static void main(String[] args) {
		System.out.println("=========CLIENT=========");

		Scanner inputFromUser = new Scanner(System.in);
		System.out.print("What is the IP for the server (type 0 for localhost): ");
		//String ipToConnect = args.length >= 1 ? args[0] : inputFromUser.nextLine();
		String ipToConnect = "0";

		System.out.print("What is the PORT for the server: ");
		//int portToConnect = args.length >= 2 ? Integer.parseInt(args[1]) : inputFromUser.nextInt();
		int portToConnect = 5656;


		final int PORT_SERVER = portToConnect;
		final String IP_SERVER_STR = ipToConnect.equals("0") ? "127.0.0.1" : ipToConnect;

		try {
			InetAddress ip = InetAddress.getByName(IP_SERVER_STR);

			System.out.println("\nConnecting...");
			System.out.println("SERVER IP: " + IP_SERVER_STR);
			System.out.println("SERVER PORT: " + PORT_SERVER + "\n");

			Socket socket = new Socket(ip, PORT_SERVER);

			InputStream inFromServer;
			OutputStream outToServer;

			Scanner usernameInput = new Scanner(System.in);

			String username;

			//Server will validate username
			System.out.print("Please enter a username: ");
			username = usernameInput.next();

			outToServer = socket.getOutputStream();

			//Sends first initial message to try joining the server
			String msgToServer = "JOIN " + username + ", " + IP_SERVER_STR + ":" + PORT_SERVER + CARRIAGE_RETURN_NEW_LINE;
			outToServer.write(msgToServer.getBytes());

			inFromServer = socket.getInputStream();

			byte[] bytes = new byte[1024];
			inFromServer.read(bytes);
			String responseFromServer = new String(bytes);

			System.out.println(responseFromServer);

			if (responseFromServer.trim().equals("J_OK")) { // trim because the byte array consists of many placeholders
				System.out.println("OK to continue...");

				/*
				Threads that sends and receive data to and from the server.
				 */
				send_IMAV_Command(outToServer);
				receiveFromServerThread(socket);
				sendMessageToServerThread(inputFromUser, outToServer, username);

			} else {
				main(args);
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	Creates a thread that handles receiving input from the server.
	 */
	private static void receiveFromServerThread(Socket socket) {
		Thread thread = new Thread(() -> {
			while (true) {
				try {
					InputStream inputStream = socket.getInputStream();

					byte[] bytes = new byte[1024];

					inputStream.read(bytes);

					String responseFromServer = new String(bytes);
					System.out.println(responseFromServer);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}

	/*
	Creates a thread that handles messages being sent to the server.
	 */
	private static void sendMessageToServerThread(Scanner inputFromUser, OutputStream outToServer, String username) {
		Thread thread = new Thread(() -> {
			while (true) {
				System.out.println("What do you want to send? ");
				String userInput = inputFromUser.nextLine();

				if (userInput.equals("QUIT")) {
					sendMessageToServer(outToServer, "QUIT" + CARRIAGE_RETURN_NEW_LINE);

					System.out.println("Logging of...");
					verbose = false;
					System.exit(0);
					break;
				} else {
					sendMessageToServer(outToServer, "DATA " + username + ": " + userInput + CARRIAGE_RETURN_NEW_LINE);
				}
			}
		});
		thread.start();
	}

	/*
	Sends a message to the server.
	 */
	private static void sendMessageToServer(OutputStream outToServer, String message) {
		try {
			outToServer.write(message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	Creates a Thread that sends a message every 60 seconds to the server
	 */
	private static void send_IMAV_Command(OutputStream outToServer) {
		Thread thread = new Thread(() -> {
			while (true) {
				if (!verbose) break; // if client QUIT
				try {
					Thread.sleep(60000);
					sendMessageToServer(outToServer, "IMAV" + CARRIAGE_RETURN_NEW_LINE);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}
}
