import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Used to send emails through an SMTP server.
public class SMTPClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // connects to an SMTP server socket
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        String greeting = in.readLine();
        ensureResponseStartsWith(greeting, "220", "CONNECT");
        System.out.println("Server: " + greeting);
    }

    // sends a command to the server and returns the response
    private String sendCommand(String command) throws IOException {
        out.print(command + "\r\n");
        out.flush();
        return in.readLine();
    }

    // validates SMTP response codes and distinguishes transient 4xx from permanent 5xx failures
    // RFC 5321 section 4.2.1 (reply code classes)
    private void ensureResponseStartsWith(String response, String expectedCode, String command) throws IOException {
        if (response == null) {
            throw new IOException(command + " failed: null response from server");
        }
        if (response.startsWith(expectedCode)) {
            return;
        }
        if (response.startsWith("4")) {
            throw new IOException(command + " transient SMTP error (4xx): " + response);
        }
        if (response.startsWith("5")) {
            throw new IOException(command + " permanent SMTP error (5xx): " + response);
        }
        throw new IOException(command + " failed with unexpected SMTP response: " + response);
    }

//section 1: sending commands to the server
    // sends a HELO command to the server
    public void sendHelo(String domain) throws IOException {
        String response = sendCommand("HELO " + domain);
        ensureResponseStartsWith(response, "250", "HELO");
    }

    // sends an EHLO command (modern SMTP greeting)
    // RFC 5321 section 4.1.4 and section 2.2.1
    public void sendEhlo(String domain) throws IOException {
        String response = sendCommand("EHLO " + domain);
        ensureResponseStartsWith(response, "250", "EHLO");
    }

    public void sendMailFrom(String fromEmail) throws IOException {
        String response = sendCommand("MAIL FROM:<" + fromEmail + ">");
        ensureResponseStartsWith(response, "250", "MAIL FROM");
    }

    public void sendRcptTo(String toEmail) throws IOException {
        String response = sendCommand("RCPT TO:<" + toEmail + ">");
        ensureResponseStartsWith(response, "250", "RCPT TO");
    }

    public void sendData(String messageContent) throws IOException {
        String response = sendCommand("DATA");
        ensureResponseStartsWith(response, "354", "DATA");

        // SMTP message terminator: a single dot on its own line.
        out.print(messageContent + "\r\n.\r\n");
        out.flush();

        response = in.readLine();
        ensureResponseStartsWith(response, "250", "MESSAGE BODY");
    }

    // sends a QUIT command to the server
    public void quit() throws IOException {
        try {
            if (out != null && in != null) {
                String response = sendCommand("QUIT");
                ensureResponseStartsWith(response, "221", "QUIT");
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
//runs everything in the main method
    public static void main(String[] args) {
        try {
            SMTPClient client = new SMTPClient();
            client.connect("localhost", 2525);
            client.sendHelo("localhost");
            client.sendMailFrom("sender@example.com");
            client.sendRcptTo("recipient@example.com");

            String message = ""
                    + "Subject: Test Mail\r\n"
                    + "From: sender@example.com\r\n"
                    + "To: recipient@example.com\r\n"
                    + "\r\n"
                    + "Hello from SMTPClient!";
            client.sendData(message);
            client.quit();

            System.out.println("Message sent successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
