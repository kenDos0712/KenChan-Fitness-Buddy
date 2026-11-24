import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;


public class FitnessServer {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_FIELD = 40;

    private final Object lock = new Object();
    private final Map<String, Handler> clients = new LinkedHashMap<>();   
    private final Map<Integer, Session> sessions = new LinkedHashMap<>(); 
    private int nextId = 1;

    
    static class Session {
        int id;
        String host, activity, when, where;
        Set<String> going = new LinkedHashSet<>();
        Set<String> declined = new LinkedHashSet<>();

        String encode() {
            return "SESSION|" + id + "|" + host + "|" + activity + "|" + when + "|" + where
                    + "|" + String.join(";", going) + "|" + String.join(";", declined);
        }
    }

    private volatile ServerSocket serverSocket;
    private ExecutorService pool;

   
    public static void main(String[] args) throws Exception {
        int port = 5000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1 || port > 65535) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.err.println("Invalid port. Usage: java FitnessServer [port]");
                return;
            }
        }
        FitnessServer server = new FitnessServer();
        server.startAsync(port);
        System.out.println("=== KenChan Fitness Buddy Server running on port " + port + " ===");
        System.out.println("Clients on the same LAN can connect using one of these IPs:");
        for (String ip : localIps()) System.out.println("   " + ip);
        new java.util.concurrent.CountDownLatch(1).await(); 
    }

    
    public void startAsync(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "client-handler");
            t.setDaemon(true);
            return t;
        });
        Thread acceptor = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket s = serverSocket.accept();
                    pool.execute(new Handler(s));
                } catch (IOException e) {
                    break; // server socket closed
                }
            }
        }, "acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

  
    public void stop() {
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) { }
        synchronized (lock) {
            for (Handler h : new ArrayList<>(clients.values())) h.closeQuietly();
        }
        if (pool != null) pool.shutdownNow();
    }

    public static List<String> localIps() {
        List<String> ips = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses()))
                    if (addr instanceof Inet4Address) ips.add(addr.getHostAddress());
            }
        } catch (SocketException ignored) { }
        return ips;
    }

    private static String now() { return LocalTime.now().format(TIME); }

    private static String clean(String s) {
        s = s.replaceAll("[|;\\r\\n]", " ").trim();
        return s.length() > MAX_FIELD ? s.substring(0, MAX_FIELD) : s;
    }

    private void log(String msg) { System.out.println("[" + now() + "] " + msg); }

    
    private void broadcast(String line) {
        for (Handler h : clients.values()) h.send(line);
    }
    private void broadcastUsers() {
        broadcast("USERS|" + String.join(",", clients.keySet()));
    }
    private void broadcastSession(Session s) { broadcast(s.encode()); }
    private void broadcastAllSessions() { for (Session s : sessions.values()) broadcastSession(s); }

    private String validateNick(String nick, Handler self) {
        if (nick == null || nick.trim().isEmpty()) return "Nickname cannot be blank.";
        if (!nick.matches("[A-Za-z0-9_\\-]{1,15}"))
            return "Nickname must be 1-15 letters, numbers, _ or - (no spaces).";
        for (Map.Entry<String, Handler> e : clients.entrySet()) {
            if (e.getValue() != self && e.getKey().equalsIgnoreCase(nick))
                return "Nickname '" + nick + "' is already in use.";
        }
        return null;
    }

    private static void renameIn(Set<String> set, String oldN, String newN) {
        Set<String> copy = new LinkedHashSet<>();
        for (String x : set) copy.add(x.equals(oldN) ? newN : x);
        set.clear();
        set.addAll(copy);
    }

    // ---------------------------------------------------------------------
    class Handler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String nick;
        private boolean registered = false;

        Handler(Socket socket) { this.socket = socket; }

        /** Thread-safe: one line at a time per client. */
        synchronized void send(String line) {
            if (out != null) out.println(line);
        }

        void closeQuietly() {
            try { socket.close(); } catch (IOException ignored) { }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                socket.setSoTimeout(10000); // handshake must arrive quickly
                String hello = in.readLine();
                socket.setSoTimeout(0);
                if (hello == null || !hello.startsWith("HELLO|")) return;

                String want = hello.substring(6).trim();
                synchronized (lock) {
                    String err = validateNick(want, this);
                    if (err != null) {
                        send("NICKERR|" + err);
                        return;
                    }
                    nick = want;
                    clients.put(nick, this);
                    registered = true;
                    send("NICKOK|" + nick);
                    broadcastUsers();
                    for (Session s : sessions.values()) send(s.encode()); 
                    log(nick + " connected from " + socket.getInetAddress().getHostAddress());
                }

                String line;
                while ((line = in.readLine()) != null) {
                    if (!handle(line)) break;
                }
            } catch (IOException e) {
              
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            synchronized (lock) {
                if (registered) {
                    registered = false;
                    clients.remove(nick);
                    for (Session s : sessions.values()) {
                        s.going.remove(nick);
                        s.declined.remove(nick);
                    }
                    broadcastUsers();
                    broadcastAllSessions();
                    broadcast("SYS|" + now() + "|" + nick + " left the gym.");
                    log(nick + " disconnected");
                }
            }
            try { socket.close(); } catch (IOException ignored) { }
        }

        
        private boolean handle(String line) {
            int i = line.indexOf('|');
            String cmd = i < 0 ? line : line.substring(0, i);
            String rest = i < 0 ? "" : line.substring(i + 1);
            switch (cmd) {
                case "CHAT":    return chat(rest);
                case "PROPOSE": propose(rest); return true;
                case "RSVP":    rsvp(rest); return true;
                case "CANCEL":  cancel(rest); return true;
                default:
                    send("SYS|" + now() + "|Unknown message type rejected by server.");
                    return true;
            }
        }

        private boolean chat(String text) {
            text = text.trim();
            if (text.isEmpty()) {
                send("SYS|" + now() + "|Blank messages are not allowed.");
                return true;
            }
            if (text.startsWith("/")) return command(text);
            synchronized (lock) {
                broadcast("MSG|" + now() + "|" + nick + "|" + text);
            }
            return true;
        }

        private boolean command(String text) {
            String[] parts = text.split("\\s+", 2);
            switch (parts[0].toLowerCase()) {
                case "/exit":
                    send("BYE|");
                    return false;
                case "/help":
                    send("SYS|" + now() + "|Commands: /name <newname> - change nickname | /exit - leave | /help - show this list");
                    send("SYS|" + now() + "|Use the Workout Sessions panel to propose a workout and confirm attendance.");
                    return true;
                case "/name":
                    if (parts.length < 2 || parts[1].trim().isEmpty()) {
                        send("SYS|" + now() + "|Usage: /name <newname>");
                    } else {
                        rename(parts[1].trim());
                    }
                    return true;
                default:
                    send("SYS|" + now() + "|Unknown command. Type /help.");
                    return true;
            }
        }

        private void rename(String newName) {
            synchronized (lock) {
                String err = validateNick(newName, this);
                if (err != null) {
                    send("SYS|" + now() + "|" + err);
                    return;
                }
                if (newName.equals(nick)) {
                    send("SYS|" + now() + "|That is already your nickname.");
                    return;
                }
                String old = nick;
                clients.remove(old);
                nick = newName;
                clients.put(nick, this);
                for (Session s : sessions.values()) {
                    if (s.host.equals(old)) s.host = newName;
                    renameIn(s.going, old, newName);
                    renameIn(s.declined, old, newName);
                }
                send("NICKOK|" + nick);
                broadcastUsers();
                broadcastAllSessions();
                broadcast("SYS|" + now() + "|" + old + " is now known as " + nick + ".");
                log(old + " renamed to " + nick);
            }
        }

       
        private void propose(String rest) {
            String[] p = rest.split("\\|", -1);
            if (p.length < 3) {
                send("SYS|" + now() + "|Invalid workout proposal.");
                return;
            }
            String activity = clean(p[0]), when = clean(p[1]), where = clean(p[2]);
            if (activity.isEmpty() || when.isEmpty() || where.isEmpty()) {
                send("SYS|" + now() + "|Workout proposal needs activity, time and place.");
                return;
            }
            synchronized (lock) {
                Session s = new Session();
                s.id = nextId++;
                s.host = nick;
                s.activity = activity;
                s.when = when;
                s.where = where;
                s.going.add(nick);
                sessions.put(s.id, s);
                broadcastSession(s);
                broadcast("SYS|" + now() + "|" + nick + " proposed workout #" + s.id + ": "
                        + activity + " at " + when + ", " + where + ".");
            }
        }

        
        private void rsvp(String rest) {
            String[] p = rest.split("\\|", -1);
            if (p.length < 2) return;
            int id;
            try { id = Integer.parseInt(p[0].trim()); } catch (NumberFormatException e) { return; }
            boolean going = p[1].equals("GOING");
            if (!going && !p[1].equals("NOTGOING")) return;
            synchronized (lock) {
                Session s = sessions.get(id);
                if (s == null) {
                    send("SYS|" + now() + "|That workout session no longer exists.");
                    return;
                }
                if (going) { s.declined.remove(nick); s.going.add(nick); }
                else       { s.going.remove(nick);    s.declined.add(nick); }
                broadcastSession(s);
                broadcast("SYS|" + now() + "|" + nick + (going ? " is GOING to" : " can't make") + " workout #"
                        + id + " (" + s.activity + ").");
            }
        }

        private void cancel(String rest) {
            int id;
            try { id = Integer.parseInt(rest.trim()); } catch (NumberFormatException e) { return; }
            synchronized (lock) {
                Session s = sessions.get(id);
                if (s == null) return;
                if (!s.host.equals(nick)) {
                    send("SYS|" + now() + "|Only the host (" + s.host + ") can cancel this session.");
                    return;
                }
                sessions.remove(id);
                broadcast("SESSIONDEL|" + id);
                broadcast("SYS|" + now() + "|" + nick + " cancelled workout #" + id + " (" + s.activity + ").");
            }
        }
    }
}
