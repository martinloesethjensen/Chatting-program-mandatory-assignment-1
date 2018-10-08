import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class TCP_Server {
	public static ArrayList<HashMap<String, Object>> users = new ArrayList<>();

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

				ServiceTheClient client = new ServiceTheClient();

				Thread thread = new Thread(() ->
					client.serviceTheClient(socket)
				);
				thread.start();

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}



}
