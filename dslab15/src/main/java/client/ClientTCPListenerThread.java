package client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import util.SecurityUtils;

/**
 * Thread to listen for incoming connections on the given socket.
 */
public class ClientTCPListenerThread extends Thread {

	private Socket socket;
	private BlockingQueue<String> queue;
	private StringBuilder lastMessage;
	private boolean boo;
	private StringBuilder username;

	public ClientTCPListenerThread(Socket socket, BlockingQueue<String> queue, StringBuilder lastMessage, StringBuilder username) {
		this.socket = socket;
		this.queue = queue;
		this.lastMessage = lastMessage;
		this.boo = false;
		this.username = username;
	}

	public void run() {
		SecurityUtils.registerBouncyCastle();

		while (true) {
			try {

				// prepare the input reader for the socket
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				
				String request;
				// read client requests
				while ((request = reader.readLine()) != null) {
					
					if(request.startsWith("public")){
						if(!request.contains(username.toString())){
							lastMessage.setLength(0);
							String message = request.replace("public ", "");
							lastMessage.append(message);
							System.out.println(message);
						}
					}else if(request.equals("logout")){
						boo = true;
						break;
					}else{
						queue.put(request);
					}
				}
				

			} catch (IOException e) {
				if(boo)
					System.out.println("Server shut down");
				break;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
