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

import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class MockMailbox {

    private static final Map<Address, MockMailbox> mailboxes = new HashMap<Address, MockMailbox>();

    public synchronized static MockMailbox get(final Address a) {
        MockMailbox mb = mailboxes.get(a);
        if (mb == null) {
            mailboxes.put(a, mb = new MockMailbox(a));
        }
        return mb;
    }

    public static MockMailbox get(final String address) throws AddressException {
        return get(new InternetAddress(address));
    }

    public static void resetAll() {

        mailboxes.clear();

    }

    private final Address address;
    private final MailboxFolder inbox;

    private final MailboxFolder root = new MailboxFolder("", this, true);

    private MockMailbox(final Address address) {
        super();
        this.address = address;
        inbox = root.addSpecialSubFolder("INBOX");
    }

    private MockMailbox(final String address) throws AddressException {
        this(new InternetAddress(address));
    }

    /**
     * @return the address
     */
    public Address getAddress() {
        return address;
    }

    public MailboxFolder getInbox() {
        return inbox;
    }

    public MailboxFolder getRoot() {
        return root;
    }

}
