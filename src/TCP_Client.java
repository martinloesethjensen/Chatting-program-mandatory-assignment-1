import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.StringTokenizer;

public class TCP_Client {
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

				send_IAMV_Command(outToServer);

				receiveFromServerThread(socket);
				sendToSeverThread(inputFromUser, outToServer, username);

			} else {
				main(args);

			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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

	private static void sendToSeverThread(Scanner inputFromUser, OutputStream outToServer, String username) {
		Thread thread = new Thread(() -> {
			while (true) {
				try {
					System.out.println("What do you want to send? ");
					String userInput = inputFromUser.nextLine();

					if (userInput.equals("QUIT")) {
						String quit_command = "QUIT" + CARRIAGE_RETURN_NEW_LINE;

						outToServer.write(quit_command.getBytes());

						System.out.println("Logging of...");
						verbose = false;
						System.exit(0);
						break;
					} else {

						//check with how many characters the message contains
						String msgToSend = "DATA " + username + ": " + userInput + CARRIAGE_RETURN_NEW_LINE;

						outToServer.write(msgToSend.getBytes());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}

	private static void send_IAMV_Command(OutputStream outToServer) {
		Thread thread = new Thread(() -> {
			while (true) {
				if (!verbose) break;
				try {
					Thread.sleep(60000);
					String IAMV = "IAMV";
					outToServer.write(IAMV.getBytes());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
			}
		});
		thread.start();
	}


}
