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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.MimeMessage;

public class MailboxFolder implements MockMessage.FlagChangeListener {

    public static final char SEPARATOR = '/';

    private final List<MailboxFolder> children = new ArrayList<MailboxFolder>();
    private boolean exists = true;
    private final MockMailbox mailbox;
    private volatile List<MailboxEventListener> mailboxEventListeners = Collections.synchronizedList(new ArrayList<MailboxEventListener>());
   
    private final Map<Long, MockMessage> messages = new HashMap<Long, MockMessage>();

    private String name;

    private MailboxFolder parent;
    private boolean simulateError = false;
    private long uidValidity = 50;
    private long uniqueMessageId = 10;
    final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    protected MailboxFolder(final String name, final MockMailbox mb, final boolean exists) {
        super();

        if (name == null) {
            this.name = "";
        } else {
            this.name = name;
        }

        this.mailbox = mb;
        this.exists = exists;

        logger.debug("Created " + name + " (exists: " + exists + ")");
    }
    
	public synchronized void addMailboxEventListener(MailboxEventListener l) {
		if (l != null)
			mailboxEventListeners.add(l);
	}

	public synchronized void removeMailboxEventListener(MailboxEventListener l) {
		if (l != null)
			mailboxEventListeners.remove(l);
	}
        
    public synchronized Message add(final MimeMessage e) throws MessagingException {
        checkExists();

        uniqueMessageId++;

        final MockMessage mockMessage = new MockMessage(e, uniqueMessageId, this, this);

        mockMessage.setSpecialHeader("Message-ID", String.valueOf(uniqueMessageId));
        mockMessage.setSpecialHeader("X-Mock-Folder", getFullName());
        mockMessage.setFlags(new Flags(Flag.RECENT), true);
        // unread.add(e);

        messages.put(uniqueMessageId, mockMessage);

        
        	for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
        		mailboxEventListener.messageAdded(this, mockMessage);
    		}
            
        

        logger.debug("Message ID " + uniqueMessageId + " to " + getFullName() + " added for user " + mailbox.getAddress());

        return mockMessage;
    }

    public synchronized MailboxFolder create() {
        if (isExists()) {
            throw new IllegalStateException("already exists");
        }
        checkFolderName(this.name);

        exists = true;

        // TODO set parent and/or children to exists?

        if (parent != null && !parent.isExists()) {
            parent.create();
        }

        /*children.clear();

        if (parent != null) {
            parent.children.add(this);
        }

        if (mailboxEventListener != null) {
            mailboxEventListener.folderCreated(this);
        }*/

        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
        	 mailboxEventListener.folderCreated(this);
		}
        
        
        logger.debug("Folder " + this.getFullName() + " created");
        return this;

    }

    public synchronized void deleteFolder(final boolean recurse) {
        checkExists();
        checkFolderName(this.name);

        if (isRoot()) {
            throw new IllegalArgumentException("root cannot be deleted");
        }

        messages.clear();
        // unread.clear();

        if (recurse) {
            for (final MailboxFolder mf : getChildren()) {
                mf.deleteFolder(recurse);
            }
        }

        parent.children.remove(this);
        this.exists = false;
    
        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
        	  mailboxEventListener.folderDeleted(this);
		}
        logger.debug("Folder " + this.getFullName() + " deleted");

    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MailboxFolder other = (MailboxFolder) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (parent == null) {
            if (other.parent != null) {
                return false;
            }
        } else if (!parent.equals(other.parent)) {
            return false;
        }
        return true;
    }

    public synchronized Message[] expunge() throws MessagingException {
        checkExists();
        final List<Message> expunged = new ArrayList<Message>();
        for (final Message msg : getByFlags(new Flags(Flag.DELETED), true)) {

            expunged.add(messages.remove(((MockMessage) msg).getMockid()));
            ((MockMessage) msg).setExpunged(true);
            
for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
	mailboxEventListener.messageExpunged(this, (MockMessage) msg, true);
    		}
        }

        logger.debug(expunged.size() + " messages expunged (deleted) from" + getFullName());
        return expunged.toArray(new Message[expunged.size()]);

    }

    public synchronized Message[] expunge(final Message[] msgs) throws MessagingException {
        checkExists();
        final List<Long> toExpunge = new ArrayList<Long>();

        for (final Message msg : msgs) {
            toExpunge.add(((MockMessage) msg).getMockid());
        }

        final List<Message> expunged = new ArrayList<Message>();
        for (final Message msg : getByFlags(new Flags(Flag.DELETED), true)) {

            if (!toExpunge.contains(new Long(((MockMessage) msg).getMockid()))) {
                continue;
            }

            expunged.add(messages.remove(((MockMessage) msg).getMockid()));
            ((MockMessage) msg).setExpunged(true);
           
for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
	 mailboxEventListener.messageExpunged(this, (MockMessage) msg, true);
    		}
        }

        logger.debug(expunged.size() + " messages expunged (deleted) from " + getFullName());
        return expunged.toArray(new Message[expunged.size()]);

    }

    public synchronized Message[] getByFlags(final Flags flags, final boolean mustSet /*final Folder folder*/) throws MessagingException {
        checkExists();
        final List<MockMessage> sms = new ArrayList<MockMessage>();
        int num = 0;

        for (final MockMessage mockMessage : new TreeSet<MockMessage>(messages.values())) {

            if (mustSet && mockMessage.getFlags().contains(flags) || !mustSet && !mockMessage.getFlags().contains(flags)) {
                mockMessage.setMessageNumber(++num);
                // mockMessage.setFolder(folder);
                sms.add(mockMessage);
            }

        }
        logger.debug("getByFlags() for " + getFullName() + " returns " + sms.size());
        return sms.toArray(new Message[sms.size()]);
    }

    public synchronized Message getById(final long id /*final Folder folder*/) {
        checkExists();
        final Message m = messages.get(id);

        if (m == null) {

            logger.debug("No message with id " + id + ", return null");
            return null;

        }

        // ((MockMessage) m).setFolder(folder);
        logger.debug("getById(" + id + ") for " + getFullName() + " returns successful");

        return m;
    }

    public synchronized Message[] getByIds(final long start, final long end/* final Folder folder*/) {
        checkExists();
        final List<MockMessage> sms = new ArrayList<MockMessage>();
        int num = 0;

        MockMessage lastMsg = null;

        for (final MockMessage mockMessage : new TreeSet<MockMessage>(messages.values())) {

            lastMsg = mockMessage;

            if (end == UIDFolder.LASTUID) {
                if (getMessageCount() != 1 && mockMessage.getMockid() < start) { // TODO
                                                                                 // check?
                    continue;
                }
            } else {
                if (mockMessage.getMockid() < start || mockMessage.getMockid() > end) {
                    continue;
                }
            }

            mockMessage.setMessageNumber(++num);
            // mockMessage.setFolder(folder);
            sms.add(mockMessage);
        }

        if (end == UIDFolder.LASTUID && sms.size() == 0) {
            lastMsg.setMessageNumber(++num);
            // lastMsg.setFolder(folder);
            sms.add(lastMsg);
        }

        logger.debug("getByIds(" + start + "," + end + " for " + getFullName() + " returns " + sms.size());
        return sms.toArray(new Message[sms.size()]);
    }

    // private List<Message> unread = new ArrayList<Message>();

    public synchronized Message[] getByIds(final long[] id /*final Folder folder*/) {
        checkExists();
        final List<Long> idlist = new ArrayList<Long>();
        for (final long value : id) {
            idlist.add(value);
        }
        final List<MockMessage> sms = new ArrayList<MockMessage>();
        int num = 0;

        for (final MockMessage mockMessage : new TreeSet<MockMessage>(messages.values())) {

            if (!idlist.contains(mockMessage.getMockid())) {
                continue;
            }

            mockMessage.setMessageNumber(++num);
            // mockMessage.setFolder(folder);
            sms.add(mockMessage);
        }

        logger.debug("getByIds(" + Arrays.toString(id) + ") for " + getFullName() + " returns " + sms.size());
        return sms.toArray(new Message[sms.size()]);
    }

    public synchronized Message getByMsgNum(final int msgnum/*, final Folder folder*/) {
        checkExists();
        final List<MockMessage> sms = new ArrayList<MockMessage>();

        int num = 0;

        for (final MockMessage mockMessage : new TreeSet<MockMessage>(messages.values())) {

            mockMessage.setMessageNumber(++num);
            // mockMessage.setFolder(folder);
            sms.add(mockMessage);
        }

        logger.debug("getByMsgNum(" + msgnum + "), size is " + sms.size());

        if (msgnum - 1 < 0 || msgnum > sms.size()) {
            throw new ArrayIndexOutOfBoundsException("message number (" + msgnum + ") out of bounds (" + sms.size() + ") for "
                    + getFullName());
        }

        final Message m = sms.get(msgnum - 1);
        return m;
    }

    /**
     * 
     * @return Unmodifieable new list copy
     */
    public synchronized List<MailboxFolder> getChildren() {
        checkExists();
        return Collections.unmodifiableList(new ArrayList<MailboxFolder>(children));
    }

    public synchronized String getFullName() {
        // checkExists();
        if (isRoot()) {
            return "";
        }

        return parent.isRoot() ? name : parent.getFullName() + SEPARATOR + name;

    }

    /**
     * @return the mailbox
     */
    public MockMailbox getMailbox() {
        return mailbox;
    }

    public synchronized int getMessageCount() {
        checkExists();
        logger.debug("getMessageCount() for " + getFullName() + " returns " + messages.size());
        return messages.size();
    }

    public synchronized Message[] getMessages(/*final Folder folder*/) {
        checkExists();
        final List<MockMessage> sms = new ArrayList<MockMessage>();
        int num = 0;

        for (final MockMessage mockMessage : new TreeSet<MockMessage>(messages.values())) {

            mockMessage.setMessageNumber(++num);
            // mockMessage.setFolder(folder);
            sms.add(mockMessage);
        }
        logger.debug("getMessages() for " + getFullName() + " returns " + sms.size());
        return sms.toArray(new Message[sms.size()]);
    }

    public String getName() {
        return name;
    }

    public MailboxFolder getOrAddSubFolder(final String name) throws MessagingException {
        // checkExists();

        if (name == null || "".equals(name.trim())) {
            throw new MessagingException("cannot get or add root folder");
        }

        logger.debug("getOrAddSubFolder(" + name + ") on " + getFullName());

        final String[] path = name.split(String.valueOf(SEPARATOR));

        MailboxFolder last = this;
        for (int i = 0; i < path.length; i++) {

            final String element = path[i];

            if ("inbox".equalsIgnoreCase(element)) {
                last = mailbox.getInbox();
            } else {
                checkFolderName(element);
                final MailboxFolder mbt = new MailboxFolder(element, mailbox, false);
                mbt.parent = last;

                int index = -1;
                if ((index = last.children.indexOf(mbt)) != -1) {

                    final MailboxFolder tmp = last.children.get(index);
                    if (tmp.isExists()) {
                        last = tmp;
                        continue;
                    }
                }

                last.children.add(mbt);

                logger.debug("Subfolder " + mbt.getFullName() + " added");
                last = mbt;
            }

        }

        return last;

    }

    public synchronized MailboxFolder getParent() {
        checkExists();
        return parent;
    }

    public synchronized int getSizeInBytes() throws MessagingException {
        checkExists();
        int size = 0;

        for (final MockMessage mockMessage : new TreeSet<MockMessage>(messages.values())) {

            if (mockMessage.getSize() > 0) {
                size += mockMessage.getSize();
            }

        }

        return size;
    }

    public synchronized long getUID(final Message msg) {
        checkExists();
        return ((MockMessage) msg).getMockid();
    }

    /**
     * @return the uidValidity
     */
    public synchronized long getUidValidity() {
        checkExists();
        return uidValidity;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        result = prime * result + (parent == null ? 0 : parent.hashCode());
        return result;
    }

    public synchronized boolean hasMessages() {
        checkExists();
        return messages.isEmpty();
    }

    public synchronized void invalidateUid() {
        checkExists();
        uidValidity += 10;
       
        
        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
        	mailboxEventListener.uidInvalidated();
		}
        logger.debug("UidValidity invalidated, new UidValidity is " + uidValidity);
    }

    /**
     * @return the exists
     */
    public boolean isExists() {
        return exists;
    }

    public boolean isInbox() {
        return name != null && name.equalsIgnoreCase("inbox");
    }

    public boolean isRoot() {
        return name == null || name.equals("") || parent == null;
    }

    /**
     * @return the simulateError
     */
    public boolean isSimulateError() {
        return simulateError;
    }

    public synchronized void markMessageAsDeleted(final Message e) throws MessagingException {
        checkExists();
        ((MockMessage) e).setFlag(Flag.DELETED, true);
        // if(mailboxEventListener!=null)
        // mailboxEventListener.messageRemoved(this, ((MockMessage)e), false);
        logger.debug("Mark message " + ((MockMessage) e).getMockid() + " as deleted (Flag DELETED set)");
    }

    public synchronized void markMessageAsSeen(final Message e) throws MessagingException {
        checkExists();
        ((MockMessage) e).setFlag(Flag.SEEN, true);
        // if(mailboxEventListener!=null)
        // mailboxEventListener.messageRemoved(this, ((MockMessage)e), false);
        logger.debug("Mark message " + ((MockMessage) e).getMockid() + " as seen (Flag SEEN set)");
    }

    @Override
    public void onFlagChange(final MockMessage msg, final Flags flags, final boolean set) {

        
        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
        	mailboxEventListener.messageChanged(this, msg, false, true);
		}
    

        logger.debug("Flags of message " + msg.getMockid() + " change");

		if(messages.size() > 0){
        try {
            if (set && messages.get(msg.getMockid()).getFlags().contains(flags)) {
                return;

            }

            if (set && !messages.get(msg.getMockid()).getFlags().contains(flags)) {
                messages.get(msg.getMockid()).setFlags(flags, set);

            }

            if (!set && messages.get(msg.getMockid()).getFlags().contains(flags)) {
                messages.get(msg.getMockid()).setFlags(flags, set);

            }

            if (!set && !messages.get(msg.getMockid()).getFlags().contains(flags)) {
                return;

            }
        } catch (final Exception e) {            
            logger.error("Error while changing flags "+e.toString(),e);
        }}

    }

    public synchronized void renameFolder(final String newName) {
        checkExists();
        checkFolderName(this.name);
        checkFolderName(newName);
        final String tmpOldName = name;

        name = newName;

       
        
        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
        	mailboxEventListener.folderRenamed(tmpOldName, this);
		}

        // TODO purge old folders, exists =false

        // TODO notify children?
        /*for (MailboxFolder mf: children) {
        	renameFolder(mf.name); //do not really change name of children, just notify because parent changes
        }*/

        logger.debug("Folder " + tmpOldName + " renamed to " + newName + newName + " - New Fullname is " + this.getFullName());

    }

    /**
     * @param simulateError
     *            the simulateError to set
     */
    public void setSimulateError(final boolean simulateError) {
        this.simulateError = simulateError;
    }

    protected MailboxFolder addSpecialSubFolder(final String name) {
        final MailboxFolder mbt = new MailboxFolder(name, mailbox, true);
        mbt.parent = this;
        children.add(mbt);
        return mbt;
    }

    protected void checkExists() {
        if (!exists) {
            throw new IllegalStateException("folder does not exist");
        }
    }

    protected void checkFolderName(final String name) {
        checkFolderName(name, true);
    }

    protected void checkFolderName(final String name, final boolean checkSeparator) {
        // TODO regex for valid folder names?

        if (name == null || name.trim().equals("") || name.equalsIgnoreCase("inbox") || checkSeparator
                && name.contains(String.valueOf(SEPARATOR))) {
            throw new IllegalArgumentException("name '" + name + "' is not valid");
        }
    }

    /**
     * @return the uniqueMessageId
     */
    protected long getUniqueMessageId() {
        return uniqueMessageId;
    }

    public static interface MailboxEventListener {

        void folderCreated(MailboxFolder mf);

        void folderDeleted(MailboxFolder mf);

        void folderRenamed(String from, MailboxFolder to);

        void messageAdded(MailboxFolder mf, MockMessage msg);

        void messageChanged(MailboxFolder mf, MockMessage msg, boolean headerChanged, boolean flagsChanged); // TODO
                                                                                                             // header
                                                                                                             // change
                                                                                                             // can
                                                                                                             // not
                                                                                                             // happen
                                                                                                             // because
                                                                                                             // MockMessage
                                                                                                             // is
                                                                                                             // readonly?

        void messageExpunged(MailboxFolder mf, MockMessage msg, boolean removed);

        void uidInvalidated();

    }
}
