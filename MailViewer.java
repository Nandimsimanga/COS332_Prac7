import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class MailViewer {
    private static void startVacationResponderMode(String pop3Host, int pop3Port, String username, String password) {
        VacationResponder responder = new VacationResponder(
                pop3Host,
                pop3Port,
                username,
                password,
                "localhost",
                3025,
                "localhost",
                username
        );
        System.out.println("Starting Vacation Responder mode. Press Ctrl+C to stop.");
        responder.run();
    }

    private static String extractHeader(String headers, String headerName) {
        String[] lines = headers.split("\\r?\\n");
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
        return "(none)";
    }

    private static List<Integer> parseDeleteSelection(String input, Set<Integer> validMessageIds) {
        List<Integer> selected = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return selected;
        }

        String[] tokens = input.split(",");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            try {
                int msgNum = Integer.parseInt(trimmed);
                if (validMessageIds.contains(msgNum) && !selected.contains(msgNum)) {
                    selected.add(msgNum);
                } else {
                    System.out.println("Skipping invalid/duplicate message number: " + trimmed);
                }
            } catch (NumberFormatException e) {
                System.out.println("Skipping invalid input token: " + trimmed);
            }
        }

        return selected;
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 3110;
        String username = "user@example.com";
        String password = "user@example.com";

        if (args.length >= 4) {
            host = args[0];
            port = Integer.parseInt(args[1]);
            username = args[2];
            password = args[3];
        }

        POP3Client client = new POP3Client();
        try (Scanner scanner = new Scanner(System.in)) {
            client.connect(host, port);
            client.login(username, password);
            System.out.println("Login successful as " + username + "!");

            List<String> messages = client.listMessages();
            System.out.println("Message count: " + messages.size());
            System.out.println("Message list: " + messages);
            if (messages.isEmpty()) {
                System.out.println("No messages found.");
                client.quit();
                return;
            }

            System.out.println("Messages:");

            Set<Integer> validMessageIds = new HashSet<>();
            Map<Integer, String> messageSizes = new HashMap<>();
            for (String messageLine : messages) {
                // LIST response lines are usually: "<msgNum> <size>"
                String[] parts = messageLine.trim().split("\\s+");
                if (parts.length >= 2) {
                    try {
                        int msgId = Integer.parseInt(parts[0]);
                        validMessageIds.add(msgId);
                        messageSizes.put(msgId, parts[1]);
                    } catch (NumberFormatException ignored) {
                        // Keep showing malformed lines, but don't accept them as valid IDs.
                    }
                }
            }

            List<Integer> sortedMessageIds = new ArrayList<>(validMessageIds);
            sortedMessageIds.sort(Integer::compareTo);

            for (int msgId : sortedMessageIds) {
                String headers = client.getMessageHeaders(msgId);
                String from = extractHeader(headers, "From");
                String subject = extractHeader(headers, "Subject");
                String size = messageSizes.getOrDefault(msgId, "?");
                System.out.println("  " + msgId + " | size=" + size + " | from=" + from + " | subject=" + subject);
            }

            System.out.println();
            System.out.println("Choose an action:");
            System.out.println("  1) View full message");
            System.out.println("  2) Delete selected messages");
            System.out.println("  3) Vacation responder mode");
            System.out.println("  4) Quit");
            System.out.print("Enter 1, 2, 3, or 4: ");
            String action = scanner.nextLine().trim();

            if ("1".equals(action)) {
                System.out.print("Enter message number to view: ");
                String msgInput = scanner.nextLine().trim();
                try {
                    int msgNum = Integer.parseInt(msgInput);
                    if (validMessageIds.contains(msgNum)) {
                        String fullMessage = client.retrieveMessage(msgNum);
                        System.out.println("----- BEGIN MESSAGE " + msgNum + " -----");
                        System.out.println(fullMessage);
                        System.out.println("----- END MESSAGE " + msgNum + " -----");
                    } else {
                        System.out.println("Invalid message number: " + msgInput);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input: " + msgInput);
                }
                client.quit();
                System.out.println("Done. QUIT sent; no messages were deleted.");
            } else if ("2".equals(action)) {
                System.out.print("Enter message numbers to delete (comma-separated), or press Enter to skip: ");
                String input = scanner.nextLine();

                List<Integer> toDelete = parseDeleteSelection(input, validMessageIds);
                for (int msgNum : toDelete) {
                    boolean deleted = client.deleteMessage(msgNum);
                    if (deleted) {
                        System.out.println("Marked message " + msgNum + " for deletion.");
                    } else {
                        System.out.println("Failed to delete message " + msgNum + ".");
                    }
                }
                client.quit();
                System.out.println("Done. QUIT sent; deletions applied.");
            } else if ("3".equals(action)) {
                client.quit();
                startVacationResponderMode(host, port, username, password);
            } else if ("4".equals(action)) {
                client.quit();
                System.out.println("Done. QUIT sent; no messages were deleted.");
            } else {
                client.quit();
                System.out.println("Invalid choice. Exiting without deletions.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
