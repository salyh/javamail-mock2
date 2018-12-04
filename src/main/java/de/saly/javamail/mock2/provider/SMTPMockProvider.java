package de.saly.javamail.mock2.provider;

import de.saly.javamail.mock2.MockTransport;

import javax.mail.Provider;

public class SMTPMockProvider extends Provider {
    public SMTPMockProvider() {
        super(Type.TRANSPORT, "smtp", MockTransport.class.getName(), "saly.de", null);
    }
}
