public class TestSMTP {
    public static void main(String[] args) {
        try {
            SMTPClient smtp = new SMTPClient();
            smtp.connect("localhost", 3025);

            String from = "user@example.com";
            String to = "testrecipient@test.com";
            String subject = "Test Subject";
            String body = "This is a test message";

            smtp.sendHelo("localhost");
            smtp.sendMailFrom(from);
            smtp.sendRcptTo(to);
            smtp.sendData(
                "Subject: " + subject + "\r\n"
                + "From: " + from + "\r\n"
                + "To: " + to + "\r\n"
                + "\r\n"
                + body
            );

            smtp.quit();
            System.out.println("Email sent successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}