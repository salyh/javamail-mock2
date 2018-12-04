package de.saly.javamail.mock2.provider;

import de.saly.javamail.mock2.IMAPMockStore;

import javax.mail.Provider;

public class IMAPMockProvider extends Provider {
    public IMAPMockProvider() {
        super(Type.STORE, "imap", IMAPMockStore.class.getName(), "saly.de", null);
    }
}
