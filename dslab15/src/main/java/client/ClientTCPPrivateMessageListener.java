package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.bouncycastle.util.encoders.Base64;

import util.Config;
import util.Keys;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

public class ClientTCPPrivateMessageListener extends Thread{

	private ServerSocket serverSocket;
	Config config;
	
	public ClientTCPPrivateMessageListener(ServerSocket serverSocket){
		this.serverSocket = serverSocket;
		config = new Config("client");
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
				
				int index = privateMessage.indexOf(" ");
				String base64Message = privateMessage.substring(0, index);

				byte[] encryptedHash = Base64.decode(base64Message);
				
				String hashMsg = privateMessage.substring(index+1, privateMessage.length());
				System.out.println(hashMsg);
				
				Key key = Keys.readSecretKey(new File(config.getString("hmac.key")));

				Mac hMac = Mac.getInstance("HmacSHA256");
				hMac.init(key);
				hMac.update(hashMsg.getBytes());
				byte[] hash = hMac.doFinal();
				
				boolean validHash = MessageDigest.isEqual(encryptedHash, hash);
				if(validHash){
					writer.println("!ack");
				}else{
					System.out.println("Warning: Message tampered");
					writer.println("!tampered");
				}

			} catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
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
