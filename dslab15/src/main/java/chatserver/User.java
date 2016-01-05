package chatserver;

import java.io.PrintWriter;
import java.util.Comparator;

public class User {
	private String username;
	private String password;
	private String ip;
	private String port;
	private boolean online;
	private boolean isRegistered;
	private PrintWriter printWriter;
	
	public User(String username, String password){
		this.username = username;
		this.password = password; 
		online = false;
		isRegistered = false;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isOnline() {
		return online;
	}
	
	public String isOnlineString(){
		if(online){
			return "online";
		}else{
			return "offline";
		}
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public boolean isRegistered() {
		return isRegistered;
	}

	public void setRegistered(boolean isRegistered) {
		this.isRegistered = isRegistered;
	}

	public PrintWriter getPrintWriter() {
		return printWriter;
	}

	public void setPrintWriter(PrintWriter printWriter) {
		this.printWriter = printWriter;
	}

	
}
