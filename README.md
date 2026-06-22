# POP3 and SMTP Clients using raw sockets
This project implements POP3 and SMTP clients using raw sockets with the support of *GreenMail*

Practical 7:
- a vacation auto-responder
- a command-line mail viewer/deleter

The implementation explicitly speaks protocol commands (no high-level email libraries).

## Project Files

- `POP3Client.java`
  - Connect/login to POP3 server
  - `STAT`, `LIST`, `RETR`, `TOP`, `UIDL`, `DELE`, `RSET`, `QUIT`
- `SMTPClient.java`
  - Connect to SMTP server
  - `HELO` and `EHLO`
  - `MAIL FROM`, `RCPT TO`, `DATA`, `QUIT`
  - Handles SMTP reply classes (`4xx` transient vs `5xx` permanent)
- `VacationResponder.java`
  - Polls POP3 mailbox every 60 seconds
  - Replies only once per sender
  - Replies only to messages with subject exactly `prac7`
  - Does not delete mailbox messages
- `MailViewer.java`
  - Lists pending messages with message id, size, sender, and subject
  - Lets user mark messages for deletion
  - Applies deletion on `QUIT`
- `TestSMTP.java`
  - Simple SMTP send test program

## Requirements

- Java (JDK 8 or later)
- Running POP3 and SMTP servers reachable from your machine
  - Example local config used during testing:
    - POP3: `localhost:3110`
    - SMTP: `localhost:2525` (or `3025` in `TestSMTP`)

## Full Instructions (Setup, Run, Demo)

### 1) Start your mail servers first

Start the GreenMail container.

### 2) Verify ports are listening

Run:

```bash
lsof -nP -iTCP -sTCP:LISTEN | rg "2525|3025|3110"
```

### 3) Compile all Java files

From the project folder:

```bash
javac *.java
```

### 4) Test SMTP sending (`TestSMTP`)

```bash
java TestSMTP
```

### 5) Run Mail Viewer

Receiver mailbox:

```bash
java MailViewer localhost 3110 user@example.com user@example.com
```

Sender mailbox:

```bash
java MailViewer localhost 3110 testrecipient@test.com testrecipient@test.com
```

## Implemented Beyond Baseline

- Added modern SMTP greeting support with `EHLO` (and `HELO` fallback in responder flow).
- Added SMTP response-class handling for `4xx` (transient) vs `5xx` (permanent) failures.
- Added proper multiline SMTP response reading (important for GreenMail compatibility).
- Added POP3 `UIDL` support (`UIDL` and `UIDL <message-number>`).
- Added POP3 `RSET` support for transaction reset behavior.
- Added UID-based processed-message tracking in `VacationResponder` to avoid reprocessing.
- Added RFC-referenced comments in protocol methods.
- Enhanced `MailViewer` with:
  - header-based listing (`id | size | from | subject`) using `LIST` + `TOP`
  - full-message view option
  - interactive actions: view, delete, vacation mode, quit.
- Enhanced `TestSMTP` output to show step-by-step protocol actions during demos.

## Notes for Demonstration

- Network-dependent values are currently set in `main` methods for quick local testing.
- If demonstrating in another environment, update host/port/credentials before demo.
- POP3 deletions only happen for messages marked with `DELE` and finalized with `QUIT`.
