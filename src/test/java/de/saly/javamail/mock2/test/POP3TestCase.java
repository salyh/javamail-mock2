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

import java.util.Arrays;
import java.util.Properties;

import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Test;

import com.sun.mail.pop3.POP3Folder;

import de.saly.javamail.mock2.MailboxFolder;
import de.saly.javamail.mock2.MockMailbox;
import de.saly.javamail.mock2.Providers;
import de.saly.javamail.mock2.test.support.MockTestException;

public class POP3TestCase extends AbstractTestCase {

    @Override
    protected Properties getProperties() {

        final Properties props = super.getProperties();
        props.setProperty("mail.store.protocol", "mock_pop3s");
        return props;
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
        inbox.open(Folder.READ_ONLY);
        Assert.assertEquals(3, inbox.getMessageCount());
        Assert.assertNotNull(inbox.getMessage(1));

        inbox.close(true);

        Assert.assertEquals(3, inbox.getMessageCount());

        inbox.open(Folder.READ_ONLY);
        inbox.getMessage(1).setFlag(Flag.DELETED, true);

        inbox.close(true);
        inbox.open(Folder.READ_ONLY);
        Assert.assertEquals(2, inbox.getMessageCount());
        Assert.assertTrue(inbox instanceof POP3Folder);
        Assert.assertEquals("12", ((POP3Folder) inbox).getUID(inbox.getMessage(1)));
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

        final Store store = session.getStore("mock_pop3");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder inbox = defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_WRITE);

        Assert.assertEquals("[INBOX]", Arrays.toString(defaultFolder.list()));

        Assert.assertEquals(3, inbox.getMessageCount());
        Assert.assertNotNull(inbox.getMessage(1));

        inbox.close(true);

        Assert.assertEquals(3, inbox.getMessageCount());

        inbox.open(Folder.READ_ONLY);
        inbox.getMessage(1).setFlag(Flag.DELETED, true);

        inbox.close(true);
        inbox.open(Folder.READ_WRITE);
        Assert.assertEquals(2, inbox.getMessageCount());
        Assert.assertTrue(inbox instanceof POP3Folder);
        Assert.assertEquals("12", ((POP3Folder) inbox).getUID(inbox.getMessage(1)));
        inbox.close(true);
    }

    @Test(expected = MockTestException.class)
    public void testOnlyInbox() throws Exception {

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

        final Store store = session.getStore(Providers.getPOP3Provider("makes_no_differernce", false, true));
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();

        try {
            defaultFolder.getFolder("test");
        } catch (final MessagingException e) {
            throw new MockTestException(e);
        }

    }

}
