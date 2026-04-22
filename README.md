# COS332 Practical 7

This project implements POP3 and SMTP clients using raw sockets for COS332 Practical 7, plus two application programs:

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

## Compile

From the project folder:

```bash
javac *.java
```

## Run

### 1) POP3 client quick check

```bash
java POP3Client
```

This runs the built-in sample `main` that connects, logs in, prints message count/list, and quits.

### 2) SMTP send test

```bash
java TestSMTP
```

Sends one test mail using `SMTPClient`.

### 3) Vacation responder

```bash
java VacationResponder
```

Behavior:

- checks mailbox every 60 seconds
- parses `From` and `Subject`
- only replies when subject is exactly `prac7`
- replies once per sender per runtime
- keeps original mail on server (no delete calls)

Stop with `Ctrl+C`.

### 4) Mail viewer/deleter

Use defaults:

```bash
java MailViewer
```

Or pass connection credentials:

```bash
java MailViewer <host> <port> <username> <password>
```

Example:

```bash
java MailViewer localhost 3110 user@example.com user@example.com
```

It will:

- list each message as `id | size | from | subject`
- prompt for comma-separated ids to delete (for example `1,3,4`)
- send `DELE` for selected ids
- send `QUIT` to commit deletions

## Notes for Demonstration

- Network-dependent values are currently set in `main` methods for quick local testing.
- If demonstrating in another environment, update host/port/credentials before demo.
- POP3 deletions only happen for messages marked with `DELE` and finalized with `QUIT`.