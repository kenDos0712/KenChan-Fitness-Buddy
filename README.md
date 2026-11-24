# Fitness Buddy

**A Java LAN group-chat application for students who coordinate workouts together.**

> Course: ITAS1 – Network Programming | Section: BSIT 2B SD
> Final Project (Pair) | Submission: November 23–27, 2026

---

## 1. Project Overview

Fitness Buddy is a Java Swing client and a multithreaded TCP server. Users on the same LAN join a shared room, chat as a group, and use a built-in **Workout Plan** panel to propose workout sessions and confirm attendance. Everything is synchronized through the server, so all clients always see the same state.

## 2. Members

| Name | GitHub Username | Role |
|------|-----------------|------|
| Kenjhon G. Dela Cruz| kendos0712 | Server and networking: FitnessServer, TCP sockets, thread-per-client handling, message protocol, nickname validation, workout session and RSVP logic, server-side testing |
| Christian S. Braza | christiansbraza26-del | GUI and client: FitnessClient Swing interface, chat panel, online list, Workout Plan panel and dialogs, client error handling, README, screenshots, test documentation |

## 3. Concept Proposal and Approval

- **Audience:** Students coordinating exercise (gym, jogging, sports) on campus.
- **Problem:** In ordinary group chats, workout plans get buried in messages. Nobody knows who is actually coming, and the time and place get lost.
- **Theme:** Fitness Buddy.
- **Original features:** (1) Workout session proposals and (2) attendance confirmations (RSVP), described in section 5.
- **Instructor approval:** `<Approved by Prof. ________ on ________>` *(fill in)*

## 4. User Problem and Theme

Plain messaging does not track who is attending. Fitness Buddy gives every proposed workout an ID, a host, an activity, a time, a place, and live **Going / Can't Make It** lists. All clients update at the same time.

## 5. Features

### Core features
- Server and multiple concurrent clients (thread per client, cached thread pool)
- Configurable connection (IP address and port)
- Unique, validated nicknames (1–15 characters: letters, numbers, `_`, `-`)
- Group chat with sender nickname and time (`HH:mm`), broadcast once to every client including the sender
- Online user list, updated on join, leave, and rename
- Join/leave/rename announcements
- Commands: `/name <newname>`, `/exit`, `/help`
- Blank-message rejection, invalid IP/port handling, clear connection-error messages
- Resources (sockets/streams) released on exit; networking runs off the GUI thread

### Original theme features
1. **Workout session proposals**: any user proposes a workout (activity, when, where). The server assigns an ID and the host is automatically marked as going. The session appears in every client's **Workout Plan** panel. Only the host can cancel it.
2. **Attendance confirmations (RSVP)**: users select a workout and press **I'm Going** or **Can't Make It**. The server updates the going/declined lists and broadcasts the change. If a user disconnects, they are removed from every session's lists.

## 6. Architecture

```
+----------------+        TCP (text lines)        +------------------------------+
| FitnessClient  | <----------------------------> |        FitnessServer         |
| (Swing GUI)    |   HELLO / CHAT / PROPOSE ...   |  acceptor thread             |
|  - EDT (UI)    |                                |  cached thread pool          |
|  - connector   |                                |   -> one Handler per client  |
|  - read loop   |                                |  shared state (lock):        |
|  - sender exec |                                |   clients, sessions          |
+----------------+                                +------------------------------+
        ^                                                       |
        |      MSG / SYS / USERS / SESSION / SESSIONDEL         |
        +--------------------- broadcast -----------------------+
```

**Message path (chat):** user types text → `sendChat()` → `CHAT|text` is queued on the single-thread sender executor → server `Handler.handle()` → `chat()` → `broadcast("MSG|time|nick|text")` under the shared lock → each client's `readLoop()` → `handle()` → `addMessage()` on the Event Dispatch Thread.

**Threading**
- Server: one acceptor thread plus a cached thread pool (one `Handler` per client). All shared-state changes and broadcasts happen inside `synchronized (lock)`. `Handler.send()` is synchronized so concurrent sends cannot interleave lines.
- Client: the connect and read loops run on background threads. Outgoing messages go through a single-thread executor. Swing updates use `SwingUtilities.invokeLater`.

### Message format

One message per line, fields separated by `|`.

**Client → Server**

| Message | Meaning |
|---------|---------|
| `HELLO\|nickname` | Register nickname on connect |
| `CHAT\|text` | Chat message, or a command if it starts with `/` |
| `PROPOSE\|activity\|when\|where` | Propose a workout |
| `RSVP\|id\|GOING` or `RSVP\|id\|NOTGOING` | Confirm or decline attendance |
| `CANCEL\|id` | Cancel a workout (host only) |
| `BYE\|` | Client leaving |

**Server → Client**

| Message | Meaning |
|---------|---------|
| `NICKOK\|nick` / `NICKERR\|reason` | Nickname accepted or rejected |
| `MSG\|time\|nick\|text` | Chat message |
| `SYS\|time\|text` | System notice (join, leave, errors, workout events) |
| `USERS\|nick1,nick2,...` | Current online users |
| `SESSION\|id\|host\|activity\|when\|where\|going;...\|declined;...` | Create or update a workout |
| `SESSIONDEL\|id` | Workout cancelled |
| `BYE\|` | Server acknowledges `/exit` |

The server removes the characters `|`, `;`, and line breaks from user fields and limits each field to 40 characters, so a user cannot break the protocol.

## 7. Commands

| Command | Description |
|---------|-------------|
| `/name <newname>` | Change nickname (validated; must be unique) |
| `/exit` | Leave the room |
| `/help` | Show available commands |

## 8. Setup Instructions

**Requirements**
- Java JDK 11 or newer (`java -version`)
- Any IDE (IntelliJ IDEA / Eclipse / VS Code) or the command line
- No external dependencies (standard Java library only)

**Compile**
```bash
javac FitnessServer.java FitnessClient.java
```

**Run the host**
```bash
java FitnessClient
```
Choose **HOST A ROOM**, enter a nickname and port (default `5000`), then click **Start Hosting**. The embedded server starts on the host computer and the host joins automatically.

*(Optional: run a standalone server with `java FitnessServer 5000`. It prints the LAN IP addresses to share.)*

**Join from another computer**
1. Find the host's LAN IP (printed by the standalone server, or run `ipconfig` / `ifconfig`).
2. Run `java FitnessClient` and choose **JOIN A ROOM**.
3. Enter a nickname, the host's IP, and the port, then click **Join Room**.

For development on one machine, use `localhost` as the IP.

**Troubleshooting**
- *Connection failed / timed out:* check the IP and port, and make sure both computers are on the same Wi-Fi/LAN.
- *Firewall:* allow Java through the firewall or open the chosen port on the host.
- *Port already in use:* choose another port.
- *"Nickname already in use":* pick a different nickname.

## 9. Screenshots

Place images in `/screenshots` and update the links.

| Screen | Image |
|--------|-------|
| Welcome menu | `screenshots/01-menu.png` |
| Host form | `screenshots/02-host.png` |
| Join form | `screenshots/03-join.png` |
| Chat with 3 clients | `screenshots/04-chat.png` |
| Workout proposal dialog | `screenshots/05-propose.png` |
| Workout Plan with RSVPs | `screenshots/06-workouts.png` |
| Error recovery (server unavailable) | `screenshots/07-error.png` |

## 10. Test Results

Fill in **Actual Result** and **Status** after running each test.

| # | Test Case | Steps | Expected Result | Actual Result | Status |
|---|-----------|-------|-----------------|---------------|--------|
| 1 | Three concurrent clients | Start server; connect 3 clients | All three show online | | |
| 2 | Message broadcast | Client A sends a message | All clients, including A, receive it once with nickname and time | | |
| 3 | Online list updates | Join / leave a client | Online list updates on all clients | | |
| 4 | Valid nickname change | `/name NewName` | Accepted; everyone is notified | | |
| 5 | Duplicate nickname | `/name` using an existing nickname, or join with a taken one | Rejected with a message | | |
| 6 | Blank message | Send empty/space-only text | Rejected, nothing broadcast | | |
| 7 | Invalid IP / port | Enter bad IP or non-numeric port | Helpful error shown, GUI stays responsive | | |
| 8 | Unavailable server | Join with no server running | "Connection failed" message, no freeze | | |
| 9 | Clean `/exit` | Send `/exit` | Client leaves; others notified | | |
| 10 | Abrupt disconnect | Kill a client process | Server and other clients continue; user removed from list | | |
| 11 | Continued chat | After a disconnect, chat between the remaining clients | Messages still delivered | | |
| 12 | Propose workout (Client A) | Propose a workout | Appears in all clients' Workout Plan | | |
| 13 | RSVP (Client B) | Select workout, click I'm Going / Can't Make It | Update appears on all clients | | |
| 14 | Cancel by non-host | Non-host cancels | Rejected with a message | | |
| 15 | Cancel by host | Host cancels | Workout removed from all clients | | |
| 16 | `/help` | Send `/help` | Command list displayed | | |
| 17 | Two-computer LAN test | Server on PC 1; clients on PC 1 and PC 2 | Communication works across machines | | |

## 11. Contribution Log

| Date | Member | Task | Commit Link |
|------|--------|------|-------------|
| 2026-11-__ | @kendos0712 | Built FitnessServer: TCP sockets, thread-per-client handling, nickname validation, and message protocol | https://github.com/kendos0712/<repo-name>/commit/<hash> |
| 2026-11-__ | @christiansbraza26-del | Built FitnessClient Swing GUI: welcome, host/join forms, chat panel, and online list | https://github.com/kendos0712/<repo-name>/commit/<hash> |
| 2026-11-__ | @kendos0712 | Added workout proposal, RSVP, and cancel logic on the server | https://github.com/kendos0712/<repo-name>/commit/<hash> |
| 2026-11-__ | @christiansbraza26-del | Added Workout Plan panel, tests, screenshots, and test documentation | https://github.com/kendos0712/<repo-name>/commit/<hash> |

## 12. Credits and AI Assistance

- Built with the Java standard library only (`java.net`, `java.io`, `javax.swing`, `java.util.concurrent`).
- AI assistance: `<state which tool(s) were used and for what, e.g., "Claude was used to help draft this README.">` *(edit or remove to match your actual use)*

## 13. Repository Structure

```
ITAS1-Section-Surname1-Surname2/
├── FitnessServer.java
├── FitnessClient.java
├── README.md
└── screenshots/
```
