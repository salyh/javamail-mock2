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

import javax.mail.Provider;

public final class Providers {

    public static Provider getIMAPProvider(final String protocol, final boolean secure, final boolean mock) {
        if (mock) {
            return new Provider(Provider.Type.STORE, protocol, secure ? "de.saly.javamail.mock2.IMAPSSLMockStore"
                    : "de.saly.javamail.mock2.IMAPMockStore", "JavaMail Mock2 provider", null);
        }

        return new Provider(Provider.Type.STORE, protocol, secure ? "com.sun.mail.imap.IMAPSSLStore" : "com.sun.mail.imap.IMAPStore",
                "Oracle", null);

    }

    public static Provider getPOP3Provider(final String protocol, final boolean secure, final boolean mock) {
        if (mock) {
            return new Provider(Provider.Type.STORE, protocol, secure ? "de.saly.javamail.mock2.POP3SSLMockStore"
                    : "de.saly.javamail.mock2.POP3MockStore", "JavaMail Mock2 provider", null);
        }

        return new Provider(Provider.Type.STORE, protocol, secure ? "com.sun.mail.pop3.POP3SSLStore" : "com.sun.mail.pop3.POP3Store",
                "Oracle", null);

    }

    public static Provider getSMTPProvider(final String protocol, final boolean secure, final boolean mock) {
        if (mock) {
            return new Provider(Provider.Type.TRANSPORT, protocol, "de.saly.javamail.mock2.MockTransport", "JavaMail Mock2 provider", null);
        }

        return new Provider(Provider.Type.TRANSPORT, protocol, secure ? "com.sun.mail.smtp.SMTPSSLTransport"
                : "com.sun.mail.smtp.SMTPTransport", "Oracle", null);
    }

    private Providers() {
        // empty
    }

}
