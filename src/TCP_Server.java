import javax.xml.crypto.Data;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class TCP_Server {



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


				Socket finalSocket = socket;
				Thread thread = new Thread(() ->
					client.serviceTheClient(socket)
				);
				thread.start();

//				String list_of_users = "LIST ";
//				for (HashMap user: client.getUsers()) {
//					if (user.get("socket") != socket) {
//						list_of_users += user.get("username") + " ";
//
//					}
//				}
//				list_of_users += client.getCARRIAGE_RETURN_NEW_LINE();
//				System.out.println(list_of_users);
//				client.getOutToClient().write(list_of_users.getBytes());

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}



}
