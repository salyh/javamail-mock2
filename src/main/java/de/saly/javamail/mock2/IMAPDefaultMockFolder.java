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

import javax.mail.Folder;
import javax.mail.MessagingException;

import com.sun.mail.imap.DefaultFolder;

public class IMAPDefaultMockFolder extends DefaultFolder {

    private final MockMailbox mailbox;
    // private final IMAPMockFolder[] children;
    private final IMAPMockStore store;
    final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    protected IMAPDefaultMockFolder(final IMAPMockStore store, final MockMailbox mailbox) {
        super(store);
        // this.children = children;
        this.mailbox = mailbox;
        this.store = store;
        logger.debug("Created");
    }

    @Override
    public synchronized boolean create(final int type) throws MessagingException {
        return true;
    }

    @Override
    public synchronized boolean exists() throws MessagingException {
        return true;
    }

    @Override
    public Folder getFolder(final String name) throws MessagingException {

        logger.debug("getFolder(" + name + ") on " + getFullName());

        if ("inbox".equalsIgnoreCase(name)) {
            return new IMAPMockFolder(store, mailbox.getInbox());
        }

        return new IMAPMockFolder(store, mailbox.getRoot().getOrAddSubFolder(name));

    }

    @Override
    public int getType() throws MessagingException {
        checkExists();
        return HOLDS_FOLDERS;
    }

    @Override
    public Folder[] list(final String pattern) throws MessagingException {

        final List<MailboxFolder> children = mailbox.getRoot().getChildren();
        final List<Folder> ret = new ArrayList<Folder>();

        for (final MailboxFolder mf : children) {
            if (mf.isExists()) {
                ret.add(new IMAPMockFolder(store, mf));
            }
        }

        logger.debug("Folder (Defaultroot) list return " + ret);

        return ret.toArray(new Folder[ret.size()]);

    }

    @Override
    public synchronized Folder[] listSubscribed(final String pattern) throws MessagingException {
        return new Folder[0];
    }

}
