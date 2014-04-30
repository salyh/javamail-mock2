javamail-mock2
==============

Open source mock classes for mockup JavaMail (useful especially for unittest)

<h3>Usage</h3>
* Include the javamail-mock2-0.4-beta1.jar file in your unittest project
* Create a mailbox and add folders/messages or use Transport.sendMail to put mails into your INBOX
* Use the JavaMail API to retrieve mails via POP3 or IMAP or do whatever your application does
* Supported POP3: cast to POP3Folder
* Supported IMAP: cast to IMAPFolder, cast to UIDFolder, Subfolders, delete/rename folders 
* Unsupported: IMAP extensions like IDLE, CONDSTORE, ... and casts to POP3Message/IMAPMessage

See unittests on how to use the library

<h3>Example</h3>
```java

		final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        final MailboxFolder mf = mb.getInbox();

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));
        mf.add(msg); // 11
        mf.add(msg); // 12
        mf.add(msg); // 13

        final Store store = session.getStore("pop3s");
        store.connect("hendrik@unknown.com", null);
        final Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        Assert.assertEquals(3, inbox.getMessageCount());
        Assert.assertNotNull(inbox.getMessage(1));
        inbox.close(true);
```