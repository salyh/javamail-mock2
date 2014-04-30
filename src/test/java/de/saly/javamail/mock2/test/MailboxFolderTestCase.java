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

import javax.mail.IllegalWriteException;
import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Test;

import de.saly.javamail.mock2.MailboxFolder;
import de.saly.javamail.mock2.MockMailbox;
import de.saly.javamail.mock2.test.support.MockTestException;

public class MailboxFolderTestCase extends AbstractTestCase {

    @Test
    public void testAddFolder() throws Exception {

        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com"); // TODO
                                                                       // spam
                                                                       // protection,
                                                                       // dont
                                                                       // use
                                                                       // real
                                                                       // email
        final MailboxFolder mf = mb.getInbox();
        final MailboxFolder archive2013 = mf.getOrAddSubFolder("Archive").getOrAddSubFolder("2013").create();
        Assert.assertEquals("INBOX/Archive/2013", archive2013.getFullName());
        Assert.assertEquals(1, mf.getChildren().size());
        Assert.assertEquals(1, mf.getChildren().get(0).getChildren().size());
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
        mf.add(msg);

        Assert.assertEquals(1, mf.getMessageCount());
        Assert.assertNotNull(mf.getByMsgNum(1));
        Assert.assertEquals(msg.getSubject(), mf.getByMsgNum(1).getSubject());

        mf.add(msg);
        mf.add(msg);
        Assert.assertEquals(3, mf.getMessageCount());
        Assert.assertNotNull(mf.getByMsgNum(3));
        Assert.assertEquals(msg.getSubject(), mf.getByMsgNum(3).getSubject());

    }

    @Test
    public void testDeleteFolder() throws Exception {

        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        final MailboxFolder mf = mb.getInbox();
        final MailboxFolder archive2013 = mf.getOrAddSubFolder("Archive").getOrAddSubFolder("2013").create();
        archive2013.deleteFolder(true);
        Assert.assertEquals(1, mf.getChildren().size());
        Assert.assertEquals(0, mf.getChildren().get(0).getChildren().size());
    }

    @Test
    public void testInitialize() throws Exception {

        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        Assert.assertNotNull(mb.getInbox());
        Assert.assertNotNull(mb.getRoot());

        Assert.assertEquals("INBOX", mb.getInbox().getFullName());
        Assert.assertEquals("INBOX", mb.getInbox().getName());

        Assert.assertEquals("", mb.getRoot().getFullName());
        Assert.assertEquals("", mb.getRoot().getName());

        Assert.assertEquals(1, mb.getRoot().getChildren().size());
        Assert.assertEquals(0, mb.getInbox().getChildren().size());
        Assert.assertEquals(0, mb.getInbox().getMessageCount());
    }

    @Test(expected = MockTestException.class)
    public void testMockMessagesReadonly() throws Exception {

        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        final MailboxFolder mf = mb.getInbox();

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));
        mf.add(msg);

        try {
            mf.getByMsgNum(1).setHeader("test", "test");
        } catch (final IllegalWriteException e) {
            throw new MockTestException(e);
        }

    }

    @Test
    public void testRenameFolder() throws Exception {

        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        final MailboxFolder mf = mb.getInbox();
        final MailboxFolder archive = mf.getOrAddSubFolder("Archive");
        final MailboxFolder archive2013 = archive.getOrAddSubFolder("2013").create();
        archive.renameFolder("dummy");
        Assert.assertEquals("INBOX/dummy/2013", archive2013.getFullName());
    }

    @Test
    public void testUIDMessages() throws Exception {

        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        final MailboxFolder mf = mb.getInbox();

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));
        mf.add(msg);
        mf.add(msg);
        mf.add(msg);

        Assert.assertTrue(mf.getUID(mf.getByMsgNum(3)) > 0);
        Assert.assertNotNull(mf.getById(mf.getUID(mf.getByMsgNum(3))));

    }

}
