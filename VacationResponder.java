import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class VacationResponder {
    private static final String REQUIRED_SUBJECT = "prac7";
    // an auto responder that checks for emails and replies to them once per user
    private final String pop3Host;
    private final int pop3Port;
    private final String pop3User;
    private final String pop3Pass;
    private final String smtpHost;
    private final int smtpPort;
    private final String localDomain;
    private final String responderAddress;

    // a set of senders that have been replied to so we don't reply to them again
    private final Set<String> repliedSenders = new HashSet<>();
    // a set of POP3 unique IDs so the same message is not processed repeatedly
    private final Set<String> processedMessageUids = new HashSet<>();

    public VacationResponder(
            // the host and port of the POP3 server
            String pop3Host,
            int pop3Port,
            String pop3User,
            String pop3Pass,
            String smtpHost,
            int smtpPort,
            String localDomain,
            String responderAddress
    ) {
        this.pop3Host = pop3Host;
        this.pop3Port = pop3Port;
        this.pop3User = pop3User;
        this.pop3Pass = pop3Pass;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.localDomain = localDomain;
        this.responderAddress = responderAddress;
    }

    // extracts a header from a message
    private String extractHeader(String message, String headerName) {
        String[] lines = message.split("\\r?\\n");
        String prefix = headerName.toLowerCase() + ":";
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                break;
            }
            String lower = line.toLowerCase();
            if (lower.startsWith(prefix)) {
                return line.substring(line.indexOf(':') + 1).trim();
            }
        }
        return "";
    }

    // sends a vacation reply to a recipient
    private void sendVacationReply(String recipient, String originalSubject) throws IOException {
        SMTPClient smtpClient = new SMTPClient();
        try {
            smtpClient.connect(smtpHost, smtpPort);
            // use EHLO first (modern SMTP), then fallback to HELO if needed
            // RFC 5321 section 4.1.4
            try {
                smtpClient.sendEhlo(localDomain);
            } catch (IOException ehloError) {
                smtpClient.sendHelo(localDomain);
            }
            smtpClient.sendMailFrom(responderAddress);
            smtpClient.sendRcptTo(recipient);

            String subject = originalSubject == null || originalSubject.isEmpty()
                    ? "Vacation Auto-Reply"
                    : "Re: " + originalSubject;

            String body = ""
                    + "Subject: " + subject + "\r\n"
                    + "From: " + responderAddress + "\r\n"
                    + "To: " + recipient + "\r\n"
                    + "\r\n"
                    + "Thank you for your email. I am currently on vacation and will respond when I return.";
            smtpClient.sendData(body);
        } finally {
            smtpClient.quit();
        }
    }

    // checks for new emails and replies to them (check for new email periodically)
    private void checkAndReply() throws IOException {
        POP3Client pop3Client = new POP3Client();
        try {
            pop3Client.connect(pop3Host, pop3Port);
            pop3Client.login(pop3User, pop3Pass);

            int messageCount = pop3Client.getStat();
            for (int msgNum = 1; msgNum <= messageCount; msgNum++) {
                String messageUid = pop3Client.getMessageUid(msgNum);
                if (processedMessageUids.contains(messageUid)) {
                    continue;
                }

                String message = pop3Client.retrieveMessage(msgNum);
                String from = extractHeader(message, "From");
                String subject = extractHeader(message, "Subject");

                if (from.isEmpty()) {
                    continue;
                }

                if (!REQUIRED_SUBJECT.equalsIgnoreCase(subject)) {
                    System.out.println("Skipping message " + msgNum + " due to subject: " + subject);
                    continue;
                }

                if (!repliedSenders.contains(from)) {
                    sendVacationReply(from, subject);
                    repliedSenders.add(from);
                    System.out.println("Auto-replied to: " + from);
                } else {
                    System.out.println("Already replied to: " + from);
                }

                processedMessageUids.add(messageUid);
            }
        } finally {
            pop3Client.quit();
        }
    }

    // runs the auto responder and stops after 1 minute
    public void run() {
        while (true) {
            try {
                checkAndReply();
            } catch (IOException e) {
                System.err.println("Mail check failed: " + e.getMessage());
            }

            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static void main(String[] args) {
        VacationResponder responder = new VacationResponder(
                "localhost",
                3110,
                "user@example.com",
                "user@example.com",
                "localhost",
                3025,
                "localhost",
                "user@example.com"
        );
        responder.run();
    }
}
