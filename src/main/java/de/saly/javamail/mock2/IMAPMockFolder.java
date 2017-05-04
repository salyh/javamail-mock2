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

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.ResyncData;
import com.sun.mail.imap.SortTerm;
import de.saly.javamail.mock2.MailboxFolder.MailboxEventListener;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.FolderNotFoundException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Quota;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.FolderEvent;
import javax.mail.event.MailEvent;
import javax.mail.event.MessageChangedEvent;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class IMAPMockFolder extends IMAPFolder implements MailboxEventListener {

    private static final int ABORTING = 2; // IDLE command aborting
    private static final int IDLE = 1; // IDLE command in effect
    private static final int RUNNING = 0; // not doing IDLE command
    private final Semaphore idleLock = new Semaphore(0, true);

    private int idleState = RUNNING;

    protected final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    private final MailboxFolder mailboxFolder;

    private final UUID objectId = UUID.randomUUID();

    private volatile boolean opened = false;

    private int openMode;

    private final IMAPMockStore store;

    protected IMAPMockFolder(final IMAPMockStore store, final MailboxFolder mailboxFolder) {
        super("DUMMY_NAME_WHICH_MUST_NOT_BE_VISIBLE", MailboxFolder.SEPARATOR, store, false);
        this.mailboxFolder = mailboxFolder;
        this.mailboxFolder.addMailboxEventListener(this);
        this.store = store;
        logger.debug("Folder created " + objectId);
    }

    protected synchronized void abortIdle() {
        if (idleState == IDLE) {
            logger.trace("Abort idle");

            if (logger.isTraceEnabled()) {
                try {
                    throw new RuntimeException();
                } catch (final Exception e) {
                    logger.trace("TRACE stacktrace ", e);
                }

            }

            idleState = ABORTING;
            idleLock.release();

        }
    }

    @Override
    public void appendMessages(final Message[] msgs) throws MessagingException {
        abortIdle();
        checkExists();
        // checkOpened();
        // checkWriteMode();
        for (final Message m : msgs) {
            mailboxFolder.add((MimeMessage) m);
        }

        logger.debug("Append " + msgs.length + " to " + getFullName());
    }

    @Override
    public synchronized AppendUID[] appendUIDMessages(final Message[] msgs) throws MessagingException {
        abortIdle();
        checkExists();
        // checkOpened();
        // checkWriteMode();
        final AppendUID[] uids = new AppendUID[msgs.length];
        int i = 0;
        for (final Message m : msgs) {
            final MockMessage mockMessage = (MockMessage) mailboxFolder.add((MimeMessage) m);
            uids[i++] = new AppendUID(mailboxFolder.getUidValidity(), mockMessage.getMockid());
        }

        logger.debug("Append " + msgs.length + " to " + getFullName());

        return uids;
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
    public synchronized void close(final boolean expunge) throws MessagingException {
        abortIdle();
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
    public synchronized void copyMessages(final Message[] msgs, final Folder folder) throws MessagingException {
        abortIdle();
        checkOpened();
        checkExists();
        if (msgs == null || folder == null || msgs.length == 0) {
            return;
        }

        if (!folder.exists()) {
            throw new FolderNotFoundException(folder.getFullName() + " does not exist", folder);
        }

        folder.appendMessages(msgs);
    }

    @Override
    public synchronized AppendUID[] copyUIDMessages(final Message[] msgs, final Folder folder) throws MessagingException {

        abortIdle();
        checkExists();
        checkOpened();
        if (msgs == null || folder == null || msgs.length == 0) {
            return null;
        }

        final AppendUID[] uids = new AppendUID[msgs.length];

        int i = 0;
        for (final Message m : msgs) {
            final MockMessage mockMessage = (MockMessage) mailboxFolder.add((MimeMessage) m);
            uids[i++] = new AppendUID(mailboxFolder.getUidValidity(), mockMessage.getMockid());
        }

        logger.debug("Copied " + msgs.length + " to " + getFullName());

        return uids;
    }

    @Override
    public synchronized boolean create(final int type) throws MessagingException {
        abortIdle();
        if (exists()) {
            return true;
        }

        mailboxFolder.create();
        notifyFolderListeners(FolderEvent.CREATED);
        return mailboxFolder.isExists();

        // return mailboxFolder.reCreate().isExists();

    }

    @Override
    public synchronized boolean delete(final boolean recurse) throws MessagingException {
        abortIdle();
        checkExists();
        checkClosed();
        mailboxFolder.deleteFolder(recurse);
        notifyFolderListeners(FolderEvent.DELETED);
        return true;
    }

    @Override
    public synchronized Object doCommand(final ProtocolCommand cmd) throws MessagingException {
        throw new MessagingException(
                "no protocol for mock class - you should never see this exception. Please file a bugrfeport and include stacktrace");

    }

    @Override
    public synchronized Object doCommandIgnoreFailure(final ProtocolCommand cmd) throws MessagingException {
        throw new MessagingException(
                "no protocol for mock class - you should never see this exception. Please file a bugrfeport and include stacktrace");

    }

    @Override
    public synchronized Object doOptionalCommand(final String err, final ProtocolCommand cmd) throws MessagingException {
        throw new MessagingException("Optional command not supported: " + err);
    }

    @Override
    protected synchronized Object doProtocolCommand(final ProtocolCommand cmd) throws ProtocolException {

        throw new ProtocolException(
                "no protocol for mock class - you should never see this exception. Please file a bugrfeport and include stacktrace");

    }

    @Override
    public synchronized boolean exists() throws MessagingException {
        abortIdle();
        return mailboxFolder.isExists();
    }

    @Override
    public synchronized Message[] expunge() throws MessagingException {
        abortIdle();
        checkExists();
        checkOpened();
        checkWriteMode();

        final Message[] removed = wrap(mailboxFolder.expunge());

        if (removed.length > 0) {
            notifyMessageRemovedListeners(true, removed);
        }

        return removed;

    }

    @Override
    public synchronized Message[] expunge(final Message[] msgs) throws MessagingException {
        abortIdle();
        checkExists();
        checkOpened();
        checkWriteMode();
        final Message[] removed = wrap(mailboxFolder.expunge(msgs));

        if (removed.length > 0) {
            notifyMessageRemovedListeners(true, removed);
        }

        return removed;

    }

    @Override
    public synchronized void fetch(final Message[] msgs, final FetchProfile fp) throws MessagingException {
        abortIdle();
        // do nothing more
    }

    @Override
    public void folderCreated(final MailboxFolder mf) {
        // ignore

    }

    @Override
    public void folderDeleted(final MailboxFolder mf) {
        // ignore

    }

    @Override
    public void folderRenamed(final String from, final MailboxFolder to) {
        // ignore

    }

    @Override
    public synchronized void forceClose() throws MessagingException {
        close(false);
    }

    @Override
    public synchronized String[] getAttributes() throws MessagingException {
        checkExists();
        // TODO \Marked \HasNoChildren ...
        return new String[0];
    }

    @Override
    public synchronized int getDeletedMessageCount() throws MessagingException {
        abortIdle();
        checkExists();
        if (!opened) {
            return -1;
        }

        return mailboxFolder.getByFlags(new Flags(Flags.Flag.DELETED), false).length;
    }

    @Override
    public synchronized Folder getFolder(final String name) throws MessagingException {
        abortIdle();
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
    public synchronized long getHighestModSeq() throws MessagingException {
        throw new MessagingException("CONDSTORE not supported");
    }

    @Override
    public Message getMessage(final int msgnum) throws MessagingException {
        abortIdle();
        checkExists();
        checkOpened();
        return new MockMessage(mailboxFolder.getByMsgNum(msgnum), this);
    }

    @Override
    public synchronized Message getMessageByUID(final long uid) throws MessagingException {
        abortIdle();
        checkExists();
        checkOpened();
        Message message = mailboxFolder.getById(uid);
        return message != null ? new MockMessage(message, this) : null;
    }

    @Override
    public int getMessageCount() throws MessagingException {
        abortIdle();
        checkExists();
        return mailboxFolder.getMessageCount();
    }

    @Override
    public Message[] getMessages(final int low, final int high) throws MessagingException {
        abortIdle();
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
        abortIdle();
        checkExists();
        checkOpened();
        return wrap(mailboxFolder.getByIds(start, end));

    }

    @Override
    public synchronized Message[] getMessagesByUID(final long[] uids) throws MessagingException {
        abortIdle();
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
        abortIdle();
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
    public Quota[] getQuota() throws MessagingException {
        throw new MessagingException("QUOTA not supported");
    }

    @Override
    public char getSeparator() throws MessagingException {
        abortIdle();
        return MailboxFolder.SEPARATOR;
    }

    @Override
    public synchronized Message[] getSortedMessages(final SortTerm[] term) throws MessagingException {
        throw new MessagingException("SORT not supported");
    }

    @Override
    public synchronized Message[] getSortedMessages(final SortTerm[] term, final SearchTerm sterm) throws MessagingException {
        throw new MessagingException("SORT not supported");
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
        abortIdle();
        return mailboxFolder.getUID(message);
    }

    /* (non-Javadoc)
     * @see com.sun.mail.imap.IMAPFolder#getUIDNext()
     */
    @Override
    public synchronized long getUIDNext() throws MessagingException {
        abortIdle();
        return mailboxFolder.getUniqueMessageId() + 10; // TODO +10 magic number
    }

    /* (non-Javadoc)
     * @see com.sun.mail.imap.IMAPFolder#getUIDValidity()
     */
    @Override
    public synchronized long getUIDValidity() throws MessagingException {
        abortIdle();
        return mailboxFolder.getUidValidity();
    }

    @Override
    public synchronized int getUnreadMessageCount() throws MessagingException {
        abortIdle();
        checkExists();
        return mailboxFolder.getByFlags(new Flags(Flags.Flag.SEEN), false).length;
    }

    @Override
    public void handleResponse(final Response r) {
        throw new RuntimeException("not implemented/should not happen");
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        checkExists();
        return getNewMessageCount() > 0;
    }

    @Override
    public Map<String, String> id(final Map<String, String> clientParams) throws MessagingException {
        return store.id(clientParams);
    }

    @Override
    public void idle(final boolean once) throws MessagingException {

        if (Thread.holdsLock(this)) {
            logger.error("Thread already hold folder lock, thats not supposed to be the case");
        }

        synchronized (this) { // blocks until folder lock available
            checkOpened();
            if (idleState == RUNNING) {

                idleState = IDLE;
                // this thread is now idle

            } else {
                // another thread must be currently idle
                logger.trace("Another thread is idle, return from idle()");
                return;
            }
        }

        // give up folder lock

        logger.trace("Now idle ...");
        try {

            while (idleState != ABORTING && opened && mailboxFolder.isExists()) {
                logger.trace("wait for folder actions");
                idleLock.acquire(); // wait for folder actions, like new mails
                logger.trace("folder action happend");

                if (once) {
                    logger.trace("once =0 true, so return from idle()");
                    break;
                }
            }

            logger.trace("while loop end with idle state " + idleState);

        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            // thread interrupted, set idleState to running and return
        } finally {
            logger.trace("set idle state to: running");
            idleState = RUNNING;
        }

        logger.trace("return from idle()");
    }

    @Override
    public boolean isOpen() {
        return opened;
    }

    @Override
    public synchronized boolean isSubscribed() {
        abortIdle();
        return mailboxFolder.isSubscribed();
    }

    @Override
    public Folder[] list(final String pattern) throws MessagingException {
        abortIdle();
        checkExists();
        // TODO evaluate pattern
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
    public Folder[] listSubscribed(final String pattern) throws MessagingException {
        abortIdle();
        checkExists();
        // TODO evaluate pattern

        final List<MailboxFolder> children = mailboxFolder.getChildren();
        final List<Folder> ret = new ArrayList<Folder>();

        for (final MailboxFolder mf : children) {
            if (mf.isExists() && mf.isSubscribed()) {
                ret.add(new IMAPMockFolder(store, mf));
            }
        }

        logger.debug("Folder (" + getFullName() + ") list subscribed return " + ret);

        return ret.toArray(new Folder[ret.size()]);
    }

    @Override
    public void messageAdded(final MailboxFolder mf, final MockMessage msg) {
        notifyMessageAddedListeners(new Message[] { msg });
        idleLock.release();

    }

    @Override
    public void messageChanged(final MailboxFolder mf, final MockMessage msg, final boolean headerChanged, final boolean flagsChanged) {
        notifyMessageChangedListeners(MessageChangedEvent.FLAGS_CHANGED, msg);
        idleLock.release();
    }

    @Override
    public void messageExpunged(final MailboxFolder mf, final MockMessage msg, final boolean removed) {
        idleLock.release();

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
    public synchronized List<MailEvent> open(final int mode, final ResyncData rd) throws MessagingException {

        if (rd == null) {
            open(mode);
            return null;
        }

        throw new MessagingException("CONDSTORE and QRESYNC not supported");

    }

    @Override
    public synchronized boolean renameTo(final Folder f) throws MessagingException {
        abortIdle();
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
    public Message[] search(final SearchTerm term) throws MessagingException {
        abortIdle();
        checkOpened();
        return mailboxFolder.search(term, null);
    }

    @Override
    public Message[] search(final SearchTerm term, final Message[] msgs) throws MessagingException {
        abortIdle();
        checkOpened();
        return mailboxFolder.search(term, msgs);
    }

    @Override
    public synchronized void setFlags(final Message[] msgs, final Flags flag, final boolean value) throws MessagingException {
        abortIdle();
        checkOpened();

        for (final Message message : msgs) {

            final Message m = mailboxFolder.getById(((MockMessage) message).getMockid());

            if (m != null) {
                m.setFlags(flag, value);
            }
        }

    }

    @Override
    public void setQuota(final Quota quota) throws MessagingException {
        throw new MessagingException("QUOTA not supported");
    }

    @Override
    public synchronized void setSubscribed(final boolean subscribe) throws MessagingException {
        abortIdle();
        mailboxFolder.setSubscribed(subscribe);
    }

    @Override
    public void uidInvalidated() {

        // ignore
    }

    private Message[] wrap(final Message[] msgs) throws MessagingException {
        final Message[] ret = new Message[msgs.length];
        int i = 0;
        for (final Message message : msgs) {
            ret[i++] = new MockMessage(message, this);
        }
        return ret;
    }

}
