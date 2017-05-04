/***********************************************************************************************************************
 *
 * JavaMail Mock2 Provider - open source mock classes for mock up JavaMail
 * =======================================================================
 *
 * Copyright (C) 2014 by Hendrik Saly (http://saly.de)
 * 
 * Based on ideas from Kohsuke Kawaguchi's Mock-javamail (https://java.net/projects/mock-javamail)
 *
 ***********************************************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ***********************************************************************************************************************
 *
 * $Id:$
 *
 **********************************************************************************************************************/
package de.saly.javamail.mock2.test;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import de.saly.javamail.mock2.MailboxFolder;
import de.saly.javamail.mock2.MockMailbox;
import de.saly.javamail.mock2.Providers;
import de.saly.javamail.mock2.test.support.MockTestException;
import org.junit.Assert;
import org.junit.Test;

import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.Properties;

public class IMAPTestCase extends AbstractTestCase {

    private static class IdleMessageCountListener implements MessageCountListener {

        private int addedCount;
        private int removedCount;

        protected int getAddedCount() {
            return addedCount;
        }

        protected int getRemovedCount() {
            return removedCount;
        }

        @Override
        public void messagesAdded(final MessageCountEvent e) {
            addedCount++;

        }

        @Override
        public void messagesRemoved(final MessageCountEvent e) {
            removedCount++;

        }

    }

    private static class IdleThread extends Thread {
        private Exception exception;
        private final Folder folder;
        private int idleCount;

        public IdleThread(final Folder folder) {
            super();
            this.folder = folder;
        }

        protected Exception getException() {
            return exception;
        }

        protected int getIdleCount() {
            return idleCount;
        }

        @Override
        public void run() {

            while (!Thread.interrupted()) {
                try {
                    // System.out.println("enter idle");
                    ((IMAPFolder) folder).idle();
                    idleCount++;
                    // System.out.println("leave idle");
                } catch (final Exception e) {
                    exception = e;
                }
            }

            // System.out.println("leave run()");
        }
    }

    @Override
    protected Properties getProperties() {

        final Properties props = super.getProperties();
        props.setProperty("mail.store.protocol", "mock_imaps");
        return props;
    }

    @Test(expected = MockTestException.class)
    public void testACLUnsupported() throws Exception {

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
        mb.getRoot().getOrAddSubFolder("test").create().add(msg);

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder test = defaultFolder.getFolder("test");

        final IMAPFolder testImap = (IMAPFolder) test;

        try {
            testImap.getACL();
        } catch (final MessagingException e) {
            throw new MockTestException(e);
        }

    }

    @Test
    public void testAddMessages() throws Exception {

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

        final Store store = session.getStore();
        store.connect("hendrik@unknown.com", null);
        final Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);
        Assert.assertEquals(3, inbox.getMessageCount());
        Assert.assertNotNull(inbox.getMessage(1));

        inbox.close(true);

        Assert.assertEquals(3, inbox.getMessageCount());

        inbox.open(Folder.READ_WRITE);
        inbox.getMessage(1).setFlag(Flag.DELETED, true);

        inbox.close(true);

        Assert.assertEquals(2, inbox.getMessageCount());
        Assert.assertTrue(inbox instanceof UIDFolder);
        inbox.open(Folder.READ_WRITE);
        Assert.assertEquals(12L, ((UIDFolder) inbox).getUID(inbox.getMessage(1)));
        inbox.close(true);
    }

    @Test
    // (expected = MockTestException.class)
    public void testAppendFailMessage() throws Exception {
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
        mb.getRoot().getOrAddSubFolder("test").create().add(msg);

        final Store store = session.getStore();
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder inbox = defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_ONLY);

        try {
            inbox.appendMessages(new MimeMessage[] { msg });
        } catch (final IllegalStateException e) {
            // throw new MockTestException(e);
        }

        // Assert.fail("Exception expected before this point");

        Assert.assertEquals(4, inbox.getMessageCount());

        inbox.close(false);

    }

    @Test
    public void testAppendMessage() throws Exception {
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
        mb.getRoot().getOrAddSubFolder("test").create().add(msg);

        final Store store = session.getStore(Providers.getIMAPProvider("makes_no_difference_here", true, true));
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder inbox = defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_WRITE);

        inbox.appendMessages(new MimeMessage[] { msg });

        Assert.assertEquals(4, inbox.getMessageCount());

        inbox.close(true);

    }

    @Test
    public void testDefaultFolder() throws Exception {

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
        mb.getRoot().getOrAddSubFolder("test").create().add(msg);

        final Store store = session.getStore("mock_imaps");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder inbox = defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_WRITE);

        Assert.assertEquals("[INBOX, test]", Arrays.toString(defaultFolder.list()));

        Assert.assertEquals(3, inbox.getMessageCount());
        Assert.assertNotNull(inbox.getMessage(1));

        inbox.close(true);

        Assert.assertEquals(3, inbox.getMessageCount());

        inbox.open(Folder.READ_WRITE);
        inbox.getMessage(1).setFlag(Flag.DELETED, true);

        inbox.close(true);
        inbox.open(Folder.READ_WRITE);
        Assert.assertEquals(2, inbox.getMessageCount());
        Assert.assertTrue(inbox instanceof UIDFolder);
        Assert.assertEquals(12L, ((UIDFolder) inbox).getUID(inbox.getMessage(1)));
        inbox.close(true);
    }

    @Test
    public void testIDLESupported() throws Exception {

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
        mb.getRoot().getOrAddSubFolder("test").create().add(msg);

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final IMAPFolder test = (IMAPFolder) defaultFolder.getFolder("test");

        final IdleMessageCountListener mcl = new IdleMessageCountListener();
        test.addMessageCountListener(mcl);

        test.open(Folder.READ_WRITE);

        final IdleThread it = new IdleThread(test);
        it.start();

        test.addMessages(new Message[] { msg });
        test.addMessages(new Message[] { msg });
        test.addMessages(new Message[] { msg });

        Thread.sleep(500);

        it.interrupt();
        it.join();

        test.close(true);

        Assert.assertNull(it.getException());
        Assert.assertEquals(3, mcl.getAddedCount());
        Assert.assertEquals(0, mcl.getRemovedCount());
        Assert.assertEquals(4, test.getMessageCount());

    }

    @Test
    public void testNotOnlyInbox() throws Exception {

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
        mb.getRoot().getOrAddSubFolder("test").create().add(msg);

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder test = defaultFolder.getFolder("test");

        Assert.assertNotNull(test);

        final Folder inbox = defaultFolder.getFolder("INBOX");

        Assert.assertNotNull(inbox);

    }

    @Test(expected = MockTestException.class)
    public void testQUOTAUnsupported() throws Exception {

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
        mb.getRoot().getOrAddSubFolder("test").create().add(msg);

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder test = defaultFolder.getFolder("test");

        final IMAPStore imapStore = (IMAPStore) store;

        try {
            imapStore.getQuota("");
        } catch (final MessagingException e) {
            throw new MockTestException(e);
        }

    }

    @Test
    public void testRenameWithSubfolder() throws Exception {

        // final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder root = store.getDefaultFolder();
        final Folder level1 = root.getFolder("LEVEL1");
        level1.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
        final Folder level2 = level1.getFolder("LEVEL2");
        level2.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
        level1.appendMessages(new Message[] { msg, msg });
        level2.appendMessages(new Message[] { msg });

        Assert.assertTrue(level1.exists());
        Assert.assertEquals("LEVEL1", level1.getFullName());
        Assert.assertEquals("LEVEL1", level1.getName());
        Assert.assertEquals(2, level1.getMessageCount());

        Assert.assertTrue(level2.exists());
        Assert.assertEquals("LEVEL1/LEVEL2", level2.getFullName());
        Assert.assertEquals("LEVEL2", level2.getName());
        Assert.assertEquals(1, level2.getMessageCount());
        Assert.assertEquals(2, root.list().length);

        // getFolder creates a store
        level1.renameTo(store.getFolder("LEVEL-1R"));

        // TODO really need a create?
        Assert.assertTrue(!store.getFolder("LEVEL1").exists());

        Assert.assertTrue(level1.exists());
        Assert.assertEquals("LEVEL-1R", level1.getFullName());
        Assert.assertEquals("LEVEL-1R", level1.getName());
        Assert.assertEquals(2, level1.getMessageCount());

        Assert.assertTrue(level2.exists());
        Assert.assertEquals("LEVEL-1R/LEVEL2", level2.getFullName());
        Assert.assertEquals("LEVEL2", level2.getName());
        Assert.assertEquals(1, level2.getMessageCount());

        Assert.assertEquals(2, root.list().length);
    }

    @Test
    public void testGetMessageByUnknownUID() throws Exception {
        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);
        final IMAPFolder imapInbox = (IMAPFolder) inbox;
        Assert.assertNull(imapInbox.getMessageByUID(666));
    }

}
