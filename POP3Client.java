import java.net.Socket;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

//used to get emails from a server
public class POP3Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String sendCommand(String command) throws IOException {
        out.print(command + "\r\n");
        out.flush();
        return in.readLine();
    }

    private void ensureOkResponse(String response, String command) throws IOException {
        if (response == null || !response.startsWith("+OK")) {
            throw new IOException(command + " failed: " + response);
        }
    }

    private List<String> readMultilineResponse() throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            if (".".equals(line)) {
                break;
            }
            // POP3 dot-stuffing: ".." at line start means literal ".".
            if (line.startsWith("..")) {
                line = line.substring(1);
            }
            lines.add(line);
        }
        return lines;
    }
    //opens socket connection to pop3 server 
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        
        // Read greeting
        String greeting = in.readLine();
        System.out.println("Server: " + greeting);
    }
    
    public void login(String username, String password) throws IOException {
        // Send USER command
        out.print("USER " + username + "\r\n");
        out.flush();
        String response = in.readLine();
        System.out.println("USER response: " + response);
        
        // Send PASS command
        out.print("PASS " + password + "\r\n");
        out.flush();
        response = in.readLine();
        System.out.println("PASS response: " + response);
    }

    public int getStat() throws IOException {
        String response = sendCommand("STAT");
        ensureOkResponse(response, "STAT");

        String[] parts = response.split("\\s+");
        if (parts.length < 2) {
            throw new IOException("Unexpected STAT response: " + response);
        }
        return Integer.parseInt(parts[1]);
    }

    public List<String> listMessages() throws IOException {
        String response = sendCommand("LIST");
        ensureOkResponse(response, "LIST");
        return readMultilineResponse();
    }

    public String retrieveMessage(int msgNum) throws IOException {
        String response = sendCommand("RETR " + msgNum);
        ensureOkResponse(response, "RETR");

        List<String> lines = readMultilineResponse();
        return String.join("\n", lines);
    }

    public String getMessageHeaders(int msgNum) throws IOException {
        String response = sendCommand("TOP " + msgNum + " 0");
        ensureOkResponse(response, "TOP");

        List<String> lines = readMultilineResponse();
        return String.join("\n", lines);
    }

    // gets the unique-id listing for all messages
    // RFC 1939 section 7 (optional POP3 commands)
    public List<String> listUniqueIds() throws IOException {
        String response = sendCommand("UIDL");
        ensureOkResponse(response, "UIDL");
        return readMultilineResponse();
    }

    // gets the unique-id for one message number
    // RFC 1939 section 7 (UIDL message-number)
    public String getMessageUid(int msgNum) throws IOException {
        String response = sendCommand("UIDL " + msgNum);
        ensureOkResponse(response, "UIDL");

        String[] parts = response.split("\\s+");
        if (parts.length < 3) {
            throw new IOException("Unexpected UIDL response: " + response);
        }
        return parts[2];
    }

    public boolean deleteMessage(int msgNum) throws IOException {
        String response = sendCommand("DELE " + msgNum);
        return response != null && response.startsWith("+OK");
    }

    // resets transaction state and clears any pending deletions
    // RFC 1939 section 6 (RSET command)
    public boolean resetTransactionState() throws IOException {
        String response = sendCommand("RSET");
        return response != null && response.startsWith("+OK");
    }

    public void quit() throws IOException {
        try {
            if (out != null && in != null) {
                String response = sendCommand("QUIT");
                ensureOkResponse(response, "QUIT");
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("Use MailViewer for mailbox listing and actions.");
        MailViewer.main(args);
    }
}