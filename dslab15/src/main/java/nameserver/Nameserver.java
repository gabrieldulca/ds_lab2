package nameserver;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cli.Command;
import cli.Shell;
import chatserver.User;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable, INameserver,
		Serializable {

	private String componentName;
	private Config config;
	transient private InputStream userRequestStream;
	transient private PrintStream userResponseStream;
	transient private Shell shell;
	private Registry registry;
	private Map<String, INameserver> nameserverMap;
	private String rootId;
	private String registryHost;
	private int registryPort;
	private INameserverForChatserver nameserverForChatserver;
	private Map<String, String> usersMap;
	INameserver remote;
	boolean isRoot;
	transient BufferedReader reader;

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
	public Nameserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		// TODO
		this.shell = new Shell(componentName, userRequestStream,
				userResponseStream);
		this.shell.register(this);
		this.nameserverMap = new HashMap<String, INameserver>();
		this.usersMap = new HashMap<String, String>();
		this.isRoot = false;
		this.reader = new BufferedReader(new InputStreamReader(System.in));
	}

	@Override
	public void run() {

		try {
			new Thread(shell).start();
			this.rootId = this.config.getString("root_id");
			this.registryHost = this.config.getString("registry.host");
			this.registryPort = this.config.getInt("registry.port");

			if (!this.config.listKeys().contains("domain")) {
				registry = LocateRegistry.createRegistry(registryPort);
				remote = (INameserver) UnicastRemoteObject
						.exportObject(this, 0);
				registry.bind(this.rootId, remote);
				isRoot = true;

				System.out.println("Root-server is up and waiting for commands...");

			} else {

				INameserver remoteObject = null;
				try {
					remoteObject = (INameserver) LocateRegistry.getRegistry(
							config.getString("registry.host"),
							config.getInt("registry.port")).lookup("root-nameserver");
					remote = (INameserver) UnicastRemoteObject
							.exportObject(this, 0);
					remoteObject.registerNameserver(
							this.config.getString("domain"), remote, remote);
				} catch (NotBoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				System.out.println(config.getString("domain") + "-server is up and waiting for commands...");


			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (AlreadyBoundException e) {
			e.printStackTrace();
		} catch (AlreadyRegisteredException e) {
			e.printStackTrace();
		} catch (InvalidDomainException e) {
			e.printStackTrace();
		}

		// TODO
	}

	@Override
	@Command
	public String nameservers() throws IOException {
		String ret = "";
		int i = 1;

		if (!nameserverMap.isEmpty()) {
			for (String s : nameserverMap.keySet()) {
				ret += i + ". " + s + "\n";
				i++;
			}
		} else {
			ret = "No Nameservers found.";
		}

		return ret;
	}

	@Override
	@Command
	public String addresses() throws IOException {
		String ret = "";

		if (!usersMap.isEmpty()) {
			for (String s : usersMap.keySet()) {
				ret += s + "\n";
			}
		} else {
			ret = "No Users found.";
		}

		return ret;
	}

	@Override
	@Command
	public String exit() throws IOException {
		try {
			UnicastRemoteObject.unexportObject(this, true);

			if (this.componentName.equals("ns-root")) {
				registry.unbind(this.registryHost);
				UnicastRemoteObject.unexportObject(registry, true);
			}
		} catch (NotBoundException e) {
			e.printStackTrace();
		}

		return "Close all connections.";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
				System.in, System.out);
		// TODO: start the nameserver

		Thread threadNameserver = new Thread((Runnable) nameserver);
		threadNameserver.start();
	}

	@Override
	public synchronized void registerNameserver(String domain, INameserver nameserver,
			INameserverForChatserver nameserverForChatserver)
			throws RemoteException, AlreadyRegisteredException,
			InvalidDomainException {
		String[] domainParts = domain.split("\\.");

		if(domainParts.length > 1){
			if (this.nameserverMap.containsKey(domainParts[domainParts.length - 1])) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < domainParts.length - 1; i++) {
					sb.append(domainParts[i]+".");
				}
				System.out.println("Request for sub-zone '" + sb.toString() + domainParts[domainParts.length - 1] + "'");
				nameserverMap.get(domainParts[domainParts.length - 1])
						.registerNameserver(sb.toString(), nameserver,
								nameserverForChatserver);
			}else{
				throw new InvalidDomainException("Domain N/A");
			}
		}else {
			if(this.nameserverMap.containsKey(domain)){
				throw new AlreadyRegisteredException("Domain already registered.");
			}
				
			System.out.println("Registering nameserver for zone '" + domainParts[domainParts.length - 1] + "'");
			nameserverMap.put(domainParts[domainParts.length - 1], nameserver);
		}
	}
	

	@Override
	public synchronized void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
		String []userParts = username.split("\\.");

		if(userParts.length > 1){
			if(nameserverMap.containsKey(userParts[userParts.length-1])){
				System.out.println("User to Subdomain: " + username);
				nameserverMap.get(userParts.length-1).registerUser(username.substring(0, username.length() - userParts[userParts.length-1].length() - 1), address);
			} else {
				throw new InvalidDomainException("Domain N/A");
			}
		} else {
			if(usersMap.containsKey(username)){
					throw new AlreadyRegisteredException("User already registered.");
			}
				
			System.out.println("Username " + username + " registered. Address: " + address);
			usersMap.put(userParts[userParts.length-1], address);
		}
	}

	@Override
	public INameserverForChatserver getNameserver(String zone)
			throws RemoteException {
		INameserverForChatserver ret;

		if (nameserverMap.containsKey(zone)) {
			return nameserverMap.get(zone);
		} else {
			return null;
		}
	}

	
	@Override
	public String lookup(String username) throws RemoteException {
		String ret = "";

		for (User u : userList) {
			if (u.getUsername().equals(username)) {
				ret += u.getIp();
			}
		}
		
		return "s";
	}
	
}
