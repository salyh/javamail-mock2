package de.saly.javamail.mock2.provider;

import de.saly.javamail.mock2.POP3MockStore;

import javax.mail.Provider;

public class POP3MockProvider extends Provider {
    public POP3MockProvider() {
        super(Type.STORE, "pop3", POP3MockStore.class.getName(), "saly.de", null);
    }
}
