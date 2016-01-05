package chatserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Thread to listen for incoming data packets on the given socket.
 */
public class ServerUDPListenerThread extends Thread {

	private DatagramSocket datagramSocket;
	private List<User> users;

	public ServerUDPListenerThread(DatagramSocket datagramSocket, List<User> users) {
		this.datagramSocket = datagramSocket;
		this.users = users;
	}

	public void run() {
		byte[] buffer;
		DatagramPacket packet;
		try {
			while (true) {
				buffer = new byte[1024];
				// create a datagram packet of specified length (buffer.length)
				/*
				 * Keep in mind that: in UDP, packet delivery is not
				 * guaranteed,and the order of the delivery/processing is not
				 * guaranteed
				 */
				packet = new DatagramPacket(buffer, buffer.length);

				// wait for incoming packets from client
				datagramSocket.receive(packet);
				Collections.sort(users, new Comparator<User>(){
				    public int compare(User s1, User s2) {
				        return s1.getUsername().compareToIgnoreCase(s2.getUsername());
				    }
				});

				// get the data from the packet
				String request = new String(packet.getData());

				System.out.println("Received request-packet from client: " + request);

				// check if request has the correct format:
				// !ping <client-name>
				StringBuilder sb = new StringBuilder();
				sb.append("Online users:");
				for(User u : users){
					if(u.isOnline()){
						sb.append("\n");
						sb.append("* "+ u.getUsername());
					}
				}
				
				String response = sb.toString();
				
				// get the address of the sender (client) from the received
				// packet
				InetAddress address = packet.getAddress();
				// get the port of the sender from the received packet
				int port = packet.getPort();
				buffer = response.getBytes();
				/*
				 * create a new datagram packet, and write the response bytes,
				 * at specified address and port. the packet contains all the
				 * needed information for routing.
				 */
				packet = new DatagramPacket(buffer, buffer.length, address,
						port);
				// finally send the packet
				datagramSocket.send(packet);
			}

		} catch (IOException e) {
			//System.err.println(e.getMessage());
		} finally {
			if (datagramSocket != null && !datagramSocket.isClosed())
				datagramSocket.close();
		}

	}
}
