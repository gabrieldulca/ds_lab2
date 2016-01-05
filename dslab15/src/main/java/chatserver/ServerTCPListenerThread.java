package chatserver;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;

/**
 * Thread to listen for incoming connections on the given socket.
 */
public class ServerTCPListenerThread extends Thread {

	private Socket socket;
	private List<User> users;
	private List<PrintWriter> publicWriter;
	
	public ServerTCPListenerThread(Socket socket, List<User> users, List<PrintWriter> publicWriter) {
		this.socket = socket;
		this.users = users;
		this.publicWriter = publicWriter;
	}


	public void run() {

		while (true) {
			try {
				// wait for Client to connect
				// prepare the input reader for the socket
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				// prepare the writer for responding to clients requests
				PrintWriter writer = new PrintWriter(socket.getOutputStream(),
						true);

				String request;
				// read client requests
				while ((request = reader.readLine()) != null) {
					System.out.println("Client sent the following request: " + request);

					String[] parts = request.split(" ");

					String response = "!error provided message does not fit the expected format: "
							+ "!ping <client-name> or !stop <client-name>";

					writer.flush();
					if(request.startsWith("!login")){
						boolean boo = false;
						for(User u : users){
							if(u.getUsername().equals(parts[1])){
								if(u.getPassword().equals(parts[2])){
									synchronized(this){
										writer.println("login success");
										u.setOnline(true);
										boo = true;
										publicWriter.add(writer);
									}	
								}
							}
						}
						if(!boo)
							writer.println("login failed");
					}else if(request.startsWith("!logout")){
						boolean boo = false;
						for(User u : users){
							if(u.getUsername().equals(parts[1])){
								if(u.isOnline()){
									synchronized(this){
										writer.println("logout success");
										u.setOnline(false);
										u.setRegistered(false);
										boo = true;
										publicWriter.remove(writer);
									}
								}
							}
						}
						if(!boo)
							writer.println("logout failed");
					}else if(request.startsWith("!send")){
						for(PrintWriter w : publicWriter){
							w.println("public " + parts[1] + ": " + request.substring(request.lastIndexOf(parts[1]) + parts[1].length() + 1));
						}
						//writer.println("public " + parts[1] + ": " + request.substring(request.lastIndexOf(parts[1]) + parts[1].length() + 1));
					}else if(request.startsWith("!register")){
						String[] adress = parts[1].split(":");
						for(User u : users){
							if(u.getUsername().equals(parts[2])){
								synchronized(this){
									u.setIp(adress[0]);
									u.setPort(adress[1]);
									u.setRegistered(true);
									writer.println("Successfully registered address for " + u.getUsername());
								}
							}
						}
					}else if(request.startsWith("!lookup")){
						boolean boo = false;
						for(User u : users){
							if(u.getUsername().equals(parts[1])){
								if(u.isOnline() && u.isRegistered()){
									synchronized(this){
										writer.println(u.getIp()+":"+u.getPort());
										boo = true;
									}
								}
							}
						}
						if(!boo)
							writer.println("userN/A");
					}else{
						writer.println("Error: Unknown command");
					}
				}

			} catch (IOException e) {
				//System.err.println(e.getMessage());
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
