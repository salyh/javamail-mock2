package de.saly.javamail.mock2.provider;

import de.saly.javamail.mock2.IMAPSSLMockStore;

import javax.mail.Provider;

public class IMAPSSLMockProvider extends Provider {
    public IMAPSSLMockProvider() {
        super(Type.STORE, "imaps", IMAPSSLMockStore.class.getName(), "saly.de", null);
    }
}
