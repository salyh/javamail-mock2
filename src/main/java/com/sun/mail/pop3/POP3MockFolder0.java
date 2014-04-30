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
package com.sun.mail.pop3;

import de.saly.javamail.mock2.POP3MockStore;

/**
 * this class is needed currently because com.sun.mail.pop3.POP3Folder has a
 * constructor with default visibility
 * See https://kenai.com/bugzilla/show_bug.cgi?id=6379
 */
public class POP3MockFolder0 extends POP3Folder {

    // POP3 has only one Folder INBOX
    private static final String INBOX = "INBOX";

    // make constructor visible for subclasses
    protected POP3MockFolder0(final POP3MockStore store) {
        super(store, INBOX);
    }

}
