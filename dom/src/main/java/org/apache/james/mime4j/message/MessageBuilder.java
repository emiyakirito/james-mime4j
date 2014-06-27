/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mime4j.message;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.dom.field.AddressListField;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentTransferEncodingField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.dom.field.MailboxField;
import org.apache.james.mime4j.dom.field.MailboxListField;
import org.apache.james.mime4j.dom.field.ParseException;
import org.apache.james.mime4j.dom.field.ParsedField;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.NameValuePair;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * {@link org.apache.james.mime4j.dom.Message} builder.
 */
public class MessageBuilder {

    private final List<Field> fields;
    private final Map<String, List<Field>> fieldMap;

    private Body body;

    public static MessageBuilder create() {
        return new MessageBuilder();
    }

    private MessageBuilder() {
        this.fields = new LinkedList<Field>();
        this.fieldMap = new HashMap<String, List<Field>>();
    }

    /**
     * Adds a field to the end of the list of fields.
     *
     * @param field the field to add.
     */
    public MessageBuilder addField(Field field) {
        List<Field> values = fieldMap.get(field.getName().toLowerCase());
        if (values == null) {
            values = new LinkedList<Field>();
            fieldMap.put(field.getName().toLowerCase(), values);
        }
        values.add(field);
        fields.add(field);
        return this;
    }

    /**
     * Gets the fields of this header. The returned list will not be
     * modifiable.
     *
     * @return the list of <code>Field</code> objects.
     */
    public List<Field> getFields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Gets a <code>Field</code> given a field name. If there are multiple
     * such fields defined in this header the first one will be returned.
     *
     * @param name the field name (e.g. From, Subject).
     * @return the field or <code>null</code> if none found.
     */
    public Field getField(String name) {
        List<Field> l = fieldMap.get(name.toLowerCase());
        if (l != null && !l.isEmpty()) {
            return l.get(0);
        }
        return null;
    }

    /**
     * Returns <code>true<code/> if there is at least one explicitly
     * set field with the given name.
     *
     * @param name the field name (e.g. From, Subject).
     * @return <code>true<code/> if there is at least one explicitly
     * set field with the given name, <code>false<code/> otherwise.
     */
    public boolean containsField(String name) {
        List<Field> l = fieldMap.get(name.toLowerCase());
        return l != null && !l.isEmpty();
    }

    /**
     * Gets all <code>Field</code>s having the specified field name.
     *
     * @param name the field name (e.g. From, Subject).
     * @return the list of fields.
     */
    public List<Field> getFields(final String name) {
        final String lowerCaseName = name.toLowerCase();
        final List<Field> l = fieldMap.get(lowerCaseName);
        final List<Field> results;
        if (l == null || l.isEmpty()) {
            results = Collections.emptyList();
        } else {
            results = Collections.unmodifiableList(l);
        }
        return results;
    }

    /**
     * Removes all <code>Field</code>s having the specified field name.
     *
     * @param name
     *            the field name (e.g. From, Subject).
     */
    public MessageBuilder removeFields(String name) {
        final String lowerCaseName = name.toLowerCase();
        List<Field> removed = fieldMap.remove(lowerCaseName);
        if (removed == null || removed.isEmpty()) {
            return this;
        }
        for (Iterator<Field> iterator = fields.iterator(); iterator.hasNext();) {
            Field field = iterator.next();
            if (field.getName().equalsIgnoreCase(name)) {
                iterator.remove();
            }
        }
        return this;
    }

    /**
     * Sets or replaces a field. This method is useful for header fields such as
     * Subject or Message-ID that should not occur more than once in a message.
     *
     * If this builder does not already contain a header field of
     * the same name as the given field then it is added to the end of the list
     * of fields (same behavior as {@link #addField(Field)}). Otherwise the
     * first occurrence of a field with the same name is replaced by the given
     * field and all further occurrences are removed.
     *
     * @param field the field to set.
     */
    public MessageBuilder setField(Field field) {
        final String lowerCaseName = field.getName().toLowerCase();
        List<Field> l = fieldMap.get(lowerCaseName);
        if (l == null || l.isEmpty()) {
            addField(field);
            return this;
        }

        l.clear();
        l.add(field);

        int firstOccurrence = -1;
        int index = 0;
        for (Iterator<Field> iterator = fields.iterator(); iterator.hasNext(); index++) {
            Field f = iterator.next();
            if (f.getName().equalsIgnoreCase(field.getName())) {
                iterator.remove();
                if (firstOccurrence == -1) {
                    firstOccurrence = index;
                }
            }
        }
        fields.add(firstOccurrence, field);
        return this;
    }

    @SuppressWarnings("unchecked")
    private <F extends ParsedField> F obtainField(String fieldName) {
        return (F) getField(fieldName);
    }

    /**
     * Returns MIME type of this message.
     *
     * @return the MIME type or <code>null</code> if no MIME
     *         type has been set.
     */
    public String getMimeType() {
        ContentTypeField field = obtainField(FieldName.CONTENT_TYPE);
        return field != null ? field.getMimeType() : null;
    }

    /**
     * Returns MIME character set encoding of this message.
     *
     * @return the MIME character set encoding or <code>null</code> if no charset
     *         type has been set.
     */
    public String getCharset() {
        ContentTypeField field = obtainField(FieldName.CONTENT_TYPE);
        return field != null ? field.getCharset() : null;
    }

    /**
     * Sets transfer encoding of this message.
     *
     * @param MIME type of this message
     *            the MIME type to use.
     */
    public void setContentType(String mimeType, NameValuePair... parameters) {
        if (mimeType == null) {
            removeFields(FieldName.CONTENT_TYPE);
        } else {
            setField(Fields.contentType(mimeType, parameters));
        }
    }

    /**
     * Returns transfer encoding of this message.
     *
     * @return the transfer encoding.
     */
    public String getContentTransferEncoding() {
        ContentTransferEncodingField field = obtainField(FieldName.CONTENT_TRANSFER_ENCODING);
        return field != null ? field.getEncoding() : null;
    }

    /**
     * Sets transfer encoding of this message.
     *
     * @param contentTransferEncoding
     *            transfer encoding to use.
     */
    public void setContentTransferEncoding(String contentTransferEncoding) {
        if (contentTransferEncoding == null) {
            removeFields(FieldName.CONTENT_TRANSFER_ENCODING);
        } else {
            setField(Fields.contentTransferEncoding(contentTransferEncoding));
        }
    }

    /**
     * Return disposition type of this message.
     *
     * @return the disposition type or <code>null</code> if no disposition
     *         type has been set.
     */
    public String getDispositionType() {
        ContentDispositionField field = obtainField(FieldName.CONTENT_DISPOSITION);
        return field != null ? field.getDispositionType() : null;
    }

    /**
     * Sets content disposition of this message to the
     * specified disposition type. No filename, size or date parameters
     * are included in the content disposition.
     *
     * @param dispositionType
     *            disposition type value (usually <code>inline</code> or
     *            <code>attachment</code>).
     */
    public void setContentDisposition(String dispositionType) {
        if (dispositionType == null) {
            removeFields(FieldName.CONTENT_DISPOSITION);
        } else {
            setField(Fields.contentDisposition(dispositionType));
        }
    }

    /**
     * Sets content disposition of this message to the
     * specified disposition type and filename. No size or date parameters are
     * included in the content disposition.
     *
     * @param dispositionType
     *            disposition type value (usually <code>inline</code> or
     *            <code>attachment</code>).
     * @param filename
     *            filename parameter value or <code>null</code> if the
     *            parameter should not be included.
     */
    public void setContentDisposition(String dispositionType, String filename) {
        if (dispositionType == null) {
            removeFields(FieldName.CONTENT_DISPOSITION);
        } else {
            setField(Fields.contentDisposition(dispositionType, filename));
        }
    }

    /**
     * Sets content disposition of this message to the
     * specified values. No date parameters are included in the content
     * disposition.
     *
     * @param dispositionType
     *            disposition type value (usually <code>inline</code> or
     *            <code>attachment</code>).
     * @param filename
     *            filename parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @param size
     *            size parameter value or <code>-1</code> if the parameter
     *            should not be included.
     */
    public void setContentDisposition(String dispositionType, String filename,
                                      long size) {
        if (dispositionType == null) {
            removeFields(FieldName.CONTENT_DISPOSITION);
        } else {
            setField(Fields.contentDisposition(dispositionType, filename, size));
        }
    }

    /**
     * Sets content disposition of this message to the
     * specified values.
     *
     * @param dispositionType
     *            disposition type value (usually <code>inline</code> or
     *            <code>attachment</code>).
     * @param filename
     *            filename parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @param size
     *            size parameter value or <code>-1</code> if the parameter
     *            should not be included.
     * @param creationDate
     *            creation-date parameter value or <code>null</code> if the
     *            parameter should not be included.
     * @param modificationDate
     *            modification-date parameter value or <code>null</code> if
     *            the parameter should not be included.
     * @param readDate
     *            read-date parameter value or <code>null</code> if the
     *            parameter should not be included.
     */
    public void setContentDisposition(String dispositionType, String filename,
                                      long size, Date creationDate, Date modificationDate, Date readDate) {
        if (dispositionType == null) {
            removeFields(FieldName.CONTENT_DISPOSITION);
        } else {
            setField(Fields.contentDisposition(dispositionType, filename, size,
                    creationDate, modificationDate, readDate));
        }
    }

    /**
     * Returns filename of the content disposition of this message.
     *
     * @return the filename parameter of the content disposition or
     *         <code>null</code> if the filename has not been set.
     */
    public String getFilename() {
        ContentDispositionField field = obtainField(FieldName.CONTENT_DISPOSITION);
        return field != null ? field.getFilename() : null;
    }

    /**
     * Returns size of the content disposition of this message.
     *
     * @return the size parameter of the content disposition or
     *         <code>-1</code> if the filename has not been set.
     */
    public long getSize() {
        ContentDispositionField field = obtainField(FieldName.CONTENT_DISPOSITION);
        return field != null ? field.getSize() : -1;
    }

    /**
     * Returns creation date of the content disposition of this message.
     *
     * @return the creation date parameter of the content disposition or
     *         <code>null</code> if the filename has not been set.
     */
    public Date getCreationDate() {
        ContentDispositionField field = obtainField(FieldName.CONTENT_DISPOSITION);
        return field != null ? field.getCreationDate() : null;
    }

    /**
     * Returns modification date of the content disposition of this message.
     *
     * @return the modification date parameter of the content disposition or
     *         <code>null</code> if the filename has not been set.
     */
    public Date getModificationDate() {
        ContentDispositionField field = obtainField(FieldName.CONTENT_DISPOSITION);
        return field != null ? field.getModificationDate() : null;
    }

    /**
     * Returns read date of the content disposition of this message.
     *
     * @return the read date parameter of the content disposition or
     *         <code>null</code> if the filename has not been set.
     */
    public Date getReadDate() {
        ContentDispositionField field = obtainField(FieldName.CONTENT_DISPOSITION);
        return field != null ? field.getReadDate() : null;
    }

    /**
     * Returns the value of the <i>Message-ID</i> header field of this message
     * or <code>null</code> if it is not present.
     *
     * @return the identifier of this message.
     */
    public String getMessageId() {
        Field field = obtainField(FieldName.MESSAGE_ID);
        return field != null ? field.getBody() : null;
    }

    /**
     * Generates and sets message ID for this message.
     *
     * @param hostname
     *            host name to be included in the identifier or
     *            <code>null</code> if no host name should be included.
     */
    public MessageBuilder generateMessageId(final String hostname) {
        if (hostname == null) {
            removeFields(FieldName.MESSAGE_ID);
        } else {
            setField(Fields.generateMessageId(hostname));
        }
        return this;
    }

    /**
     * Generates and sets message ID for this message.
     *
     * @param hostname
     *            host name to be included in the identifier or
     *            <code>null</code> if no host name should be included.
     */
    public MessageBuilder setMessageId(final String messageId) {
        if (messageId == null) {
            removeFields(FieldName.MESSAGE_ID);
        } else {
            setField(Fields.messageId(messageId));
        }
        return this;
    }

    /**
     * Returns the (decoded) value of the <i>Subject</i> header field of this
     * message or <code>null</code> if it is not present.
     *
     * @return the subject of this message.
     */
    public String getSubject() {
        UnstructuredField field = obtainField(FieldName.SUBJECT);
        return field != null ? field.getValue() : null;
    }

    /**
     * Sets <i>Subject</i> header field for this message. The specified
     * string may contain non-ASCII characters, in which case it gets encoded as
     * an 'encoded-word' automatically.
     *
     * @param subject
     *            subject to set or <code>null</code> to remove the subject
     *            header field.
     */
    public MessageBuilder setSubject(final String subject) {
        if (subject == null) {
            removeFields(FieldName.SUBJECT);
        } else {
            setField(Fields.subject(subject));
        }
        return this;
    }

    /**
     * Returns the value of the <i>Date</i> header field of this message as
     * <code>Date</code> object or <code>null</code> if it is not present.
     *
     * @return the date of this message.
     */
    public Date getDate() {
        DateTimeField field = obtainField(FieldName.DATE);
        return field != null ? field.getDate() : null;
    }

    /**
     * Sets <i>Date</i> header field for this message. This method uses the
     * default <code>TimeZone</code> of this host to encode the specified
     * <code>Date</code> object into a string.
     *
     * @param date
     *            date to set or <code>null</code> to remove the date header
     *            field.
     */
    public MessageBuilder setDate(Date date) {
        return setDate(date, null);
    }

    /**
     * Sets <i>Date</i> header field for this message. The specified
     * <code>TimeZone</code> is used to encode the specified <code>Date</code>
     * object into a string.
     *
     * @param date
     *            date to set or <code>null</code> to remove the date header
     *            field.
     * @param zone
     *            a time zone.
     */
    public MessageBuilder setDate(Date date, TimeZone zone) {
        if (date == null) {
            removeFields(FieldName.DATE);
        } else {
            setField(Fields.date(FieldName.DATE, date, zone));
        }
        return this;
    }

    /**
     * Returns the value of the <i>Sender</i> header field of this message as
     * <code>Mailbox</code> object or <code>null</code> if it is not
     * present.
     *
     * @return the sender of this message.
     */
    public Mailbox getSender() {
        return getMailbox(FieldName.SENDER);
    }

    /**
     * Sets <i>Sender</i> header field of this message to the specified
     * mailbox address.
     *
     * @param sender
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public MessageBuilder setSender(Mailbox sender) {
        return setMailbox(FieldName.SENDER, sender);
    }

    /**
     * Sets <i>Sender</i> header field of this message to the specified
     * mailbox address.
     *
     * @param sender
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public MessageBuilder setSender(String sender) throws ParseException {
        return setMailbox(FieldName.SENDER, sender);
    }

    /**
     * Returns the value of the <i>From</i> header field of this message as
     * <code>MailboxList</code> object or <code>null</code> if it is not
     * present.
     *
     * @return value of the from field of this message.
     */
    public MailboxList getFrom() {
        return getMailboxList(FieldName.FROM);
    }

    /**
     * Sets <i>From</i> header field of this message to the specified
     * mailbox address.
     *
     * @param from
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public MessageBuilder setFrom(Mailbox from) {
        return setMailboxList(FieldName.FROM, from);
    }

    /**
     * Sets <i>From</i> header field of this message to the specified
     * mailbox address.
     *
     * @param from
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public MessageBuilder setFrom(String from) throws ParseException {
        return setMailboxList(FieldName.FROM, from);
    }

    /**
     * Sets <i>From</i> header field of this message to the specified
     * mailbox addresses.
     *
     * @param from
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public MessageBuilder setFrom(Mailbox... from) {
        return setMailboxList(FieldName.FROM, from);
    }

    /**
     * Sets <i>From</i> header field of this message to the specified
     * mailbox addresses.
     *
     * @param from
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public MessageBuilder setFrom(String... from) throws ParseException {
        return setMailboxList(FieldName.FROM, from);
    }

    /**
     * Sets <i>From</i> header field of this message to the specified
     * mailbox addresses.
     *
     * @param from
     *            addresses to set or <code>null</code> or an empty collection
     *            to remove the header field.
     */
    public MessageBuilder setFrom(Collection<Mailbox> from) {
        return setMailboxList(FieldName.FROM, from);
    }

    /**
     * Returns the value of the <i>To</i> header field of this message as
     * <code>AddressList</code> object or <code>null</code> if it is not
     * present.
     *
     * @return value of the to field of this message.
     */
    public AddressList getTo() {
        return getAddressList(FieldName.TO);
    }

    /**
     * Sets <i>To</i> header field of this message to the specified
     * address.
     *
     * @param to
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public MessageBuilder setTo(Address to) {
        return setAddressList(FieldName.TO, to);
    }

    /**
     * Sets <i>To</i> header field of this message to the specified
     * address.
     *
     * @param to
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public MessageBuilder setTo(String to) throws ParseException {
        return setAddressList(FieldName.TO, to);
    }

    /**
     * Sets <i>To</i> header field of this message to the specified
     * addresses.
     *
     * @param to
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public MessageBuilder setTo(Address... to) {
        return setAddressList(FieldName.TO, to);
    }

    /**
     * Sets <i>To</i> header field of this message to the specified
     * addresses.
     *
     * @param to
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public MessageBuilder setTo(String... to) throws ParseException {
        return setAddressList(FieldName.TO, to);
    }

    /**
     * Sets <i>To</i> header field of this message to the specified
     * addresses.
     *
     * @param to
     *            addresses to set or <code>null</code> or an empty collection
     *            to remove the header field.
     */
    public MessageBuilder setTo(Collection<? extends Address> to) {
        return setAddressList(FieldName.TO, to);
    }

    /**
     * Returns the value of the <i>Cc</i> header field of this message as
     * <code>AddressList</code> object or <code>null</code> if it is not
     * present.
     *
     * @return value of the cc field of this message.
     */
    public AddressList getCc() {
        return getAddressList(FieldName.CC);
    }

    /**
     * Sets <i>Cc</i> header field of this message to the specified
     * address.
     *
     * @param cc
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public MessageBuilder setCc(Address cc) {
        return setAddressList(FieldName.CC, cc);
    }

    /**
     * Sets <i>Cc</i> header field of this message to the specified
     * addresses.
     *
     * @param cc
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public MessageBuilder setCc(Address... cc) {
        return setAddressList(FieldName.CC, cc);
    }

    /**
     * Sets <i>Cc</i> header field of this message to the specified
     * addresses.
     *
     * @param cc
     *            addresses to set or <code>null</code> or an empty collection
     *            to remove the header field.
     */
    public MessageBuilder setCc(Collection<? extends Address> cc) {
        return setAddressList(FieldName.CC, cc);
    }

    /**
     * Returns the value of the <i>Bcc</i> header field of this message as
     * <code>AddressList</code> object or <code>null</code> if it is not
     * present.
     *
     * @return value of the bcc field of this message.
     */
    public AddressList getBcc() {
        return getAddressList(FieldName.BCC);
    }

    /**
     * Sets <i>Bcc</i> header field of this message to the specified
     * address.
     *
     * @param bcc
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public MessageBuilder setBcc(Address bcc) {
        return setAddressList(FieldName.BCC, bcc);
    }

    /**
     * Sets <i>Bcc</i> header field of this message to the specified
     * addresses.
     *
     * @param bcc
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public MessageBuilder setBcc(Address... bcc) {
        return setAddressList(FieldName.BCC, bcc);
    }

    /**
     * Sets <i>Bcc</i> header field of this message to the specified
     * addresses.
     *
     * @param bcc
     *            addresses to set or <code>null</code> or an empty collection
     *            to remove the header field.
     */
    public MessageBuilder setBcc(Collection<? extends Address> bcc) {
        return setAddressList(FieldName.BCC, bcc);
    }

    /**
     * Returns the value of the <i>Reply-To</i> header field of this message as
     * <code>AddressList</code> object or <code>null</code> if it is not
     * present.
     *
     * @return value of the reply to field of this message.
     */
    public AddressList getReplyTo() {
        return getAddressList(FieldName.REPLY_TO);
    }

    /**
     * Sets <i>Reply-To</i> header field of this message to the specified
     * address.
     *
     * @param replyTo
     *            address to set or <code>null</code> to remove the header
     *            field.
     */
    public MessageBuilder setReplyTo(Address replyTo) {
        return setAddressList(FieldName.REPLY_TO, replyTo);
    }

    /**
     * Sets <i>Reply-To</i> header field of this message to the specified
     * addresses.
     *
     * @param replyTo
     *            addresses to set or <code>null</code> or no arguments to
     *            remove the header field.
     */
    public MessageBuilder setReplyTo(Address... replyTo) {
        return setAddressList(FieldName.REPLY_TO, replyTo);
    }

    /**
     * Sets <i>Reply-To</i> header field of this message to the specified
     * addresses.
     *
     * @param replyTo
     *            addresses to set or <code>null</code> or an empty collection
     *            to remove the header field.
     */
    public MessageBuilder setReplyTo(Collection<? extends Address> replyTo) {
        return setAddressList(FieldName.REPLY_TO, replyTo);
    }

    private Mailbox getMailbox(String fieldName) {
        MailboxField field = obtainField(fieldName);
        return field != null ? field.getMailbox() : null;
    }

    private MessageBuilder setMailbox(String fieldName, Mailbox mailbox) {
        if (mailbox == null) {
            removeFields(fieldName);
        } else {
            setField(Fields.mailbox(fieldName, mailbox));
        }
        return this;
    }

    private MessageBuilder setMailbox(String fieldName, String mailbox) throws ParseException {
        if (mailbox == null) {
            removeFields(fieldName);
        } else {
            setField(Fields.mailbox(fieldName, AddressBuilder.DEFAULT.parseMailbox(mailbox)));
        }
        return this;
    }

    private MailboxList getMailboxList(String fieldName) {
        MailboxListField field = obtainField(fieldName);
        return field != null ? field.getMailboxList() : null;
    }

    private MessageBuilder setMailboxList(String fieldName, Mailbox mailbox) {
        return setMailboxList(fieldName, mailbox == null ? null : Collections.singleton(mailbox));
    }

    private MessageBuilder setMailboxList(String fieldName, String mailbox) throws ParseException {
        return setMailboxList(fieldName, mailbox == null ? null : AddressBuilder.DEFAULT.parseMailbox(mailbox));
    }

    private MessageBuilder setMailboxList(String fieldName, Mailbox... mailboxes) {
        return setMailboxList(fieldName, mailboxes == null ? null : Arrays.asList(mailboxes));
    }

    private List<Mailbox> parseMailboxes(String... mailboxes) throws ParseException {
        if (mailboxes == null || mailboxes.length == 0) {
            return null;
        } else {
            List<Mailbox> list = new ArrayList<Mailbox>();
            for (String mailbox: mailboxes) {
                list.add(AddressBuilder.DEFAULT.parseMailbox(mailbox));
            }
            return list;
        }
    }

    private MessageBuilder setMailboxList(String fieldName, String... mailboxes) throws ParseException {
        return setMailboxList(fieldName, parseMailboxes(mailboxes));
    }

    private MessageBuilder setMailboxList(String fieldName, Collection<Mailbox> mailboxes) {
        if (mailboxes == null || mailboxes.isEmpty()) {
            removeFields(fieldName);
        } else {
            setField(Fields.mailboxList(fieldName, mailboxes));
        }
        return this;
    }

    private AddressList getAddressList(String fieldName) {
        AddressListField field = obtainField(fieldName);
        return field != null? field.getAddressList() : null;
    }

    private MessageBuilder setAddressList(String fieldName, Address address) {
        return setAddressList(fieldName, address == null ? null : Collections.singleton(address));
    }

    private MessageBuilder setAddressList(String fieldName, String address) throws ParseException {
        return setAddressList(fieldName, address == null ? null :AddressBuilder.DEFAULT.parseMailbox(address));
    }

    private MessageBuilder setAddressList(String fieldName, Address... addresses) {
        return setAddressList(fieldName, addresses == null ? null : Arrays.asList(addresses));
    }

    private List<Address> parseAddresses(String... addresses) throws ParseException {
        if (addresses == null || addresses.length == 0) {
            return null;
        } else {
            List<Address> list = new ArrayList<Address>();
            for (String address: addresses) {
                list.add(AddressBuilder.DEFAULT.parseAddress(address));
            }
            return list;
        }
    }

    private MessageBuilder setAddressList(String fieldName, String... addresses) throws ParseException {
        return setAddressList(fieldName, parseAddresses(addresses));
    }

    private MessageBuilder setAddressList(String fieldName, Collection<? extends Address> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            removeFields(fieldName);
        } else {
            setField(Fields.addressList(fieldName, addresses));
        }
        return this;
    }

    /**
     * Sets body of this message.
     *
     * @param body
     *            the body.
     */
    public MessageBuilder setBody(Body body) {
        this.body = body;
        return this;
    }

    /**
     * Sets body of this message.
     *
     * @param body
     *            the body.
     */
    public MessageBuilder setBody(String body, Charset charset) {
        return setBody(SingleBodyBuilder.create()
                .setText(body)
                .setCharset(charset)
                .build());
    }

    public Message build() {
        Message message = new MessageImpl();
        final Header header = message.getHeader();
        if (!containsField(FieldName.MIME_VERSION)) {
            header.setField(Fields.version("1.0"));
        }
        for (final Field field : fields) {
            header.addField(field);
        }

        if (!containsField(FieldName.CONTENT_TYPE) && body != null) {
            if (body instanceof Message) {
                header.setField(Fields.contentType("message/rfc822"));
            } else if (body instanceof Multipart) {
                Multipart multipart = (Multipart) body;
                header.setField(Fields.contentType("multipart/" + multipart.getSubType(),
                        new NameValuePair("boundary", MimeUtil.createUniqueBoundary())));
            } else if (body instanceof TextBody) {
                TextBody textBody = (TextBody) body;
                String mimeCharset = textBody.getMimeCharset();
                if ("us-ascii".equalsIgnoreCase(mimeCharset)) {
                    mimeCharset = null;
                }
                if (mimeCharset != null) {
                    header.setField(Fields.contentType("text/plain", new NameValuePair("charset", mimeCharset)));
                } else {
                    header.setField(Fields.contentType("text/plain"));
                }
            }
        }

        if (!containsField(FieldName.DATE)) {
            header.setField(Fields.date(new Date()));
        }

        message.setBody(body);

        return message;
    }

}