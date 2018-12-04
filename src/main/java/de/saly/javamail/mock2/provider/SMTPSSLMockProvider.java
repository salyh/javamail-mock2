package de.saly.javamail.mock2.provider;

import de.saly.javamail.mock2.MockTransport;

import javax.mail.Provider;

public class SMTPSSLMockProvider extends Provider {
    public SMTPSSLMockProvider() {
        super(Type.TRANSPORT, "smtps", MockTransport.class.getName(), "saly.de", null);
    }
}
