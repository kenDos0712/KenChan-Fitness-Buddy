import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FitnessClient extends JFrame {

   

    private static final Color GREEN = new Color(34, 139, 84);
    private static final Color DARK = new Color(24, 62, 44);
    private static final Color BG = new Color(245, 247, 246);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(225, 231, 228);
    private static final Color MUTED = new Color(105, 115, 110);
    private static final Color OWN_BUBBLE = new Color(34, 139, 84);
    private static final Color OTHER_BUBBLE = new Color(235, 240, 237);
    private static final Color RED = new Color(190, 65, 65);

   

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    

    private final JTextField hostNick = new JTextField();
    private final JTextField hostPort = new JTextField("5000");

    private final JButton startHostBtn =
            new JButton("Start Hosting");

    private final JTextField joinNick = new JTextField();
    private final JTextField joinIp =
            new JTextField("localhost");
    private final JTextField joinPort =
            new JTextField("5000");

    private final JButton joinBtn =
            new JButton("Join Room");


    private final JLabel roomLabel =
            new JLabel("Not connected");

    private final JLabel statusLabel =
            new JLabel("● Offline");

    private final JButton leaveBtn =
            new JButton("Leave");

    private final JTextField input =
            new JTextField();

    private final JButton sendBtn =
            new JButton("➤");

   

    private final DefaultListModel<String> usersModel =
            new DefaultListModel<>();

    private final JList<String> usersList =
            new JList<>(usersModel);

   

    private final DefaultListModel<Workout> workoutModel =
            new DefaultListModel<>();

    private final JList<Workout> workoutList =
            new JList<>(workoutModel);


    private final JPanel messagesPanel =
            new JPanel();

    private final JScrollPane messagesScroll =
            new JScrollPane(messagesPanel);

   

    private final ExecutorService sender =
            Executors.newSingleThreadExecutor(r -> {

                Thread t = new Thread(r, "fitness-sender");

                t.setDaemon(true);

                return t;
            });

    private volatile Socket socket;
    private volatile PrintWriter out;

    private volatile boolean connected = false;
    private volatile boolean intentionalClose = false;

    private volatile FitnessServer server;

    private String myName = "";

   

    static class Workout {

        int id;

        String host;
        String activity;
        String when;
        String where;
        String going;
        String declined;

        static String safe(String s) {

            if (s == null) {
                return "";
            }

            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }

        int count(String s) {

            if (s == null || s.isEmpty()) {
                return 0;
            }

            return s.split(";").length;
        }

        @Override
        public String toString() {

            return "#" + id +
                    "  " + activity +
                    "  •  " + when;
        }
    }

   

    public FitnessClient() {

        super("KenChan Fitness Buddy");

        setDefaultCloseOperation(
                DO_NOTHING_ON_CLOSE
        );

        addWindowListener(
                new WindowAdapter() {

                    @Override
                    public void windowClosing(
                            WindowEvent e) {

                        shutdown();
                    }
                }
        );

        root.setBackground(BG);

        root.add(
                buildWelcome(),
                "menu"
        );

        root.add(
                buildForm(true),
                "host"
        );

        root.add(
                buildForm(false),
                "join"
        );

        root.add(
                buildChat(),
                "chat"
        );

        add(root);

        startHostBtn.addActionListener(
                e -> hostRoom()
        );

        joinBtn.addActionListener(
                e -> joinRoom()
        );

        setSize(1180, 720);

        setMinimumSize(
                new Dimension(980, 620)
        );

        setLocationRelativeTo(null);

        cards.show(root, "menu");
    }

   
    private JPanel buildWelcome() {

        JPanel p =
                new JPanel(new GridBagLayout());

        p.setBackground(GREEN);

        JPanel card =
                new RoundedPanel(
                        28,
                        Color.WHITE
                );

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBorder(
                new EmptyBorder(
                        42,
                        60,
                        42,
                        60
                )
        );

        card.setPreferredSize(
                new Dimension(520, 440)
        );

        JLabel logo =
                new JLabel("KC");

        logo.setOpaque(true);

        logo.setBackground(GREEN);

        logo.setForeground(Color.WHITE);

        logo.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        logo.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        30
                )
        );

        logo.setPreferredSize(
                new Dimension(72, 72)
        );

        logo.setMaximumSize(
                new Dimension(72, 72)
        );

        logo.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JLabel title =
                new JLabel("KenChan Fitness Buddy");

        title.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        36
                )
        );

        title.setForeground(DARK);

        title.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JLabel sub =
                new JLabel(
                        "Chat. Plan workouts. Stay together."
                );

        sub.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        15
                )
        );

        sub.setForeground(MUTED);

        sub.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JButton host =
                mainButton(
                        "HOST A ROOM",
                        GREEN
                );

        JButton join =
                mainButton(
                        "JOIN A ROOM",
                        DARK
                );

        host.addActionListener(
                e -> cards.show(root, "host")
        );

        join.addActionListener(
                e -> cards.show(root, "join")
        );

        card.add(logo);

        card.add(
                Box.createVerticalStrut(18)
        );

        card.add(title);

        card.add(
                Box.createVerticalStrut(5)
        );

        card.add(sub);

        card.add(
                Box.createVerticalStrut(32)
        );

        card.add(host);

        card.add(
                Box.createVerticalStrut(12)
        );

        card.add(join);

        card.add(
                Box.createVerticalStrut(20)
        );

        JLabel tip =
                new JLabel(
                        "TCP client-server • LAN group chat"
                );

        tip.setForeground(MUTED);

        tip.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        11
                )
        );

        tip.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        card.add(tip);

        p.add(card);

        return p;
    }

    
    private JButton mainButton(
            String text,
            Color color) {

        JButton b =
                new JButton(text);

        b.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        b.setMaximumSize(
                new Dimension(390, 48)
        );

        b.setPreferredSize(
                new Dimension(390, 48)
        );

        b.setBackground(color);

        b.setForeground(Color.WHITE);

        b.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        13
                )
        );

        b.setFocusPainted(false);

        b.setBorderPainted(false);

        b.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        return b;
    }

   

    private JPanel buildForm(
            boolean hosting) {

        JPanel outer =
                new JPanel(
                        new GridBagLayout()
                );

        outer.setBackground(BG);

        JPanel card =
                new RoundedPanel(
                        24,
                        CARD
                );

        card.setLayout(
                new BoxLayout(
                        card,
                        BoxLayout.Y_AXIS
                )
        );

        card.setBorder(
                new EmptyBorder(
                        30,
                        40,
                        30,
                        40
                )
        );

        card.setPreferredSize(
                new Dimension(
                        540,
                        hosting ? 430 : 490
                )
        );

        JLabel icon =
                new JLabel(
                        hosting ? "HOST" : "JOIN"
                );

        icon.setForeground(GREEN);

        icon.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        13
                )
        );

        icon.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JLabel title =
                new JLabel(
                        hosting
                                ? "Create a Fitness Room"
                                : "Join a Fitness Room"
                );

        title.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        27
                )
        );

        title.setForeground(DARK);

        title.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JLabel hint =
                new JLabel(
                        hosting
                                ? "Start the server on this computer, then share your LAN IP."
                                : "Enter the host computer's IP address and port."
                );

        hint.setForeground(MUTED);

        hint.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        13
                )
        );

        hint.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        card.add(icon);

        card.add(
                Box.createVerticalStrut(8)
        );

        card.add(title);

        card.add(
                Box.createVerticalStrut(5)
        );

        card.add(hint);

        card.add(
                Box.createVerticalStrut(28)
        );

        if (hosting) {

            addField(
                    card,
                    "Your nickname",
                    hostNick
            );

            addField(
                    card,
                    "Port",
                    hostPort
            );

        } else {

            addField(
                    card,
                    "Your nickname",
                    joinNick
            );

            addField(
                    card,
                    "Host IP address",
                    joinIp
            );

            addField(
                    card,
                    "Port",
                    joinPort
            );
        }

        JButton primary =
                hosting
                        ? startHostBtn
                        : joinBtn;

        stylePrimary(primary);

        card.add(
                Box.createVerticalStrut(14)
        );

        card.add(primary);

        JButton back =
                new JButton("← Back");

        back.setBorderPainted(false);

        back.setContentAreaFilled(false);

        back.setForeground(MUTED);

        back.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        back.addActionListener(
                e -> cards.show(root, "menu")
        );

        card.add(
                Box.createVerticalStrut(8)
        );

        card.add(back);

        outer.add(card);

        return outer;
    }

    

    private void addField(
            JPanel panel,
            String label,
            JTextField field) {

        JLabel l =
                new JLabel(label);

        l.setForeground(DARK);

        l.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        12
                )
        );

        l.setAlignmentX(
                Component.LEFT_ALIGNMENT
        );

        panel.add(l);

        panel.add(
                Box.createVerticalStrut(6)
        );

        field.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        14
                )
        );

        field.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        40
                )
        );

        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        new EmptyBorder(
                                8,
                                10,
                                8,
                                10
                        )
                )
        );

        panel.add(field);

        panel.add(
                Box.createVerticalStrut(15)
        );
    }

   
    private void stylePrimary(
            JButton b) {

        b.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        b.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        44
                )
        );

        b.setBackground(GREEN);

        b.setForeground(Color.WHITE);

        b.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        13
                )
        );

        b.setFocusPainted(false);

        b.setBorderPainted(false);
    }


    private JPanel buildChat() {

        JPanel main =
                new JPanel(new BorderLayout());

        main.setBackground(BG);

       
        JPanel top =
                new JPanel(new BorderLayout());

        top.setBackground(Color.WHITE);

        top.setBorder(
                new EmptyBorder(
                        12,
                        18,
                        12,
                        18
                )
        );

        JLabel avatar =
                new JLabel("KC");

        avatar.setOpaque(true);

        avatar.setBackground(GREEN);

        avatar.setForeground(Color.WHITE);

        avatar.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        avatar.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        16
                )
        );

        avatar.setPreferredSize(
                new Dimension(45, 45)
        );

        JPanel titlePanel =
                new JPanel();

        titlePanel.setOpaque(false);

        titlePanel.setLayout(
                new BoxLayout(
                        titlePanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title =
                new JLabel("KenChan Fittness Buddy");

        title.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        17
                )
        );

        title.setForeground(DARK);

        roomLabel.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        11
                )
        );

        roomLabel.setForeground(MUTED);

        titlePanel.add(title);

        titlePanel.add(roomLabel);

        JPanel left =
                new JPanel(new FlowLayout(
                        FlowLayout.LEFT,
                        12,
                        0
                ));

        left.setOpaque(false);

        left.add(avatar);

        left.add(titlePanel);

        statusLabel.setForeground(GREEN);

        statusLabel.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        12
                )
        );

        leaveBtn.setForeground(RED);

        leaveBtn.setBackground(Color.WHITE);

        leaveBtn.setFocusPainted(false);

        leaveBtn.addActionListener(
                e -> leaveRoom()
        );

        top.add(
                left,
                BorderLayout.WEST
        );

        JPanel right =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.RIGHT,
                                10,
                                0
                        )
                );

        right.setOpaque(false);

        right.add(statusLabel);

        right.add(leaveBtn);

        top.add(
                right,
                BorderLayout.EAST
        );

        main.add(
                top,
                BorderLayout.NORTH
        );


        JPanel sidebar =
                new JPanel(new BorderLayout());

        sidebar.setPreferredSize(
                new Dimension(220, 0)
        );

        sidebar.setBackground(
                new Color(239, 243, 241)
        );

        sidebar.setBorder(
                new EmptyBorder(
                        15,
                        12,
                        15,
                        12
                )
        );

        JLabel messagesTitle =
                new JLabel("MESSAGES");

        messagesTitle.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        11
                )
        );

        messagesTitle.setForeground(MUTED);

        sidebar.add(
                messagesTitle,
                BorderLayout.NORTH
        );

        JPanel userPanel =
                new JPanel(new BorderLayout());

        userPanel.setBackground(
                Color.WHITE
        );

        userPanel.setBorder(
                new EmptyBorder(
                        12,
                        10,
                        12,
                        10
                )
        );

        JLabel group =
                new JLabel(
                        "<html><b>Fitness Group</b><br>" +
                        "<font color='#69736e'>Group Chat</font></html>"
                );

        userPanel.add(
                group,
                BorderLayout.CENTER
        );

        sidebar.add(
                userPanel,
                BorderLayout.CENTER
        );

        JPanel onlinePanel =
                new JPanel(new BorderLayout());

        onlinePanel.setOpaque(false);

        JLabel onlineTitle =
                new JLabel("ONLINE");

        onlineTitle.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        11
                )
        );

        onlineTitle.setForeground(MUTED);

        usersList.setBackground(
                new Color(239, 243, 241)
        );

        usersList.setBorder(null);

        usersList.setCellRenderer(
                new UserRenderer()
        );

        onlinePanel.add(
                onlineTitle,
                BorderLayout.NORTH
        );

        onlinePanel.add(
                new JScrollPane(usersList),
                BorderLayout.CENTER
        );

        sidebar.add(
                onlinePanel,
                BorderLayout.SOUTH
        );

        
        onlinePanel.setPreferredSize(
                new Dimension(190, 230)
        );

        main.add(
                sidebar,
                BorderLayout.WEST
        );

       

        messagesPanel.setLayout(
                new BoxLayout(
                        messagesPanel,
                        BoxLayout.Y_AXIS
                )
        );

        messagesPanel.setBackground(BG);

        messagesScroll.setBorder(null);

        messagesScroll.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );

        messagesScroll.getVerticalScrollBar()
                .setUnitIncrement(16);

        JPanel center =
                new JPanel(new BorderLayout());

        center.setBackground(BG);

        center.add(
                messagesScroll,
                BorderLayout.CENTER
        );

      
        JPanel inputPanel =
                new JPanel(new BorderLayout(8, 0));

        inputPanel.setBackground(Color.WHITE);

        inputPanel.setBorder(
                new EmptyBorder(
                        10,
                        12,
                        10,
                        12
                )
        );

        input.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        14
                )
        );

        input.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                BORDER
                        ),
                        new EmptyBorder(
                                8,
                                12,
                                8,
                                12
                        )
                )
        );

        sendBtn.setPreferredSize(
                new Dimension(50, 42)
        );

        sendBtn.setBackground(GREEN);

        sendBtn.setForeground(Color.WHITE);

        sendBtn.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        18
                )
        );

        sendBtn.setFocusPainted(false);

        sendBtn.setBorderPainted(false);

        sendBtn.addActionListener(
                e -> sendChat()
        );

        input.addActionListener(
                e -> sendChat()
        );

        inputPanel.add(
                input,
                BorderLayout.CENTER
        );

        inputPanel.add(
                sendBtn,
                BorderLayout.EAST
        );

        center.add(
                inputPanel,
                BorderLayout.SOUTH
        );

        main.add(
                center,
                BorderLayout.CENTER
        );

      

        JPanel workoutPanel =
                new JPanel(new BorderLayout());

        workoutPanel.setPreferredSize(
                new Dimension(275, 0)
        );

        workoutPanel.setBackground(Color.WHITE);

        workoutPanel.setBorder(
                new EmptyBorder(
                        15,
                        12,
                        15,
                        12
                )
        );

        JLabel workoutTitle =
                new JLabel("WORKOUT PLAN");

        workoutTitle.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        13
                )
        );

        workoutTitle.setForeground(DARK);

        workoutPanel.add(
                workoutTitle,
                BorderLayout.NORTH
        );

        workoutList.setBackground(
                Color.WHITE
        );

        workoutList.setCellRenderer(
                new WorkoutRenderer()
        );

        workoutList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        workoutPanel.add(
                new JScrollPane(workoutList),
                BorderLayout.CENTER
        );

        JPanel workoutButtons =
                new JPanel();

        workoutButtons.setLayout(
                new BoxLayout(
                        workoutButtons,
                        BoxLayout.Y_AXIS
                )
        );

        workoutButtons.setBackground(Color.WHITE);

        JButton propose =
                new JButton("+ Propose Workout");

        JButton going =
                new JButton("✓ I'm Going");

        JButton notGoing =
                new JButton("× Can't Make It");

        JButton cancel =
                new JButton("Cancel Selected");

        styleSmallButton(propose, GREEN);
        styleSmallButton(going, GREEN);
        styleSmallButton(notGoing, RED);
        styleSmallButton(cancel, DARK);

        propose.addActionListener(
                e -> proposeDialog()
        );

        going.addActionListener(
                e -> rsvp(true)
        );

        notGoing.addActionListener(
                e -> rsvp(false)
        );

        cancel.addActionListener(
                e -> cancelSelected()
        );

        workoutButtons.add(propose);

        workoutButtons.add(
                Box.createVerticalStrut(7)
        );

        workoutButtons.add(going);

        workoutButtons.add(
                Box.createVerticalStrut(7)
        );

        workoutButtons.add(notGoing);

        workoutButtons.add(
                Box.createVerticalStrut(7)
        );

        workoutButtons.add(cancel);

        workoutPanel.add(
                workoutButtons,
                BorderLayout.SOUTH
        );

        main.add(
                workoutPanel,
                BorderLayout.EAST
        );

        return main;
    }

   

    private void styleSmallButton(
            JButton b,
            Color color) {

        b.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        b.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        38
                )
        );

        b.setBackground(color);

        b.setForeground(Color.WHITE);

        b.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        11
                )
        );

        b.setFocusPainted(false);

        b.setBorderPainted(false);
    }


    private void hostRoom() {

        String name =
                hostNick.getText().trim();

        if (name.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter your nickname."
            );

            return;
        }

        int port;

        try {

            port = Integer.parseInt(
                    hostPort.getText().trim()
            );

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid port number."
            );

            return;
        }

        try {

            server = new FitnessServer();

            server.startAsync(port);

            myName = name;

            intentionalClose = false;

            connectBlocking(
                    "127.0.0.1",
                    port,
                    name
            );

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Could not start server:\n"
                    + e.getMessage()
            );
        }
    }

  
    private void joinRoom() {

        String name =
                joinNick.getText().trim();

        String ip =
                joinIp.getText().trim();

        if (name.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter your nickname."
            );

            return;
        }

        int port;

        try {

            port = Integer.parseInt(
                    joinPort.getText().trim()
            );

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Invalid port number."
            );

            return;
        }

        myName = name;

        intentionalClose = false;

        connectBlocking(
                ip,
                port,
                name
        );
    }

   

    private void connectBlocking(
            String host,
            int port,
            String nickname) {

        new Thread(() -> {

            try {

                Socket s =
                        new Socket();

                s.connect(
                        new InetSocketAddress(
                                host,
                                port
                        ),
                        5000
                );

                socket = s;

                out = new PrintWriter(
                        new OutputStreamWriter(
                                s.getOutputStream(),
                                StandardCharsets.UTF_8
                        ),
                        true
                );

                connected = true;

                send(
                        "HELLO|" + nickname
                );

                SwingUtilities.invokeLater(() -> {

                    roomLabel.setText(
                            host + ":" + port
                    );

                    statusLabel.setText(
                            "● Online"
                    );

                    statusLabel.setForeground(
                            GREEN
                    );

                    cards.show(
                            root,
                            "chat"
                    );

                    input.requestFocusInWindow();
                });

                readLoop();

            } catch (Exception e) {

                SwingUtilities.invokeLater(() -> {

                    JOptionPane.showMessageDialog(
                            this,
                            "Connection failed:\n"
                            + e.getMessage()
                    );
                });
            }

        }, "fitness-connector").start();
    }

   
    private void readLoop() {

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    );

            String line;

            while (
                    connected
                    &&
                    (line = reader.readLine()) != null
            ) {

                handle(line);
            }

        } catch (IOException e) {

            if (!intentionalClose) {

                SwingUtilities.invokeLater(() -> {

                    statusLabel.setText(
                            "● Disconnected"
                    );

                    statusLabel.setForeground(
                            RED
                    );
                });
            }

        } finally {

            connected = false;
        }
    }

    

    private void handle(String line) {

        if (line == null) {
            return;
        }

        String[] parts =
                line.split("\\|", -1);

        if (parts.length == 0) {
            return;
        }

        String type = parts[0];

      

        if (type.equals("NICKOK")) {

            return;
        }

      

        if (type.equals("NICKERR")) {

            SwingUtilities.invokeLater(() -> {

                JOptionPane.showMessageDialog(
                        this,
                        parts.length > 1
                                ? parts[1]
                                : "Nickname error."
                );

                leaveRoom();
            });

            return;
        }

       
        if (type.equals("MSG")) {

            if (parts.length >= 4) {

                String time = parts[1];
                String senderName = parts[2];
                String text = parts[3];

                addMessage(
                        senderName,
                        time,
                        text,
                        senderName.equals(myName)
                );
            }

            return;
        }


        if (type.equals("SYS")) {

            if (parts.length >= 3) {

                addSystemMessage(
                        parts[1],
                        parts[2]
                );
            }

            return;
        }


        if (type.equals("USERS")) {

            usersModel.clear();

            for (int i = 1; i < parts.length; i++) {

                if (!parts[i].trim().isEmpty()) {

                    usersModel.addElement(
                            parts[i]
                    );
                }
            }

            return;
        }


        if (type.equals("PROPOSE")) {

            if (parts.length >= 4) {

                String activity = parts[1];
                String when = parts[2];
                String where = parts[3];

                addSystemMessage(
                        "",
                        "Workout proposed: "
                        + activity
                        + " • "
                        + when
                        + " • "
                        + where
                );
            }

            return;
        }

    
        if (type.equals("SESSION")) {

            if (parts.length >= 7) {

                try {

                    int id =
                            Integer.parseInt(
                                    parts[1]
                            );

                    Workout w =
                            new Workout();

                    w.id = id;
                    w.host = parts[2];
                    w.activity = parts[3];
                    w.when = parts[4];
                    w.where = parts[5];
                    w.going = parts[6];

                    if (parts.length > 7) {
                        w.declined = parts[7];
                    }

                    upsertWorkout(w);

                } catch (NumberFormatException ignored) {
                }
            }

            return;
        }

     

        if (type.equals("SESSIONDEL")) {

            if (parts.length >= 2) {

                try {

                    int id =
                            Integer.parseInt(
                                    parts[1]
                            );

                    for (int i = 0;
                         i < workoutModel.size();
                         i++) {

                        if (workoutModel
                                .get(i)
                                .id == id) {

                            workoutModel.remove(i);

                            break;
                        }
                    }

                } catch (NumberFormatException ignored) {
                }
            }

            return;
        }
    }

   

    private void addMessage(
            String senderName,
            String time,
            String text,
            boolean own) {

        SwingUtilities.invokeLater(() -> {

            JPanel row =
                    new JPanel(
                            new FlowLayout(
                                    own
                                            ? FlowLayout.RIGHT
                                            : FlowLayout.LEFT,
                                    12,
                                    6
                            )
                    );

            row.setOpaque(false);

            JPanel bubble =
                    new JPanel();

            bubble.setLayout(
                    new BorderLayout(
                            5,
                            4
                    )
            );

            bubble.setBorder(
                    new EmptyBorder(
                            9,
                            12,
                            9,
                            12
                    )
            );

            bubble.setBackground(
                    own
                            ? OWN_BUBBLE
                            : OTHER_BUBBLE
            );

            JLabel name =
                    new JLabel(
                            senderName
                    );

            name.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            11
                    )
            );

            name.setForeground(
                    own
                            ? Color.WHITE
                            : DARK
            );

            JTextArea message =
                    new JTextArea(text);

            message.setEditable(false);

            message.setWrapStyleWord(true);

            message.setLineWrap(true);

            message.setOpaque(false);

            message.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.PLAIN,
                            13
                    )
            );

            message.setForeground(
                    own
                            ? Color.WHITE
                            : DARK
            );

            message.setRows(
                    Math.max(
                            1,
                            Math.min(
                                    6,
                                    (text.length() / 45) + 1
                            )
                    )
            );

            message.setColumns(28);

            JLabel timestamp =
                    new JLabel(time);

            timestamp.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.PLAIN,
                            9
                    )
            );

            timestamp.setForeground(
                    own
                            ? new Color(220, 245, 230)
                            : MUTED
            );

            bubble.add(
                    name,
                    BorderLayout.NORTH
            );

            bubble.add(
                    message,
                    BorderLayout.CENTER
            );

            bubble.add(
                    timestamp,
                    BorderLayout.SOUTH
            );

            row.add(bubble);

            messagesPanel.add(row);

            messagesPanel.revalidate();

            messagesPanel.repaint();

            SwingUtilities.invokeLater(() -> {

                JScrollBar bar =
                        messagesScroll
                                .getVerticalScrollBar();

                bar.setValue(
                        bar.getMaximum()
                );
            });
        });
    }


    private void addSystemMessage(
            String time,
            String text) {

        SwingUtilities.invokeLater(() -> {

            JLabel label =
                    new JLabel(
                            "<html><center>"
                            + text
                            + "</center></html>"
                    );

            label.setForeground(MUTED);

            label.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.ITALIC,
                            11
                    )
            );

            label.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            JPanel row =
                    new JPanel();

            row.setOpaque(false);

            row.setBorder(
                    new EmptyBorder(
                            8,
                            10,
                            8,
                            10
                    )
            );

            row.add(label);

            messagesPanel.add(row);

            messagesPanel.revalidate();

            messagesPanel.repaint();

            SwingUtilities.invokeLater(() -> {

                JScrollBar bar =
                        messagesScroll
                                .getVerticalScrollBar();

                bar.setValue(
                        bar.getMaximum()
                );
            });
        });
    }

   
    private void sendChat() {

        String text =
                input.getText().trim();

        if (text.isEmpty()) {
            return;
        }

        if (!connected) {

            JOptionPane.showMessageDialog(
                    this,
                    "You are not connected."
            );

            return;
        }

        send(
                "CHAT|" +
                text.replace("|", " ")
        );

        input.setText("");
    }

   
    private void send(String message) {

        if (out == null) {
            return;
        }

        sender.submit(() -> {

            try {

                out.println(message);

            } catch (Exception ignored) {
            }
        });
    }

    

    private void proposeDialog() {

        JTextField activity =
                new JTextField();

        JTextField when =
                new JTextField();

        JTextField where =
                new JTextField();

        JPanel panel =
                new JPanel();

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );

        panel.add(
                new JLabel("Activity:")
        );

        panel.add(activity);

        panel.add(
                Box.createVerticalStrut(8)
        );

        panel.add(
                new JLabel("When:")
        );

        panel.add(when);

        panel.add(
                Box.createVerticalStrut(8)
        );

        panel.add(
                new JLabel("Where:")
        );

        panel.add(where);

        int result =
                JOptionPane.showConfirmDialog(
                        this,
                        panel,
                        "Propose Workout",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String a =
                activity.getText().trim();

        String w =
                when.getText().trim();

        String place =
                where.getText().trim();

        if (
                a.isEmpty()
                ||
                w.isEmpty()
                ||
                place.isEmpty()
        ) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please complete all fields."
            );

            return;
        }

        a = a.replace("|", " ");
        w = w.replace("|", " ");
        place = place.replace("|", " ");

        send(
                "PROPOSE|"
                + a
                + "|"
                + w
                + "|"
                + place
        );
    }

   
    private void rsvp(
            boolean going) {

        Workout selected =
                workoutList.getSelectedValue();

        if (selected == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select a workout first."
            );

            return;
        }

        send(
                "RSVP|"
                + selected.id
                + "|"
                + (going
                        ? "GOING"
                        : "NOTGOING")
        );
    }

  

    private void cancelSelected() {

        Workout selected =
                workoutList.getSelectedValue();

        if (selected == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select a workout first."
            );

            return;
        }

        int confirm =
                JOptionPane.showConfirmDialog(
                        this,
                        "Cancel workout #"
                        + selected.id
                        + "?",
                        "Confirm",
                        JOptionPane.YES_NO_OPTION
                );

        if (
                confirm
                ==
                JOptionPane.YES_OPTION
        ) {

            send(
                    "CANCEL|"
                    + selected.id
            );
        }
    }


    private void upsertWorkout(
            Workout workout) {

        SwingUtilities.invokeLater(() -> {

            for (int i = 0;
                 i < workoutModel.size();
                 i++) {

                if (
                        workoutModel
                                .get(i)
                                .id
                        ==
                        workout.id
                ) {

                    workoutModel.set(
                            i,
                            workout
                    );

                    return;
                }
            }

            workoutModel.addElement(
                    workout
            );
        });
    }

   

    private void leaveRoom() {

        intentionalClose = true;

        try {

            if (out != null) {

                out.println("BYE|");
            }

        } catch (Exception ignored) {
        }

        try {

            if (socket != null) {

                socket.close();
            }

        } catch (IOException ignored) {
        }

        connected = false;

        SwingUtilities.invokeLater(() -> {

            statusLabel.setText(
                    "● Offline"
            );

            statusLabel.setForeground(
                    RED
            );

            roomLabel.setText(
                    "Not connected"
            );

            cards.show(
                    root,
                    "menu"
            );
        });
    }

   
    private void shutdown() {

        intentionalClose = true;

        try {

            if (out != null) {

                out.println("BYE|");
            }

        } catch (Exception ignored) {
        }

        try {

            if (socket != null) {

                socket.close();
            }

        } catch (IOException ignored) {
        }

        try {

            if (server != null) {

                server.stop();
            }

        } catch (Exception ignored) {
        }

        connected = false;

        sender.shutdownNow();

        dispose();

        System.exit(0);
    }

   

    static class RoundedPanel
            extends JPanel {

        private final int radius;

        RoundedPanel(
                int radius,
                Color background) {

            this.radius = radius;

            setBackground(
                    background
            );

            setOpaque(false);
        }

        @Override
        protected void paintComponent(
                Graphics g) {

            Graphics2D g2 =
                    (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(
                    getBackground()
            );

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    radius,
                    radius
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

   

    static class UserRenderer
            extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focus) {

            JLabel label =
                    (JLabel) super
                            .getListCellRendererComponent(
                                    list,
                                    value,
                                    index,
                                    selected,
                                    focus
                            );

            label.setText(
                    "●  " + value
            );

            label.setForeground(
                    GREEN
            );

            label.setBackground(
                    list.getBackground()
            );

            label.setBorder(
                    new EmptyBorder(
                            7,
                            4,
                            7,
                            4
                    )
            );

            return label;
        }
    }

   

    static class WorkoutRenderer
            extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focus) {

            JLabel label =
                    (JLabel) super
                            .getListCellRendererComponent(
                                    list,
                                    value,
                                    index,
                                    selected,
                                    focus
                            );

            Workout w =
                    (Workout) value;

            label.setText(
                    "<html>"
                    + "<b>#"
                    + w.id
                    + " "
                    + Workout.safe(w.activity)
                    + "</b><br>"
                    + "<font color='#69736e'>"
                    + Workout.safe(w.when)
                    + "<br>"
                    + Workout.safe(w.where)
                    + "</font>"
                    + "</html>"
            );

            label.setBorder(
                    new EmptyBorder(
                            10,
                            8,
                            10,
                            8
                    )
            );

            if (selected) {

                label.setBackground(
                        new Color(
                                224,
                                241,
                                231
                        )
                );

            } else {

                label.setBackground(
                        Color.WHITE
                );
            }

            label.setForeground(DARK);

            return label;
        }
    }

  
    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                UIManager.setLookAndFeel(
                        UIManager
                                .getSystemLookAndFeelClassName()
                );

            } catch (Exception ignored) {
            }

            FitnessClient app =
                    new FitnessClient();

            app.setVisible(true);
        });
    }
}