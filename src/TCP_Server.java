import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TCP_Server {

	public static void main(String[] args) {
		System.out.println("=========SERVER=========");

		final int PORT_LISTEN = 5656;

		try {
			ServerSocket server = new ServerSocket(PORT_LISTEN);

			System.out.println("Starting server...\n");

			Socket socket = server.accept();
			System.out.println("Client connected");
			String clientIp = socket.getInetAddress().getHostAddress();
			System.out.println("IP: " + clientIp);
			System.out.println("PORT: " + socket.getPort());

			InputStream input = socket.getInputStream();
			OutputStream output = socket.getOutputStream();

			byte[] dataIn = new byte[1024];
			input.read(dataIn);
			String msgIn = new String(dataIn);
			msgIn = msgIn.trim();


			System.out.println("IN -->" + msgIn + "<--");

			String msgToSend = "SERVER: [sender:" + clientIp + " ]: " + msgIn;
			byte[] dataToSend = msgToSend.getBytes();
			output.write(dataToSend);


		} catch (IOException e) {
			e.printStackTrace();
		}
	}


//	public static boolean validateCommand(String command){
//		return command.equals("JOIN") || command.equals("DATA") || command.equals("IAMV") || command.equals("QUIT");
//	}

}
