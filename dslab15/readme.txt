Reflect about your solution!

Summary:
-) TUWEL Template, Application is working with predefined shell class.
-) Implemented User class to handle Client data.
-) Dslab port range 14790 - 14979, Account dslab1228773.

Server:
-) 2 Listener Threads (TCP, UDP)
-) List<User> and List<PrintWriter> working as reference between Server class and Threads
-) ExecutorService implemented (Fixed Thread pool, size = 100)

Client:
-) 3 Listener Threads (TCP public&private, UDP)
-) Thread safe BlockingQueue used as pipe between TCP public Listener Thread and Client class
-) StringBuilder lastMessage and username working as reference between Client and public Listener thread


Stage 1:
-) Registration of nameservers work top-down.
-) !nameservers show all registered nameservers of this nameserver
-) !addresses show all registered users of one nameserver

Stage 2:
-) You can successfully authenticate a user on a Chatserver
-) encrypt of the command works, but because the decryption of the commands doesn't work,
   so the commands doesn't work

Stage 3:
-) The communication between two clients is secure
