package client;

import java.io.BufferedReader;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import cli.Command;
import cli.Shell;
import util.Config;

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
		this.shell = new Shell(componentName, userRequestStream, userResponseStream);
		this.shell.register(this);
		this.loggedIn = false;
		this.queue = new ArrayBlockingQueue<String>(1024);
		this.lastMessage = new StringBuilder();
		this.usernameThread = new StringBuilder();
		// TODO
	}

	@Override
	public void run() {
		System.out.println(getClass().getName()
				+ " up and waiting for commands!");

		new Thread(shell).start();


		try {
			try{
				socket = new Socket(config.getString("chatserver.host"), config.getInt("chatserver.tcp.port"));
			}catch(ConnectException e){
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
			tcpSocketInputReader = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			// create a writer to send messages to the server
			tcpSocketOutputWriter = new PrintWriter(socket.getOutputStream(), true);
			
			new ClientTCPListenerThread(socket, queue, lastMessage, usernameThread).start();
			
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
			synchronized(this){
				answer = queue.take();
			}
		
			if(answer.equals("login success")){
				loggedIn = true;
				this.username = username;
				this.usernameThread.append(username);
				return "Successfully logged in.";
			}else{
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
		if(!checkLogin()){
			return "You need to login first!";
		}
		
		String logout = ("!logout " + username);
		tcpSocketOutputWriter.println(logout);
		String answer;
		try {
			synchronized(this){
				answer = queue.take();
			}
		
			if(answer.equals("logout success")){
				loggedIn = false;
				return "Successfully logged out.";
			}else{
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
		if(!checkLogin()){
			return "You need to login first!";
		}
		
		String send = ("!send " + username + " " + message);
		tcpSocketOutputWriter.println(send);
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
		if(!checkLogin()){
			return "You need to login first!";
		}
		
		String msg = (username + ("(private)")+ ": " + message);
		String lookup = lookup(username);
		String[] parts = lookup.split(":");
		
		Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
		BufferedReader privateInputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter privateOutputWriter = new PrintWriter(socket.getOutputStream(), true);
		
		privateOutputWriter.println(msg);
		
		return username + " replied with " + privateInputReader.readLine();
	
	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
		if(!checkLogin()){
			return "You need to login first!";
		}
		
		String lookup = ("!lookup " + username);
		tcpSocketOutputWriter.println(lookup);
		
		try {
			synchronized(this){
				String answer = queue.take();
			
			
				if(answer.equals("userN/A")){
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
		if(!checkLogin()){
			return "You need to login first!";
		}
		
		String register = ("!register " + privateAddress + " " + username);
		
		tcpSocketOutputWriter.println(register);
		Integer port = Integer.parseInt(privateAddress.substring(privateAddress.lastIndexOf(":")+1));
		serverSocket = new ServerSocket(port);
		new ClientTCPPrivateMessageListener(serverSocket).start();
		
		try {
			synchronized(this){
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
		if(!checkLogin()){
			return "You need to login first!";
		}
		return lastMessage.toString();
	}

	@Override
	@Command
	public String exit() throws IOException {
		if(checkLogin()){
			logout();
		}
		
		if(socket != null)
			this.socket.close();
		if(datagramSocket != null)
			this.datagramSocket.close();
		if(shell != null)
			this.shell.close();
		if(serverSocket != null)
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

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	private boolean checkLogin(){
		if(loggedIn){
			return true;
		}else{
			return false;
		}
	}

}
