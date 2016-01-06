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

import chatserver.User;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable, INameserver, Serializable {

    private String componentName;
    private Config config;
    transient private InputStream userRequestStream;
    transient private PrintStream userResponseStream;

    private Registry registry;
    private Map<String, INameserver> nameserverMap;
    private String rootId;
    private String registryHost;
    private int registryPort;
    private INameserverForChatserver nameserverForChatserver;
    private List<User> userList;
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

        this.nameserverMap = new HashMap<String, INameserver>();
        this.userList = new ArrayList<User>();
        this.isRoot = false;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {

        try {
            this.rootId = this.config.getString("root_id");
            this.registryHost = this.config.getString("registry.host");
            this.registryPort = this.config.getInt("registry.port");

            if(!this.config.listKeys().contains("domain")){
                registry = LocateRegistry.createRegistry(registryPort);
                remote = (INameserver) UnicastRemoteObject.exportObject(this, 0);
                registry.bind(this.rootId, remote);
                isRoot = true;

                System.out.println("Root-server is up and waiting for commands...");

                try {
                    String command;
                    while ((command = reader.readLine()) != null) {

                        if (command.equals("!nameservers")) {
                            System.out.print(nameservers());
                        } else if (command.equals("!addresses")) {
                            System.out.println(addresses());
                        } else if (command.equals("!exit")) {
                            System.out.println(exit());
                        } else {
                            System.out.println("No valid command!");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else {
            	
                INameserver remoteObject = null;
				try {
					remoteObject = (INameserver) LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port")).lookup("root-nameserver");
					remoteObject.registerNameserver(this.config.getString("domain"), this, nameserverForChatserver);
				} catch (NotBoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                	
                //registerNameserver(this.config.getString("domain"), this, nameserverForChatserver);

                System.out.println("AT-server is up and waiting for commands...");

                try {
                    String command;
                    while ((command = reader.readLine()) != null) {

                        if (command.equals("!nameservers")) {
                            System.out.print(nameservers());
                        } else if (command.equals("!addresses")) {
                            System.out.println(addresses());
                        } else if (command.equals("!exit")) {
                            System.out.println(exit());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
    public String nameservers() throws IOException {
        String ret = "";

        if(!nameserverMap.isEmpty()) {
            for (String s : nameserverMap.keySet()) {
                ret += " " + s + "\n";
            }
        } else {
            ret = "No Nameservers found.";
        }

        return ret;
    }

    @Override
    public String addresses() throws IOException {
        String ret = "";

        if(!userList.isEmpty()) {
            for (User u : userList) {
                ret += " " + u.getUsername() + " " + u.getIp() + "\n";
            }
        } else {
            ret = "No Users found.";
        }

        return ret;
    }

    @Override
    public String exit() throws IOException {
        try {
            UnicastRemoteObject.unexportObject(this, true);

            if(this.componentName.equals("ns-root")) {
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
    public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        //try {
            //INameserver remoteObject = (INameserver) LocateRegistry.getRegistry(config.getString("registry.host"), config.getInt("registry.port")).lookup("root-nameserver");
            
            String[] domainParts = domain.split("\\.");
            
            if(this.nameserverMap.containsKey(domainParts[domainParts.length-1])){
            	StringBuilder sb = new StringBuilder();
            	for(int i = 0; i < domainParts.length-1; i++){
            		sb.append(domainParts[i]+".");
            	}
            	System.out.println(domainParts[domainParts.length-1]);
            	registerNameserver(sb.toString(), this.nameserverMap.get(domainParts[domainParts.length-1]), nameserverForChatserver);
            }else{
            	System.out.println(domainParts[domainParts.length-1]);
            	this.nameserverMap.put(domainParts[domainParts.length-1], nameserver);
            }
  
        /*} catch (NotBoundException e) {
            e.printStackTrace();
        }*/
    }

    // TODO !!!!!!!!!!!!!!!!!!!!!!!!
    @Override
    public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {
        String[] userParts = username.split("\\.");

        if(isRoot) {
            if (nameserverMap.containsKey(userParts[userParts.length - 1])) {               // deeper zone available
                String newUsername = "";
                for (int i = 1; i < userParts.length; i++) {
                    newUsername += userParts[i - 1] + ".";
                }
                nameserverMap.get(userParts[userParts.length - 1]).registerUser(newUsername, address);
            } else {                                                                    // no deeper zone available
                userList.add(new User(userParts[0], address, true));
            }
        }
    }

    @Override
    public INameserverForChatserver getNameserver(String zone) throws RemoteException {
        INameserverForChatserver ret;

        if(nameserverMap.containsKey(zone)){
            return nameserverMap.get(zone);
        } else {
            return null;
        }
    }

    @Override
    public String lookup(String username) throws RemoteException {
        String ret = "";

        for(User u : userList){
            if(u.getUsername().equals(username)){
                ret += u.getIp();
            }
        }

        return ret;
    }
}
