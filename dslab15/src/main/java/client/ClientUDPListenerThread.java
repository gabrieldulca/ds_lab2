package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ClientUDPListenerThread extends Thread{
	private DatagramSocket datagramSocket;
	private DatagramPacket packet;

	public ClientUDPListenerThread(DatagramSocket datagramSocket, DatagramPacket packet) {
		this.datagramSocket = datagramSocket;
		this.packet = packet;
	}
	
	@Override
	public void run(){
		try {
			datagramSocket.receive(packet);
			System.out.println(new String(packet.getData()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			if (datagramSocket != null )
				datagramSocket.close();
		}
	}
}
