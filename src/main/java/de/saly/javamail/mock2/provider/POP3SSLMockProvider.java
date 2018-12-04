package de.saly.javamail.mock2.provider;

import de.saly.javamail.mock2.POP3SSLMockStore;

import javax.mail.Provider;

public class POP3SSLMockProvider extends Provider {
    public POP3SSLMockProvider() {
        super(Type.STORE, "pop3s", POP3SSLMockStore.class.getName(), "saly.de", null);
    }
}
