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

package org.apache.james.mime4j.field;

public class DefaultFieldParser extends DelegatingFieldParser {
    
    public DefaultFieldParser() {
        setFieldParser(AbstractField.CONTENT_TRANSFER_ENCODING, new ContentTransferEncodingField.Parser());
        setFieldParser(AbstractField.CONTENT_TYPE, new ContentTypeField.Parser());
        setFieldParser(AbstractField.CONTENT_DISPOSITION, new ContentDispositionField.Parser());
        
        final DateTimeField.Parser dateTimeParser = new DateTimeField.Parser();
        setFieldParser(AbstractField.DATE, dateTimeParser);
        setFieldParser(AbstractField.RESENT_DATE, dateTimeParser);
        
        final MailboxListField.Parser mailboxListParser = new MailboxListField.Parser();
        setFieldParser(AbstractField.FROM, mailboxListParser);
        setFieldParser(AbstractField.RESENT_FROM, mailboxListParser);
        
        final MailboxField.Parser mailboxParser = new MailboxField.Parser();
        setFieldParser(AbstractField.SENDER, mailboxParser);
        setFieldParser(AbstractField.RESENT_SENDER, mailboxParser);
        
        final AddressListField.Parser addressListParser = new AddressListField.Parser();
        setFieldParser(AbstractField.TO, addressListParser);
        setFieldParser(AbstractField.RESENT_TO, addressListParser);
        setFieldParser(AbstractField.CC, addressListParser);
        setFieldParser(AbstractField.RESENT_CC, addressListParser);
        setFieldParser(AbstractField.BCC, addressListParser);
        setFieldParser(AbstractField.RESENT_BCC, addressListParser);
        setFieldParser(AbstractField.REPLY_TO, addressListParser);
    }
}
