public class TestSMTP {
    public static void main(String[] args) {
        try {
            System.out.println("Starting SMTP demo...");
            SMTPClient smtp = new SMTPClient();
            System.out.println("Connecting to SMTP server on localhost:3025");
            smtp.connect("localhost", 3025);

            String from = "testrecipient@test.com";
            String to = "user@example.com";
            String[] subjects = {
                "prac7",
                "General Update",
                "Meeting Notes"
            };

            System.out.println("Connection successful!");
            System.out.println("Sending HELO command");
            smtp.sendHelo("localhost");
            for (int i = 0; i < subjects.length; i++) {
                String subject = subjects[i];
                String body = "Demo email " + (i + 1) + " with subject: " + subject;
System.out.println("\nSending message " + (i + 1) + " with subject: " + subject);
                System.out.println("Sending MAIL FROM: " + from);
                smtp.sendMailFrom(from);
                System.out.println("Sending RCPT TO: " + to);
                smtp.sendRcptTo(to);
                System.out.println("Sending DATA for message " + (i + 1));
                smtp.sendData(
                    "Subject: " + subject + "\r\n"
                    + "From: " + from + "\r\n"
                    + "To: " + to + "\r\n"
                    + "\r\n"
                    + body
                );
                System.out.println("Message " + (i + 1) + " sent.\n");
            }


            smtp.quit();
            System.out.println("3 demo emails sent successfully! \nExiting...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}