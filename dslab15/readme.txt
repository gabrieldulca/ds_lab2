Reflect about your solution!

Summary:
-) TUWEL Template, Application is working with predefined shell class.
-) Implemented User class to handle Client data.
-) Dslab port range 14790 - 14979, Account dslab1228773.
-) "ant test" is not working --> chatserver does not start. I tested with multiple terminal windows
	and "ant run-client", "ant run-server" (local and in lab environment remote)

Server:
-) 2 Listener Threads (TCP, UDP)
-) List<User> and List<PrintWriter> working as reference between Server class and Threads
-) ExecutorService implemented (Fixed Thread pool, size = 100)

Client:
-) 3 Listener Threads (TCP public&private, UDP)
-) Thread safe BlockingQueue used as pipe between TCP public Listener Thread and Client class
-) StringBuilder lastMessage and username working as reference between Client and public Listener thread