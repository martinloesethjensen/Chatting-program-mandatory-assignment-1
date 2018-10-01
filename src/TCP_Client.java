import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class TCP_Client {
	public static void main(String[] args) {
		System.out.println("=========CLIENT=========");

		Scanner inputFromUser = new Scanner(System.in);
		System.out.print("What is the IP for the server (type 0 for localhost): ");
		String ipToConnect = args.length >= 1 ? args[0] : inputFromUser.nextLine();

		System.out.print("What is the PORT for the server: ");
		int portToConnect = args.length >= 2 ? Integer.parseInt(args[1]) : inputFromUser.nextInt();


		final int PORT_SERVER = portToConnect;
		final String IP_SERVER_STR = ipToConnect.equals("0") ? "127.0.0.1" : ipToConnect;

		try {
			InetAddress ip = InetAddress.getByName(IP_SERVER_STR);

			System.out.println("\nConnecting...");
			System.out.println("SERVER IP: " + IP_SERVER_STR);
			System.out.println("SERVER PORT: " + PORT_SERVER + "\n");

			Socket socket = new Socket(ip, PORT_SERVER);

			InputStream input = socket.getInputStream();
			OutputStream output = socket.getOutputStream();

			Scanner usernameInput = new Scanner(System.in);
			// Validating username.
			// Username may only be max 12 chars long, only letters, digits, ‘-‘ and ‘_’ allowed.
			while (true) {
				System.out.print("Please enter a username: ");
				String username = usernameInput.next();
				if(validateUsername(username))break;
				System.out.println("Username may only be max 12 chars long, only letters, digits, ‘-‘ and ‘_’ allowed.\n");
			}

			inputFromUser = new Scanner(System.in);
			System.out.println("What do you want to send? ");

			//check with how many characters the message contains
			String msgToSend = inputFromUser.nextLine();

			byte[] dataToSend = msgToSend.getBytes();
			output.write(dataToSend);

			byte[] dataIn = new byte[1024];
			input.read(dataIn);
			String msgIn = new String(dataIn);
			msgIn = msgIn.trim();


			System.out.println("IN -->" + msgIn + "<--");

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean validateUsername(String username) {
		String pattern = "^[a-z0-9_-]{1,12}$";
		return username.matches(pattern);
	}
}
