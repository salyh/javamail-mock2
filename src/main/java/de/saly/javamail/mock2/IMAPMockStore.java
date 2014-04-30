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

import java.util.Map;
import java.util.UUID;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Quota;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.event.ConnectionEvent;

import com.sun.mail.imap.IMAPStore;

public class IMAPMockStore extends IMAPStore {
    private boolean connected;
    // private IMAPMockFolder folder;
    private MockMailbox mailbox;
    private final UUID objectId = UUID.randomUUID();
    final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    public IMAPMockStore(final Session session, final URLName urlname) {
        this(session, urlname, "imap", false);

    }

    public IMAPMockStore(final Session session, final URLName url, final String name, final boolean isSSL) {
        super(session, url, name, isSSL);

        logger.debug("Created " + objectId);
    }

    @Override
    public synchronized void close() throws MessagingException {
        this.connected = false;
        notifyConnectionListeners(ConnectionEvent.CLOSED);
        logger.debug("Closed " + objectId);
    }

    @Override
    public void connect() throws MessagingException {
        if (isConnected()) {
            throw new IllegalStateException("already connected");
        }
        super.connect(url.getHost(), url.getPort(), url.getUsername(), url.getPassword());
    }

    @Override
    public void connect(final String user, final String password) throws MessagingException {
        if (isConnected()) {
            throw new IllegalStateException("already connected");
        }
        super.connect(url.getHost(), url.getPort(), user, password);
    }

    @Override
    public void connect(final String host, final String user, final String password) throws MessagingException {
        if (isConnected()) {
            throw new IllegalStateException("already connected");
        }
        super.connect(host, url.getPort(), user, password);
    }

    @Override
    public Folder getDefaultFolder() throws MessagingException {
        checkConnected();

        return new IMAPDefaultMockFolder(this, mailbox);
    }

    @Override
    public Folder getFolder(final String name) throws MessagingException {
        checkConnected();
        logger.debug("getFolder(" + name + ")");
        if ("inbox".equalsIgnoreCase(name)) {
            return new IMAPMockFolder(this, mailbox.getInbox());
        } else {

            return new IMAPMockFolder(this, mailbox.getRoot().getOrAddSubFolder(name));
        }
    }

    @Override
    public Folder getFolder(final URLName url) throws MessagingException {
        checkConnected();
        return getFolder(url.getFile());
    }

    @Override
    public synchronized Quota[] getQuota(final String root) throws MessagingException {
        throw new MessagingException("QUOTA not supported");
    }

    @Override
    public Folder[] getSharedNamespaces() throws MessagingException {
        // TODO Auto-generated method stub
        return super.getSharedNamespaces();
    }

    @Override
    public Folder[] getUserNamespaces(final String user) throws MessagingException {
        // TODO Auto-generated method stub
        return super.getUserNamespaces(user);
    }

    /* (non-Javadoc)
     * @see com.sun.mail.imap.IMAPStore#hasCapability(java.lang.String)
     */
    @Override
    public synchronized boolean hasCapability(final String capability) throws MessagingException {
        return capability != null && capability.toLowerCase().startsWith("imap4");
    }

    @Override
    public synchronized Map<String, String> id(final Map<String, String> clientParams) throws MessagingException {
        throw new MessagingException("ID not supported");
    }

    // /-------

    @Override
    public void idle() throws MessagingException {
        throw new MessagingException("IDLE not supported");
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public synchronized void setQuota(final Quota quota) throws MessagingException {

        throw new MessagingException("QUOTA not supported");

    }

    protected void checkConnected() throws MessagingException {
        if (!isConnected()) {
            throw new MessagingException("Not connected");
        }
    }

    @Override
    protected boolean protocolConnect(final String host, final int port, final String user, final String password)
            throws MessagingException {
        logger.debug("Connect to " + user + " (" + objectId + ")");
        mailbox = MockMailbox.get(user);
        // folder = new IMAPMockFolder(this, mailbox.getInbox());
        if (mailbox.getInbox().isSimulateError()) {
            throw new MessagingException("Simulated error connecting to mailbox of " + user);
        }

        this.connected = true;

        return true;
    }

    Session getSession() {
        return session;
    }

}
