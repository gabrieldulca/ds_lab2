package chatserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import util.Config;
import util.Keys;
import util.SecurityUtils;

/**
 * Thread to listen for incoming connections on the given socket.
 */
public class ServerTCPListenerThread extends Thread {

	private Socket socket;
	private List<User> users;
	private List<PrintWriter> publicWriter;
	private PrivateKey chatserverPrivateKey;
	private String chatserverChallenge;
	private SecretKey sessionKey;
	private byte[] sessionIV;
	
	public ServerTCPListenerThread(Socket socket, List<User> users, List<PrintWriter> publicWriter, PrivateKey privatekey) {
		this.socket = socket;
		this.users = users;
		this.publicWriter = publicWriter;
		this.chatserverPrivateKey = privatekey;
	}


	public void run() {
		boolean validsession = false;

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
					if(!validsession){
						String response = this.authenticate(request);
						writer.println(response);
					}
					else{
						
						if (this.checkSession(request)) {
							validsession = true;
						} else {
							writer.println("Invalid session");
							break;
						}
						
						try {
							String decryptedRequest = new String (SecurityUtils.decryptMessageAES(request.getBytes(), this.sessionKey, this.sessionIV));
						
						System.out.println("Client sent the following request: " + decryptedRequest);
	
						String[] parts = decryptedRequest.split(" ");
	
						String response = "!error provided message does not fit the expected format: "
								+ "!ping <client-name> or !stop <client-name>";
	
						writer.flush();
						if(decryptedRequest.startsWith("!login")){
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
						}else if(decryptedRequest.startsWith("!logout")){
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
						}else if(decryptedRequest.startsWith("!send")){
							for(PrintWriter w : publicWriter){
								String r = "public " + parts[1] + ": " + decryptedRequest.substring(decryptedRequest.lastIndexOf(parts[1]) + parts[1].length() + 1);
								String cliResponse = new String(SecurityUtils.encryptMessageAES(r.getBytes(), this.sessionKey,
										this.sessionIV));

								w.println(cliResponse);
							}
							//writer.println("public " + parts[1] + ": " + request.substring(request.lastIndexOf(parts[1]) + parts[1].length() + 1));
						}else if(decryptedRequest.startsWith("!register")){
							String[] adress = parts[1].split(":");
							for(User u : users){
								if(u.getUsername().equals(parts[2])){
									synchronized(this){
										u.setIp(adress[0]);
										u.setPort(adress[1]);
										u.setRegistered(true);
										String r = "Successfully registered address for " + u.getUsername();
										String cliResponse = new String(SecurityUtils.encryptMessageAES(r.getBytes(), this.sessionKey,
												this.sessionIV));

										writer.println(cliResponse);
										
									}
								}
							}
						}else if(decryptedRequest.startsWith("!lookup")){
							boolean boo = false;
							for(User u : users){
								if(u.getUsername().equals(parts[1])){
									if(u.isOnline() && u.isRegistered()){
										synchronized(this){
											String r = u.getIp()+":"+u.getPort();
											String cliResponse = new String(SecurityUtils.encryptMessageAES(r.getBytes(), this.sessionKey,
													this.sessionIV));

											writer.println(cliResponse);
											
											boo = true;
										}
									}
								}
							}
							if(!boo) {
								String r = "userN/A";
								String cliResponse = new String(SecurityUtils.encryptMessageAES(r.getBytes(), this.sessionKey,
										this.sessionIV));

								writer.println(cliResponse);
							}
						}else{
							String r = "Error: Unknown command";
							String cliResponse = new String(SecurityUtils.encryptMessageAES(r.getBytes(), this.sessionKey,
									this.sessionIV));

							writer.println(cliResponse);
						}} catch (InvalidKeyException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchPaddingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalBlockSizeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (BadPaddingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvalidAlgorithmParameterException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
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
	
	
	private boolean checkSession(String request) {
		String receivedChallenge;
		
		try {
			receivedChallenge = new String(SecurityUtils.decryptMessageAES(request.getBytes(),
						this.sessionKey, this.sessionIV));
			if (receivedChallenge.equals(this.chatserverChallenge)) {
				return true;
			}
			else {
				return false;
			}
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	private String authenticate(String request) {
		// decrypt client request (RSA):
		String[] args;
		try {
			args = new String(SecurityUtils.decryptMessageRSA(request.getBytes(),
					this.chatserverPrivateKey)).split(" ");
		}
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
				| NoSuchAlgorithmException | NoSuchPaddingException e) {
			
			return "!error decryptionFailed";
		}

		// only accept "!authenticate" request:
		// ATTENTION: arguments are considered sanitized and valid!
		if (args[0].equals("!authenticate")) {
			// decode request:
			String username = args[1]; // plain text
			String clientChallenge = args[2]; // is base64 encoded

			// read users public key:
			Config config = new Config("chatserver");
			File userPubKeyFile = new File(config.getString("keys.dir"), username + ".pub.pem");
			PublicKey userPublicKey;
			try {
				userPublicKey = Keys.readPublicPEM(userPubKeyFile);
			}
			catch (IOException e) {
				
				return "!error PublicKeyNotFound";
			}

			// generate key:
			this.chatserverChallenge = SecurityUtils.generateChallenge(); // already encoded
			this.sessionKey = SecurityUtils.generateKeyAES();
			this.sessionIV = SecurityUtils.generateIV();
			String encodedKey = SecurityUtils.encodeSessionKey(this.sessionKey.getEncoded());
			String encodedIv = SecurityUtils.encodeIV(this.sessionIV);

			// respond to users auth-request:
			String response = String.format("!ok %s %s %s %s", clientChallenge,
					chatserverChallenge, encodedKey, encodedIv);
			try {
				return new String(SecurityUtils.encryptMessageRSA(response.getBytes(),
						userPublicKey));
			}
			catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
					| IllegalBlockSizeException | BadPaddingException e) {
				
				return "!error encryptionFailed";
			}
		}
		else {
			return "!error authenticationNeeded";
		}

	}
}
