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

package org.apache.james.mime4j.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.james.mime4j.ExampleMail;

import junit.framework.TestCase;

public class CodecUtilTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCopy() throws Exception {
        byte[] content = ExampleMail.MULTIPART_WITH_BINARY_ATTACHMENTS_BYTES;
        ByteArrayInputStream in = new ByteArrayInputStream(content);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CodecUtil.copy(in, out);
        assertEquals(content, out.toByteArray());
    }
    
    public void testEncodeQuotedPrintableLargeInput() throws Exception {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 1024 * 5; i++) {
            sb.append((char) ('0' + (i % 10)));
        }
        String expected = sb.toString().replaceAll("(\\d{75})", "$1=\r\n");
        
        InputStream in = new ByteArrayInputStream(sb.toString().getBytes("US-ASCII"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CodecUtil.encodeQuotedPrintableBinary(in, out);
        String actual = new String(out.toByteArray(), "US-ASCII");
        assertEquals(expected, actual);
    }

    public void testEncodeQuotedPrintableNonAsciiChars() throws Exception {
        String s = "7bit content with euro \u20AC symbol";
        InputStream in = new ByteArrayInputStream(s.getBytes("iso-8859-15"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CodecUtil.encodeQuotedPrintableBinary(in, out);
        String actual = new String(out.toByteArray(), "US-ASCII");
        assertEquals("7bit=20content=20with=20euro=20=A4=20symbol", actual);
    }
    
    private void assertEquals(byte[] expected, byte[] actual) {
        StringBuffer buffer = new StringBuffer(expected.length);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            buffer.append((char)actual[i]);
            assertEquals("Mismatch@" + i, expected[i], actual[i]);
        }
    }
}