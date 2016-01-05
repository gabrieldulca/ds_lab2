package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientTCPPrivateMessageListener extends Thread{

	private ServerSocket serverSocket;
	
	public ClientTCPPrivateMessageListener(ServerSocket serverSocket){
		this.serverSocket = serverSocket;
	}
	
	@Override
	public void run() {
		while (true) {
			Socket socket = null;
			try {
				// wait for Client to connect
				socket = serverSocket.accept();
				// prepare the input reader for the socket
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				// prepare the writer for responding to clients requests
				PrintWriter writer = new PrintWriter(socket.getOutputStream(),
						true);

				// read client requests
				String privateMessage = reader.readLine();
				System.out.println(privateMessage);
				writer.println("!ack");

			} catch (IOException e) {
				//System.err.println("Private messaging error: " + e.getMessage());
				break;
			} finally {
				if (socket != null && !socket.isClosed())
					try {
						socket.close();
					} catch (IOException e) {
						// Ignored because we cannot handle it
					}
			}
		}
	}
}
