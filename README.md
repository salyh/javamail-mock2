javamail-mock2
==============

Open source mock classes for mockup JavaMail (useful especially for unittest). Supports IMAP IDLE.

[![Build Status](https://travis-ci.org/salyh/javamail-mock2.svg?branch=master)](https://travis-ci.org/salyh/javamail-mock2)

<a href="mailto:hendrikdev22@gmail.com">E-Mail hendrikdev22@gmail.com</a><p>
<a href="https://twitter.com/hendrikdev22">Twitter @hendrikdev22</a>

<h3>Features</h3>
* Support imap, imaps, pop3, pop3s, smtp, smtps
* Support for SMTP: Mock Transport.send()
* Supported for POP3: 
    * cast to POP3Folder
    * Folder.getUID(Message msg)
* Supported for IMAP: 
    * cast to IMAPFolder
    * cast to UIDFolder
    * IDLE
    * ID
    * Subfolders
    * Folder.getMessagesByUID(...)
    * delete/rename folders
    * append messages
* Unsupported for the moment: 
    * All IMAP extensions except IDLE and ID
    * casts to POP3Message/IMAPMessage
    * store listeners

The library come in two flavors/modes
* Normal (or halfmock): Allows also to connect to real IMAP/POP servers. Use this if you have mixed testing setups (mockend an real server). Require a little bit of setup.
* Fullmock: Use this if you have mocked test only. Normally no setup required.

See unittests for how to use the library.
Maven site docu is here: [http://salyh.github.io/javamail-mock2/](http://salyh.github.io/javamail-mock2)

<h3>Usage: Normal (= Halfmock) mode</h3>
* Include the javamail-mock2-halfmock-x.x.jar file in your unittest project (or use maven, see below)
* Make sure every operation that should be mocked uses as protocol either mock_smtp, mock_imap or mock_pop3 (or mock_smtps, mock_imaps or mock_pop3s)
* See unittest how to archive this
* Create a mailbox and add folders/messages or use Transport.sendMail to put mails into your INBOX
* Use the JavaMail API to retrieve mails via POP3 or IMAP or do whatever your application does

<h3>Usage: Fullmock mode</h3>
* Include the javamail-mock2-fullmock-x.x.jar file in your unittest project (or use maven, see below)
* Create a mailbox and add folders/messages or use Transport.sendMail to put mails into your INBOX
* Use the JavaMail API to retrieve mails via POP3 or IMAP or do whatever your application does

<h3>Maven: Normal (= Halfmock)</h3>
```xml
	<dependency>
		<groupId>de.saly</groupId>
		<artifactId>javamail-mock2-halfmock</artifactId>
		<version>0.5-beta4</version>
		<scope>test</scope>
	</dependency>
```

<h3>Maven: Fullmock</h3>
```xml
	<dependency>
		<groupId>de.saly</groupId>
		<artifactId>javamail-mock2-fullmock</artifactId>
		<version>0.5-beta4</version>
		<scope>test</scope>
	</dependency>
```

<h3>Examples</h3>
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
        final Store store = session.getStore("pop3s"); //or mock_pop3s for halfmock
        store.connect("hendrik@unknown.com", null);
        final Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        Assert.assertEquals(3, inbox.getMessageCount());
        Assert.assertNotNull(inbox.getMessage(1));
        inbox.close(true);
```

For a real usage scenario look here: [Elasticsearch IMAP River](https://github.com/salyh/elasticsearch-river-imap)
