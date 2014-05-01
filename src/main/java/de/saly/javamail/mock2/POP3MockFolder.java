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
package de.saly.javamail.mock2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.mail.FetchProfile;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.MethodNotSupportedException;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.MessageChangedEvent;

import com.sun.mail.pop3.POP3MockFolder0;

import de.saly.javamail.mock2.MailboxFolder.MailboxEventListener;

public class POP3MockFolder extends POP3MockFolder0 implements MailboxEventListener {
    private final MailboxFolder mailboxFolder;
    private final UUID objectId = UUID.randomUUID();
    private volatile boolean opened;
    final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    protected POP3MockFolder(final POP3MockStore store, final MailboxFolder mailboxFolder) {
        super(store);
        this.mailboxFolder = mailboxFolder;
        this.mailboxFolder.addMailboxEventListener(this);
        logger.debug("Folder created " + objectId);

    }

    /*
     * TODO superclass ok?
    public Folder getParent(){
        return null;
    }
      */

    @Override
    public synchronized void close(final boolean expunge) throws MessagingException {
        checkOpened();

        if (expunge) {
            mailboxFolder.expunge();

        }

        opened = false;

        logger.debug("Folder closed " + objectId);
        notifyConnectionListeners(ConnectionEvent.CLOSED);
    }

    @Override
    public void fetch(final Message[] msgs, final FetchProfile fp) throws MessagingException {
        // just do nothing
    }

    @Override
    public void folderCreated(final MailboxFolder mf) {
        // not valid for pop3

    }

    @Override
    public void folderDeleted(final MailboxFolder mf) {
        // not valid for pop3

    }

    @Override
    public void folderRenamed(final String from, final MailboxFolder to) {
        // not valid for pop3

    }

    @Override
    public synchronized Message getMessage(final int msgnum) throws MessagingException {
        checkOpened();
        return new MockMessage(mailboxFolder.getByMsgNum(msgnum), this);
    }

    @Override
    public synchronized int getMessageCount() throws MessagingException {
        return mailboxFolder.getMessageCount();
    }

    @Override
    public synchronized Message[] getMessages() throws MessagingException {
        checkOpened();
        final List<Message> messages = new ArrayList<Message>();
        for (int i = 1; i <= mailboxFolder.getMessageCount(); i++) {
            final Message m = mailboxFolder.getByMsgNum(i);
            messages.add(new MockMessage(m, this));
        }
        return messages.toArray(new Message[messages.size()]);
    }

    @Override
    public synchronized Message[] getMessages(final int low, final int high) throws MessagingException {

        checkOpened();
        final List<Message> messages = new ArrayList<Message>();
        for (int i = low; i <= high; i++) {
            final Message m = mailboxFolder.getByMsgNum(i);
            messages.add(new MockMessage(m, this));
        }
        return messages.toArray(new Message[messages.size()]);
    }

    @Override
    public synchronized Message[] getMessages(final int[] msgnums) throws MessagingException {
        checkOpened();

        final List<Integer> idlist = new ArrayList<Integer>();
        for (final int value : msgnums) {
            idlist.add(value);
        }

        final List<Message> messages = new ArrayList<Message>();

        for (int i = 1; i <= mailboxFolder.getMessageCount(); i++) {

            if (!idlist.contains(new Integer(i))) {
                continue;
            }

            final Message m = mailboxFolder.getByMsgNum(i);
            messages.add(new MockMessage(m, this));
        }
        return messages.toArray(new Message[messages.size()]);
    }

    @Override
    public synchronized int getSize() throws MessagingException {
        checkOpened();
        return mailboxFolder.getSizeInBytes();
    }

    @Override
    public synchronized int[] getSizes() throws MessagingException {
        checkOpened();
        final int count = getMessageCount();
        final int[] sizes = new int[count];

        for (int i = 1; i <= count; i++) {
            sizes[i - 1] = getMessage(i).getSize();
        }

        return sizes;

    }

    @Override
    public synchronized String getUID(final Message msg) throws MessagingException {
        checkOpened();
        return String.valueOf(((MockMessage) msg).getMockid());
    }

    @Override
    public boolean isOpen() {
        return opened;
    }

    @Override
    public InputStream listCommand() throws MessagingException, IOException {
        throw new MethodNotSupportedException();
    }

    @Override
    public void messageAdded(final MailboxFolder mf, final MockMessage msg) {
        // ignore
        // TODO JavaMail impl seems to not fire a event here for pop3, so we
        // ignore it

    }

    @Override
    public void messageChanged(final MailboxFolder mf, final MockMessage msg, final boolean headerChanged, final boolean flagsChanged) {
        notifyMessageChangedListeners(MessageChangedEvent.FLAGS_CHANGED, msg);

    }

    @Override
    public void messageExpunged(final MailboxFolder mf, final MockMessage msg, final boolean removed) {
        // not valid for pop3

    }

    @Override
    public synchronized void open(final int mode) throws MessagingException {
        checkClosed();
        opened = true;
        logger.debug("Open " + objectId);
        notifyConnectionListeners(ConnectionEvent.OPENED);
    }

    @Override
    public void uidInvalidated() {
        // not valid for pop3

    }

    protected synchronized void checkClosed() {
        if (opened) {
            throw new IllegalStateException("This operation is not allowed on an open folder " + objectId);
        }
    }

    protected synchronized void checkOpened() throws FolderClosedException {

        if (!opened) {

            throw new IllegalStateException("This operation is not allowed on a closed folder " + objectId);

        }
    }

}
