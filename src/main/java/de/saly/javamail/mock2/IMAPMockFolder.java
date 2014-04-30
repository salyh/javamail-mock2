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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.FolderNotFoundException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.FolderEvent;
import javax.mail.event.MessageChangedEvent;
import javax.mail.internet.MimeMessage;

import com.sun.mail.iap.Response;
import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;

import de.saly.javamail.mock2.MailboxFolder.MailboxEventListener;

public class IMAPMockFolder extends IMAPFolder implements MailboxEventListener{

    private final MailboxFolder mailboxFolder;

    private final UUID objectId = UUID.randomUUID();

    private volatile boolean opened = false;

    private int openMode;

    private final IMAPMockStore store;

    final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    protected IMAPMockFolder(final IMAPMockStore store, final MailboxFolder mailboxFolder) {
        super("DUMMY_NAME_WHICH_MUST_NOT_BE_VISIBLE", MailboxFolder.SEPARATOR, store, false);
        this.mailboxFolder = mailboxFolder;
        this.mailboxFolder.addMailboxEventListener(this);
        this.store = store;
        logger.debug("Folder created " + objectId);
    }

    @Override
    public void appendMessages(final Message[] msgs) throws MessagingException {
        checkExists();
        // checkOpened();
        // checkWriteMode();
        for (final Message m : msgs) {
            mailboxFolder.add((MimeMessage) m);
        }

        logger.debug("Append " + msgs.length + " to " + getFullName());
    }

    @Override
    public AppendUID[] appendUIDMessages(final Message[] msgs) throws MessagingException {
        final AppendUID[] uids = new AppendUID[msgs.length];
        checkExists();
        // checkOpened();
        // checkWriteMode();
        int i = 0;
        for (final Message m : msgs) {
            final MockMessage mockMessage = (MockMessage) mailboxFolder.add((MimeMessage) m);
            uids[i++] = new AppendUID(mailboxFolder.getUidValidity(), mockMessage.getMockid());
        }

        logger.debug("Append " + msgs.length + " to " + getFullName());

        return uids;
    }

    @Override
    public void close(final boolean expunge) throws MessagingException {
        checkOpened();
        checkExists();

        if (expunge) {
            expunge();
        }

        opened = false;
        logger.debug("Folder " + getFullName() + " closed (" + objectId + ")");
        notifyConnectionListeners(ConnectionEvent.CLOSED);
    }

    @Override
    public boolean create(final int type) throws MessagingException {

        if (exists()) {
            return true;
        }

        mailboxFolder.create();
        notifyFolderListeners(FolderEvent.CREATED);
        return mailboxFolder.isExists();

        // return mailboxFolder.reCreate().isExists();

    }

    @Override
    public boolean delete(final boolean recurse) throws MessagingException {
        checkExists();
        checkClosed();
        mailboxFolder.deleteFolder(recurse);
        notifyFolderListeners(FolderEvent.DELETED);
        return true;
    }

    @Override
    public Object doOptionalCommand(final String err, final ProtocolCommand cmd) throws MessagingException {
        throw new MessagingException("Optional command not supported: " + err);
    }

    @Override
    public boolean exists() throws MessagingException {
        return mailboxFolder.isExists();
    }

    @Override
    public Message[] expunge() throws MessagingException {
        checkExists();
        checkOpened();
        checkWriteMode();

        final Message[] removed = wrap(mailboxFolder.expunge());

        if(removed.length > 0)
        notifyMessageRemovedListeners(true, removed);

        return removed;

    }

    @Override
    public synchronized Message[] expunge(final Message[] msgs) throws MessagingException {
        checkExists();
        checkOpened();
        checkWriteMode();
        final Message[] removed = wrap(mailboxFolder.expunge(msgs));
        
        if(removed.length > 0)
        notifyMessageRemovedListeners(true, removed);
        
        return removed;

    }

    @Override
    public synchronized void fetch(final Message[] msgs, final FetchProfile fp) throws MessagingException {
        // do nothing
    }

    @Override
    public Folder getFolder(final String name) throws MessagingException {
        // checkExists();

        logger.debug("getFolder(" + name + ") on " + getFullName());

        if ("inbox".equalsIgnoreCase(name)) {
            return new IMAPMockFolder(store, mailboxFolder.getMailbox().getInbox());
        }

        return new IMAPMockFolder(store, mailboxFolder.getOrAddSubFolder(name));

    }

    @Override
    public synchronized String getFullName() {

        return mailboxFolder.getFullName();
    }

    @Override
    public Message getMessage(final int msgnum) throws MessagingException {
        checkExists();
        checkOpened();
        return new MockMessage(mailboxFolder.getByMsgNum(msgnum), this);
    }

    @Override
    public synchronized Message getMessageByUID(final long uid) throws MessagingException {
        checkExists();
        checkOpened();
        return new MockMessage(mailboxFolder.getById(uid), this);
    }

    @Override
    public int getMessageCount() throws MessagingException {
        checkExists();
        return mailboxFolder.getMessageCount();
    }

    @Override
    public Message[] getMessages(final int low, final int high) throws MessagingException {
        checkExists();
        checkOpened();
        final List<Message> messages = new ArrayList<Message>();
        for (int i = low; i <= high; i++) {
            final Message m = mailboxFolder.getByMsgNum(i);
            messages.add(new MockMessage(m, this));
        }
        return messages.toArray(new Message[messages.size()]);
    }

    @Override
    public synchronized Message[] getMessagesByUID(final long start, final long end) throws MessagingException {
        checkExists();
        checkOpened();
        return wrap(mailboxFolder.getByIds(start, end));

    }

    @Override
    public synchronized Message[] getMessagesByUID(final long[] uids) throws MessagingException {
        checkExists();
        checkOpened();
        return wrap(mailboxFolder.getByIds(uids));
    }

    @Override
    public synchronized Message[] getMessagesByUIDChangedSince(final long start, final long end, final long modseq)
            throws MessagingException {
        throw new MessagingException("CONDSTORE not supported");
    }

    @Override
    public synchronized String getName() {

        return mailboxFolder.getName();
    }

    @Override
    public int getNewMessageCount() throws MessagingException {
        checkExists();
        return mailboxFolder.getByFlags(new Flags(Flag.RECENT), true).length; // TODO
        // or
        // is
        // it
        // SEEN
        // false?
    }

    @Override
    public Folder getParent() throws MessagingException {
        checkExists();
        if (mailboxFolder.getParent() == null) {
            throw new MessagingException("no parent, is already default root");
        }

        return new IMAPMockFolder(store, mailboxFolder.getParent());
    }

    @Override
    public Flags getPermanentFlags() {
        return null;
    }

    @Override
    public char getSeparator() throws MessagingException {
        return MailboxFolder.SEPARATOR;
    }

    @Override
    public int getType() throws MessagingException {
        // checkExists();
        return mailboxFolder.isRoot() ? HOLDS_FOLDERS : HOLDS_MESSAGES | HOLDS_FOLDERS;
    }

    /* (non-Javadoc)
     * @see com.sun.mail.imap.IMAPFolder#getUID(javax.mail.Message)
     */
    @Override
    public synchronized long getUID(final Message message) throws MessagingException {

        return mailboxFolder.getUID(message);
    }

    /* (non-Javadoc)
     * @see com.sun.mail.imap.IMAPFolder#getUIDNext()
     */
    @Override
    public synchronized long getUIDNext() throws MessagingException {

        return mailboxFolder.getUniqueMessageId() + 10; // TODO +10 magic number
    }

    /* (non-Javadoc)
     * @see com.sun.mail.imap.IMAPFolder#getUIDValidity()
     */
    @Override
    public synchronized long getUIDValidity() throws MessagingException {

        return mailboxFolder.getUidValidity();
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        checkExists();
        return getNewMessageCount() > 0;
    }

    @Override
    public boolean isOpen() {
        return opened;
    }

    @Override
    public Folder[] list(final String pattern) throws MessagingException {
        checkExists();

        final List<MailboxFolder> children = mailboxFolder.getChildren();
        final List<Folder> ret = new ArrayList<Folder>();

        for (final MailboxFolder mf : children) {
            if (mf.isExists()) {
                ret.add(new IMAPMockFolder(store, mf));
            }
        }

        logger.debug("Folder (" + getFullName() + ") list return " + ret);

        return ret.toArray(new Folder[ret.size()]);

    }

    @Override
    public void open(final int mode) throws MessagingException {
        checkClosed();
        checkExists();
        opened = true;
        openMode = mode;
        logger.debug("Open folder " + getFullName() + " (" + objectId + ")");
        notifyConnectionListeners(ConnectionEvent.OPENED);
    }

    @Override
    public synchronized boolean renameTo(final Folder f) throws MessagingException {
        checkClosed(); // insure that we are closed.
        checkExists();
        if (f.getStore() != store) {
            throw new MessagingException("Can't rename across Stores");
        }

        mailboxFolder.renameFolder(f.getName());
        notifyFolderRenamedListeners(f);
        return true;
    }

    @Override
	public void handleResponse(Response r) {
		throw new RuntimeException("not implemented/should not happen");
	}

	private Message[] wrap(final Message[] msgs) throws MessagingException {
        final Message[] ret = new Message[msgs.length];
        int i = 0;
        for (final Message message : msgs) {
            ret[i++] = new MockMessage(message, this);
        }
        return ret;
    }

    @Override
    protected void checkClosed() {
        if (opened) {
            throw new IllegalStateException("This operation is not allowed on an open folder:" + getFullName() + " (" + objectId + ")");
        }
    }

    @Override
    protected void checkExists() throws MessagingException {
        if (!exists()) {
            throw new FolderNotFoundException(this, getFullName() + " not found");
        }
    }

    @Override
    protected void checkOpened() throws FolderClosedException {

        if (!opened) {

            throw new IllegalStateException("This operation is not allowed on a closed folder: " + getFullName() + " (" + objectId + ")");

        }
    }

    protected void checkWriteMode() {
        if (openMode != Folder.READ_WRITE) {
            throw new IllegalStateException("Folder " + getFullName() + " is readonly" + " (" + objectId + ")");
        }
    }

    
    @Override
	public void folderCreated(MailboxFolder mf) {
		// ignore
		
	}

	@Override
	public void folderDeleted(MailboxFolder mf) {
		// ignore
		
	}

	@Override
	public void folderRenamed(String from, MailboxFolder to) {
		// ignore
		
	}

	@Override
	public void messageAdded(MailboxFolder mf, MockMessage msg) {
		notifyMessageAddedListeners(new Message[]{msg});
		
	}

	@Override
	public void messageChanged(MailboxFolder mf, MockMessage msg,
			boolean headerChanged, boolean flagsChanged) {
	    
        notifyMessageChangedListeners(MessageChangedEvent.FLAGS_CHANGED, msg);
		
	}

	@Override
	public void messageExpunged(MailboxFolder mf, MockMessage msg,
			boolean removed) {
		// ignore
		
	}

	@Override
	public void uidInvalidated() {
		
		// ignore
	}
}
