javamail-mock2
==============

Open source mock classes for mockup JavaMail (useful especially for unittest)

[![Build Status](https://travis-ci.org/salyh/javamail-mock2.svg?branch=v0.4-beta1)](https://travis-ci.org/salyh/javamail-mock2)

<a href="mailto:hendrikdev22@gmail.com">E-Mail hendrikdev22@gmail.com</a><p>
<a href="https://twitter.com/hendrikdev22">Twitter @hendrikdev22</a>

<h3>Usage</h3>
* Include the javamail-mock2-x.x.jar file in your unittest project (or use maven, see below)
* Create a mailbox and add folders/messages or use Transport.sendMail to put mails into your INBOX
* Use the JavaMail API to retrieve mails via POP3 or IMAP or do whatever your application does
* Support imap, imaps, pop3, pop3s
* Supported for POP3: cast to POP3Folder, Folder.getUID(Message msg)
* Supported for IMAP: cast to IMAPFolder, cast to UIDFolder, Subfolders, Folder.getMessagesByUID(...), delete/rename folders, append messages
* Unsupported for the moment: IMAP extensions like IDLE, CONDSTORE, ... and casts to POP3Message/IMAPMessage, store listeners

See unittests on how to use the library.
Maven site docu is here: [http://salyh.github.io/javamail-mock2/](http://salyh.github.io/javamail-mock2)

<h3>Maven</h3>
```xml
	<dependency>
		<groupId>de.saly</groupId>
		<artifactId>javamail-mock2</artifactId>
		<version>0.4-beta3</version>
	</dependency>
```

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

		Session session = Session.getInstance(new Properties());
        final Store store = session.getStore("pop3s");
        store.connect("hendrik@unknown.com", null);
        final Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        Assert.assertEquals(3, inbox.getMessageCount());
        Assert.assertNotNull(inbox.getMessage(1));
        inbox.close(true);
```

For a real usage scenario look here: [https://github.com/salyh/elasticsearch-river-imap](Elasticsearch IMAP River)