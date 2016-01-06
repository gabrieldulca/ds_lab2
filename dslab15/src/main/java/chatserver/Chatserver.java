package chatserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Command;
import cli.Shell;
import chatserver.ServerTCPListenerThread;
import util.Config;
import util.Keys;
import util.SecurityUtils;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private Shell shell;
	private List<User> users;
	private List<PrintWriter> publicWriter;
	private PrivateKey privatekey;

	/**
	 * 
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.shell = new Shell(componentName, userRequestStream, userResponseStream);
		this.shell.register(this);
		this.users = new ArrayList<User>();
		this.publicWriter = new ArrayList<PrintWriter>();
		try {
			
			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
			this.privatekey = Keys.readPrivatePEM(new File (config.getString("key")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		readUsers();

		// TODO
	}

	@Override
	public void run() {
		SecurityUtils.registerBouncyCastle();
	
		try {
			
			// handle incoming connections from client in a separate thread
			
			if(Thread.currentThread().getName().equals("tcp")){
				new Thread(shell).start();
				System.out.println(getClass().getName()
						+ " up and waiting for commands!");
				
				serverSocket = new ServerSocket(config.getInt("tcp.port"));
				ExecutorService executorTCPService = Executors.newFixedThreadPool(100);

				try{
					for(;;){
						/*executorService.execute(new Runnable() {
						    public void run() {
						    	try {
									new ServerListenerThread(serverSocket.accept(), users).start();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
						    }
						});*/
						executorTCPService.execute(new ServerTCPListenerThread(serverSocket.accept(), users, publicWriter, this.privatekey));
						
					}
				}catch(SocketException e){
					executorTCPService.shutdown();
				}
			}else if(Thread.currentThread().getName().equals("udp")){
				datagramSocket = new DatagramSocket(config.getInt("udp.port"));
				
				/*ExecutorService executorUDPService = Executors.newFixedThreadPool(100);

				try{
					for(;;)
						executorUDPService.execute(new ServerUDPListenerThread(datagramSocket, users));
				}catch(SocketException e){
					executorUDPService.shutdown();
				}*/

				new ServerUDPListenerThread(datagramSocket, users).start();
			}
			
		} catch (IOException e) {
			throw new RuntimeException("Cannot listen.", e);
		} finally {
			/*if(serverSocket != null){
				try {
					serverSocket.close();
				} catch (IOException e) {
					throw new RuntimeException("Cannot listen on TCP port.", e);
				}
			}*/
			if(datagramSocket != null){
				//datagramSocket.close();
			}
		}
	}

	@Override
	@Command
	public String users() throws IOException {
		//Config users = new Config("user");
		StringBuffer sb = new StringBuffer();
		
		for(User s : users){
			sb.append(s.getUsername());
			sb.append(" " + s.isOnlineString());
			sb.append("\n");
		}
		
		return sb.toString();
		
	}

	@Override
	@Command
	public String exit() throws IOException {
		for(PrintWriter w : publicWriter){
			w.println("logout");
		}

		this.serverSocket.close();
		this.datagramSocket.close();
		shell.close();

		return null;
	}


	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
				
		Thread tcp = new Thread((Runnable) chatserver);
		tcp.setName("tcp");
		tcp.start();
		
		Thread udp = new Thread((Runnable) chatserver);
		udp.setName("udp");
		udp.start();
		
		//new Thread((Runnable) chatserver).start();
	}
	
	private void readUsers(){
		users.clear();
		Properties properties = new Properties();
		try{
			properties.load(new FileInputStream("src/main/resources/user.properties"));
		}catch(IOException ex){
			System.err.println("Error while reading user.properties file");
		}
		
		for(String key : properties.stringPropertyNames()) {
			  String value = properties.getProperty(key);
			  users.add(new User(key.replace(".password", ""), value));
		}
	}

}
