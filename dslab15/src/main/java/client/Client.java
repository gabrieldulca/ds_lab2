package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Mac;


import cli.Command;
import cli.Shell;
import util.Config;
import util.Keys;
import util.SecurityUtils;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private Shell shell;
	private Socket socket;
	private BufferedReader tcpSocketInputReader;
	private PrintWriter tcpSocketOutputWriter;
	private DatagramSocket datagramSocket;
	private boolean loggedIn;
	private String username;
	private StringBuilder usernameThread;
	private StringBuilder lastMessage;
	private BlockingQueue<String> queue;
	private ServerSocket serverSocket;
	private SecretKey sessionKey;
	private byte[] sessionIV;

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.shell = new Shell(componentName, userRequestStream,
				userResponseStream);
		this.shell.register(this);
		this.loggedIn = false;
		this.queue = new ArrayBlockingQueue<String>(1024);
		this.lastMessage = new StringBuilder();
		this.usernameThread = new StringBuilder();
		// TODO
	}

	@Override
	public void run() {
		SecurityUtils.registerBouncyCastle();

		System.out.println(getClass().getName()
				+ " up and waiting for commands!");

		new Thread(shell).start();

		try {
			try {
				socket = new Socket(config.getString("chatserver.host"),
						config.getInt("chatserver.tcp.port"));
			} catch (ConnectException e) {
				System.out.println("Server is N/A. Exit in 3 seconds ...");
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				this.exit();
				System.exit(1);
			}
			// create a reader to retrieve messages send by the server
			tcpSocketInputReader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			// create a writer to send messages to the server
			tcpSocketOutputWriter = new PrintWriter(socket.getOutputStream(),
					true);

			new ClientTCPListenerThread(socket, queue, lastMessage,
					usernameThread).start();

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		String login = ("!login " + username + " " + password);
		tcpSocketOutputWriter.println(login);
		String answer;
		try {
			synchronized (this) {
				answer = queue.take();
			}

			if (answer.equals("login success")) {
				loggedIn = true;
				this.username = username;
				this.usernameThread.append(username);
				return "Successfully logged in.";
			} else {
				return "Wrong username or password.";
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	@Override
	@Command
	public String logout() throws IOException {

		if (!checkLogin()) {
			return "You need to login first!";
		}


		String logout = ("!logout " + username);
		//tcpSocketOutputWriter.println(logout);
		this.sendToChatserver(logout);
		String answer;
		try {
			synchronized (this) {
				answer = queue.take();
			}

			if (answer.equals("logout success")) {
				loggedIn = false;
				return "Successfully logged out.";
			} else {
				return "Logout failed.";
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		Key key = Keys.readSecretKey(new File("hmac.key"));
		System.out.println(key.toString());

		if (!checkLogin()) {
			return "You need to login first!";
		}

		String send = ("!send " + username + " " + message);
		//tcpSocketOutputWriter.println(send);
		this.sendToChatserver(send);
		return null;
	}

	@Override
	@Command
	public String list() throws IOException {
		byte[] buffer = "!list".getBytes();
		DatagramPacket packet;
		datagramSocket = new DatagramSocket();

		packet = new DatagramPacket(buffer, buffer.length,
				InetAddress.getByName(config.getString("chatserver.host")),
				config.getInt("chatserver.udp.port"));

		datagramSocket.send(packet);

		buffer = new byte[1024];
		// create a fresh packet
		packet = new DatagramPacket(buffer, buffer.length);
		// wait for response-packet from server

		new ClientUDPListenerThread(datagramSocket, packet).start();

		return null;
	}

	@Override
	@Command
	public String msg(String username, String message) throws IOException {
		if (!checkLogin()) {
			return "You need to login first!";
		}

		String decryptMsg = username + ("(private)") + ": " + message;
		
		String lookup = lookup(username);
		String[] parts = lookup.split(":");

		Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
		BufferedReader privateInputReader = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));
		PrintWriter privateOutputWriter = new PrintWriter(
				socket.getOutputStream(), true);

		Key key;

		try {
			key = Keys.readSecretKey(new File(config.getString("hmac.key")));

			Mac hMac = Mac.getInstance("HmacSHA256");
			hMac.init(key);
			hMac.update(decryptMsg.getBytes());
			byte[] hash = hMac.doFinal();
			
			byte[] encryptedMessage = hash;
			byte[] base64Message = Base64.encode(encryptedMessage);
									
			String msg = new String(base64Message) + " " + (username + ("(private)") + ": " + message);

			privateOutputWriter.println(msg);

		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String returned = privateInputReader.readLine();
		if(returned.equals("!ack")){
			return username + " replied with " + returned;

		}else{
			return "!tampered " + message;
		}

	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
		if (!checkLogin()) {
			return "You need to login first!";
		}

		String lookup = ("!lookup " + username);

		//tcpSocketOutputWriter.println(lookup);
		this.sendToChatserver(lookup);
		

		tcpSocketOutputWriter.println(lookup);


		try {
			synchronized (this) {
				String answer = queue.take();

				if (answer.equals("userN/A")) {
					return "User N/A";
				}

				return answer;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {
		if (!checkLogin()) {
			return "You need to login first!";
		}

		String register = ("!register " + privateAddress + " " + username);

		
		//tcpSocketOutputWriter.println(register);
		this.sendToChatserver(register);
		Integer port = Integer.parseInt(privateAddress.substring(privateAddress.lastIndexOf(":")+1));


		tcpSocketOutputWriter.println(register);
		port = Integer.parseInt(privateAddress.substring(privateAddress.lastIndexOf(":") + 1));

		serverSocket = new ServerSocket(port);
		new ClientTCPPrivateMessageListener(serverSocket).start();

		try {
			synchronized (this) {
				return queue.take();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	@Override
	@Command
	public String lastMsg() throws IOException {
		if (!checkLogin()) {
			return "You need to login first!";
		}
		return lastMessage.toString();
	}

	@Override
	@Command
	public String exit() throws IOException {
		if (checkLogin()) {
			logout();
		}

		if (socket != null)
			this.socket.close();
		if (datagramSocket != null)
			this.datagramSocket.close();
		if (shell != null)
			this.shell.close();
		if (serverSocket != null)
			serverSocket.close();

		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);

		new Thread((Runnable) client).start();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---
	
	private String sendChatserverRawMessage(String message) throws IOException {
		String response;
		this.tcpSocketOutputWriter.println(message);
		response = this.tcpSocketInputReader.readLine();
		return response;
	}

	@Override
	public String authenticate(String username) throws IOException {
		// get RSA-Keys:
				PublicKey chatserverPublicKey = Keys.readPublicPEM(new File(this.config
						.getString("chatserver.key")));
				PrivateKey userPrivateKey = Keys.readPrivatePEM(new File(this.config.getString("keys.dir"),
						username + ".pem"));

				// generate authentication request:
				String clientChallenge = SecurityUtils.generateChallenge();
				String request = String.format("!authenticate %s %s", username, clientChallenge);

				// encrypt and send first authentication request:
				String chatserverResponse;
				try {
					chatserverResponse = new String(SecurityUtils.decryptMessageRSA(
							this.sendChatserverRawMessage(
									new String(SecurityUtils.encryptMessageRSA(request.getBytes(),
											chatserverPublicKey))).getBytes(), userPrivateKey));
				}
				catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
						| NoSuchAlgorithmException | NoSuchPaddingException e) {
					return (e.getMessage());
				}

				// decode controllers response (should contain AES Key):
				String[] responseArgs = chatserverResponse.split(" ");
				String controllerChallenge;
				if (responseArgs[0].equals("!ok")) {
					if (responseArgs[1].equals(clientChallenge)) {
						// matching clientchallenge. -> continue
						controllerChallenge = responseArgs[2]; // base64 encoded, leave it that way
						this.sessionKey = new SecretKeySpec(SecurityUtils.decode(responseArgs[3]), "AES");
						this.sessionIV = SecurityUtils.decode(responseArgs[4]);
						// now try to encode with new AES key to get verification:
						String cipherText;
						try {
							cipherText = new String(SecurityUtils.encryptMessageAES(
									controllerChallenge.getBytes(), this.sessionKey, this.sessionIV));
						}
						catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
								| NoSuchAlgorithmException | NoSuchPaddingException
								| InvalidAlgorithmParameterException e) {
							return "!error " + e.getMessage();
						}
						this.tcpSocketOutputWriter.println(cipherText);
						return "success";
					}
					else {
						return "!error invalidChallenge";
					}
				}
				else {
					return chatserverResponse;
				}
	}
	
	/**
	 * Sends AES encrypted Message (base64 encoded).
	 * 
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchPaddingException
	 * @throws NoSuchAlgorithmException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws InvalidKeyException
	 */
	private String sendToChatserver(String message) throws IOException {
		if (this.sessionKey == null) {
			return "!error authenticationNeeded";
		}
		String cipherText;
		try {
			cipherText = new String(SecurityUtils.encryptMessageAES(message.getBytes(),
					this.sessionKey, this.sessionIV));
		}
		catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
				| NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException e) {
			return "!error " + e.getMessage();
		}
		String response = this.sendChatserverRawMessage(cipherText);
		try {
			return new String(SecurityUtils.decryptMessageAES(response.getBytes(), this.sessionKey,
					this.sessionIV));
		}
		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| IllegalBlockSizeException | BadPaddingException
				| InvalidAlgorithmParameterException e) {
			return "!error " + e.getMessage();
		}
	}

	private boolean checkLogin() {
		if (loggedIn) {
			return true;
		} else {
			return false;
		}
	}

}
