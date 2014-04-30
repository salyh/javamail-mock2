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

import java.util.Date;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.IllegalWriteException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

public class MockMessage extends MimeMessage implements Comparable<MockMessage> {

    private final FlagChangeListener flagChangeListener;
    private Folder folder;
    private final MailboxFolder mbf;
    private final long mockid;
    final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    protected MockMessage(final Message source, final Folder folder) throws MessagingException {
        this((MimeMessage) source, ((MockMessage) source).mockid, ((MockMessage) source).mbf, ((MockMessage) source).flagChangeListener);
        this.folder = folder;
        this.setMessageNumber(source.getMessageNumber());
    }

    protected MockMessage(final MimeMessage source, final long mockid, final MailboxFolder mbf, final FlagChangeListener flagChangeListener)
            throws MessagingException {
        super(source);
        this.mockid = mockid;
        this.flagChangeListener = flagChangeListener;
        this.mbf = mbf;

    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#addFrom(javax.mail.Address[])
     */
    @Override
    public void addFrom(final Address[] addresses) throws MessagingException {

        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#addHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void addHeader(final String name, final String value) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#addHeaderLine(java.lang.String)
     */
    @Override
    public void addHeaderLine(final String line) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.Message#addRecipient(javax.mail.Message.RecipientType, javax.mail.Address)
     */
    @Override
    public void addRecipient(final javax.mail.Message.RecipientType type, final Address address) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#addRecipients(javax.mail.Message.RecipientType, javax.mail.Address[])
     */
    @Override
    public void addRecipients(final javax.mail.Message.RecipientType type, final Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#addRecipients(javax.mail.Message.RecipientType, java.lang.String)
     */
    @Override
    public void addRecipients(final javax.mail.Message.RecipientType type, final String addresses) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    @Override
    public int compareTo(final MockMessage o) {

        return new Long(this.getMockid()).compareTo(new Long(o.getMockid()));
    }

    @Override
    public synchronized Folder getFolder() {
        if (folder == null) {
            throw new RuntimeException("wrong/unshaded mock message");
        } else {
            return folder;
        }
    }

    /**
     * @return the mockid
     */
    public long getMockid() {
        return mockid;
    }

    // IllegalWriteException("Mock messages are read-only");

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#removeHeader(java.lang.String)
     */
    @Override
    public void removeHeader(final String name) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#saveChanges()
     */
    @Override
    public void saveChanges() throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setContent(javax.mail.Multipart)
     */
    @Override
    public void setContent(final Multipart mp) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setContent(java.lang.Object, java.lang.String)
     */
    @Override
    public void setContent(final Object o, final String type) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setContentID(java.lang.String)
     */
    @Override
    public void setContentID(final String cid) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setContentLanguage(java.lang.String[])
     */
    @Override
    public void setContentLanguage(final String[] languages) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setContentMD5(java.lang.String)
     */
    @Override
    public void setContentMD5(final String md5) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    @Override
    public void setDataHandler(final DataHandler content) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setDescription(java.lang.String)
     */
    @Override
    public void setDescription(final String description) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setDescription(java.lang.String, java.lang.String)
     */
    @Override
    public void setDescription(final String description, final String charset) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setDisposition(java.lang.String)
     */
    @Override
    public void setDisposition(final String disposition) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setFileName(java.lang.String)
     */
    @Override
    public void setFileName(final String filename) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setFlags(javax.mail.Flags, boolean)
     */
    @Override
    public synchronized void setFlags(final Flags flag, final boolean set) throws MessagingException {

        super.setFlags(flag, set);

        if (flagChangeListener != null) {
            flagChangeListener.onFlagChange(this, flag, set);
        }
               
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setFrom()
     */
    @Override
    public void setFrom() throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setFrom(javax.mail.Address)
     */
    @Override
    public void setFrom(final Address address) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setFrom(java.lang.String)
     */
    @Override
    public void setFrom(final String address) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void setHeader(final String name, final String value) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.Message#setRecipient(javax.mail.Message.RecipientType, javax.mail.Address)
     */
    @Override
    public void setRecipient(final javax.mail.Message.RecipientType type, final Address address) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setRecipients(javax.mail.Message.RecipientType, javax.mail.Address[])
     */
    @Override
    public void setRecipients(final javax.mail.Message.RecipientType type, final Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setRecipients(javax.mail.Message.RecipientType, java.lang.String)
     */
    @Override
    public void setRecipients(final javax.mail.Message.RecipientType type, final String addresses) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setReplyTo(javax.mail.Address[])
     */
    @Override
    public void setReplyTo(final Address[] addresses) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setSender(javax.mail.Address)
     */
    @Override
    public void setSender(final Address address) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setSentDate(java.util.Date)
     */
    @Override
    public void setSentDate(final Date d) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setSubject(java.lang.String)
     */
    @Override
    public void setSubject(final String subject) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setSubject(java.lang.String, java.lang.String)
     */
    @Override
    public void setSubject(final String subject, final String charset) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setText(java.lang.String)
     */
    @Override
    public void setText(final String text) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setText(java.lang.String, java.lang.String)
     */
    @Override
    public void setText(final String text, final String charset) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    /* (non-Javadoc)
     * @see javax.mail.internet.MimeMessage#setText(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void setText(final String text, final String charset, final String subtype) throws MessagingException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

    @Override
    protected void setExpunged(final boolean expunged) {

        super.setExpunged(expunged);
    }

    @Override
    protected void setMessageNumber(final int msgnum) {

        super.setMessageNumber(msgnum);
    }

    void setSpecialHeader(final String name, final String value) throws MessagingException {
        super.addHeader(name, value);
    }

    public static interface FlagChangeListener {
        void onFlagChange(MockMessage msg, Flags flags, boolean set);
    }

}
