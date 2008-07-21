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

package org.apache.james.mime4j.field.address;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.field.address.parser.ParseException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;

public class AddressTest extends TestCase {

    public void testExceptionTree() {
        // make sure that our ParseException extends MimeException.
        assertTrue(MimeException.class.isAssignableFrom(ParseException.class));
    }

    public void testParse1() throws ParseException {
        AddressList addrList = AddressList.parse("John Doe <jdoe@machine(comment).  example>");
        assertEquals(1, addrList.size());
        NamedMailbox mailbox = (NamedMailbox)addrList.get(0);
        assertEquals("John Doe", mailbox.getName());
        assertEquals("jdoe", mailbox.getLocalPart());
        assertEquals("machine.example", mailbox.getDomain());
    }

    public void testParse2() throws ParseException {
        AddressList addrList = AddressList.parse("Mary Smith \t    \t\t  <mary@example.net>");
        assertEquals(1, addrList.size());
        NamedMailbox mailbox = (NamedMailbox)addrList.get(0);
        assertEquals("Mary Smith", mailbox.getName());
        assertEquals("mary", mailbox.getLocalPart());
        assertEquals("example.net", mailbox.getDomain());
    }

    public void testEmptyGroup() throws ParseException {
        AddressList addrList = AddressList.parse("undisclosed-recipients:;");
        assertEquals(1, addrList.size());
        Group group = (Group)addrList.get(0);
        assertEquals(0, group.getMailboxes().size());
        assertEquals("undisclosed-recipients", group.getName());
    }

    public void testMessyGroupAndMailbox() throws ParseException {
        AddressList addrList = AddressList.parse("Marketing  folks :  Jane Smith < jane @ example . net >, \" Jack \\\"Jackie\\\" Jones \" < jjones@example.com > (comment(comment)); ,, (comment)  , <@example . net,@example(ignore\\)).com:(ignore)john@(ignore)example.net>");
        assertEquals(2, addrList.size());

        Group group = (Group)addrList.get(0);
        assertEquals("Marketing  folks", group.getName());
        assertEquals(2, group.getMailboxes().size());

        NamedMailbox namedMailbox1 = (NamedMailbox)group.getMailboxes().get(0);
        NamedMailbox namedMailbox2 = (NamedMailbox)group.getMailboxes().get(1);

        assertEquals("Jane Smith", namedMailbox1.getName());
        assertEquals("jane", namedMailbox1.getLocalPart());
        assertEquals("example.net", namedMailbox1.getDomain());

        assertEquals(" Jack \"Jackie\" Jones ", namedMailbox2.getName());
        assertEquals("jjones", namedMailbox2.getLocalPart());
        assertEquals("example.com", namedMailbox2.getDomain());

        Mailbox mailbox = (Mailbox)addrList.get(1);
        assertFalse(mailbox instanceof NamedMailbox);
        assertEquals("john", mailbox.getLocalPart());
        assertEquals("example.net", mailbox.getDomain());
        assertEquals(2, mailbox.getRoute().size());
        assertEquals("example.net", mailbox.getRoute().get(0));
        assertEquals("example.com", mailbox.getRoute().get(1));
    }

    public void testEmptyAddressList() throws ParseException {
        assertEquals(0, AddressList.parse("  \t   \t ").size());
        assertEquals(0, AddressList.parse("  \t  ,  , , ,,, , \t ").size());
    }

    public void testSimpleForm() throws ParseException {
        AddressList addrList = AddressList.parse("\"a b c d e f g\" (comment) @example.net");
        assertEquals(1, addrList.size());
        Mailbox mailbox = (Mailbox)addrList.get(0);
        assertEquals("a b c d e f g", mailbox.getLocalPart());
        assertEquals("example.net", mailbox.getDomain());
    }

    public void testFlatten() throws ParseException {
        AddressList addrList = AddressList.parse("dev : one@example.com, two@example.com; , ,,, marketing:three@example.com ,four@example.com;, five@example.com");
        assertEquals(3, addrList.size());
        assertEquals(5, addrList.flatten().size());
    }

    public void testTortureTest() throws ParseException {

        // Source: http://mailformat.dan.info/headers/from.html
        // (Commented out pending confirmation of legality--I think the local-part is illegal.) 
        // AddressList.parse("\"Guy Macon\" <guymacon+\" http://www.guymacon.com/ \"00@spamcop.net>");

        // Taken mostly from RFC822.

        // Just make sure these are recognized as legal address lists;
        // there shouldn't be any aspect of the RFC that is tested here
        // but not in the other unit tests.

        AddressList.parse("Alfred Neuman <Neuman@BBN-TENEXA>");
        AddressList.parse("Neuman@BBN-TENEXA");
        AddressList.parse("\"George, Ted\" <Shared@Group.Arpanet>");
        AddressList.parse("Wilt . (the Stilt) Chamberlain@NBA.US");

        // NOTE: In RFC822 8.1.5, the following example did not have "Galloping Gourmet"
        // in double-quotes.  I can only assume this was a typo, since 6.2.4 specifically
        // disallows spaces in unquoted local-part.
        AddressList.parse("     Gourmets:  Pompous Person <WhoZiWhatZit@Cordon-Bleu>," +
                "                Childs@WGBH.Boston, \"Galloping Gourmet\"@" +
                "                ANT.Down-Under (Australian National Television)," +
                "                Cheapie@Discount-Liquors;," +
                "       Cruisers:  Port@Portugal, Jones@SEA;," +
                "         Another@Somewhere.SomeOrg");

        // NOTE: In RFC822 8.3.3, the following example ended with a lone ">" after
        // Tops-20-Host.  I can only assume this was a typo, since 6.1 clearly shows
        // ">" requires a matching "<".
        AddressList.parse("Important folk:" +
                "                   Tom Softwood <Balsa@Tree.Root>," +
                "                   \"Sam Irving\"@Other-Host;," +
                "                 Standard Distribution:" +
                "                   /main/davis/people/standard@Other-Host," +
                "                   \"<Jones>standard.dist.3\"@Tops-20-Host;");

        // The following are from a Usenet post by Dan J. Bernstein:
        // http://groups.google.com/groups?selm=1996Aug1418.21.01.28081%40koobera.math.uic.edu
        AddressList.parse("\":sysmail\"@  Some-Group.\t         Some-Org, Muhammed.(I am  the greatest) Ali @(the)Vegas.WBA");
        AddressList.parse("me@home.com (comment (nested (deeply\\))))");
        AddressList.parse("mailing list: me@home.com, route two <you@work.com>, them@play.com ;");

    }

    public void testLexicalError() {
        // ensure that TokenMgrError doesn't get thrown
        try {
            AddressList.parse(")");
            fail("Expected parsing error");
        }
        catch (ParseException e) {

        }
    }
    
    public void testNullConstructorAndBadUsage() {
        AddressList al = new AddressList(null, false);
        assertEquals(0, al.size());
        
        try {
            al.get(-1);
            fail("Expected index out of bound exception!");
        } catch (IndexOutOfBoundsException e) {
        }
        
        try {
            al.get(0);
            fail("Expected index out of bound exception!");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    
    public void testAddressList() throws ParseException {
        AddressList addlist = AddressList.parse("foo@example.com, bar@example.com, third@example.com");
        ArrayList al = new ArrayList();
        al.add(addlist.get(0));

        // shared arraylist
        AddressList dl = new AddressList(al, true);
        assertEquals(1, dl.size());
        al.add(addlist.get(1));
        assertEquals(2, dl.size());
        
        // cloned arraylist
        AddressList dlcopy = new AddressList(al, false);
        assertEquals(2, dlcopy.size());
        al.add(addlist.get(2));
        assertEquals(2, dlcopy.size());
        
        // check route string
        assertEquals(2, dlcopy.flatten().size());
    }

    public void testInteractiveMain() throws Exception {
        PrintStream out_orig = System.out;
        InputStream in_orig = System.in;
        PrintStream err_orig = System.err;
        try {
            PipedOutputStream piped = new PipedOutputStream();
            PipedInputStream newInput = new PipedInputStream(piped);
            
            PipedInputStream inOut = new PipedInputStream();
            PrintStream outPs = new PrintStream(new PipedOutputStream(inOut));
            BufferedReader outReader = new BufferedReader(new InputStreamReader(inOut));
            PipedInputStream inErr = new PipedInputStream();
            PrintStream errPs = new PrintStream(new PipedOutputStream(inErr));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(inErr));
            
            
            System.setIn(newInput);
            System.setOut(outPs);
            System.setErr(errPs);
            Thread t = new Thread() {
                public void run() {
                    try {
                        AddressList.main(null);
                    } catch (Exception e) {
                        fail("Catched an exception in main: "+e);
                    }
                }
            };
            t.start();
            
            PrintWriter input = new PrintWriter(piped);
            
            input.write("Test <test@example.com>\r\n");
            input.flush();

            String out = outReader.readLine();
            assertEquals("> Test <test@example.com>", out);

            input.write("A <foo@example.com>\r\n");
            input.flush();
            
            String out2 = outReader.readLine();
            assertEquals("> A <foo@example.com>", out2);

            input.write("\"Foo Bar\" <foo>\r\n");
            input.flush();
            
            String out3 = errReader.readLine();
            assertNotNull(out3);

            input.write("quit\r\n");
            input.flush();
            
            // we read 2 angular brackets because one was from the previous exception error.
            String out4 = outReader.readLine();
            assertEquals("> > Goodbye.", out4);

            t.join();
        } finally {
            System.setIn(in_orig);
            System.setOut(out_orig);
            System.setErr(err_orig);
        }
    }

    public void testEmptyDomainList() {
        DomainList dl = new DomainList(null, false);
        assertEquals(0, dl.size());
        
        try {
            dl.get(-1);
            fail("Expected index out of bound exception!");
        } catch (IndexOutOfBoundsException e) {
        }
        
        try {
            dl.get(0);
            fail("Expected index out of bound exception!");
        } catch (IndexOutOfBoundsException e) {
        }
    }
    
    public void testDomainList() {
        ArrayList al = new ArrayList();
        al.add("example.com");

        // shared arraylist
        DomainList dl = new DomainList(al, true);
        assertEquals(1, dl.size());
        al.add("foo.example.com");
        assertEquals(2, dl.size());
        
        // cloned arraylist
        DomainList dlcopy = new DomainList(al, false);
        assertEquals(2, dlcopy.size());
        al.add("bar.example.com");
        assertEquals(2, dlcopy.size());
        
        // check route string
        assertEquals("@example.com,@foo.example.com", dlcopy.toRouteString());
    }
    

    public void testEmptyMailboxList() {
        MailboxList ml = new MailboxList(null, false);
        assertEquals(0, ml.size());
        
        try {
            ml.get(-1);
            fail("Expected index out of bound exception!");
        } catch (IndexOutOfBoundsException e) {
        }
        
        try {
            ml.get(0);
            fail("Expected index out of bound exception!");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testMailboxList() {
        ArrayList al = new ArrayList();
        al.add(new Mailbox("local","example.com"));

        // shared arraylist
        MailboxList ml = new MailboxList(al, true);
        assertEquals(1, ml.size());
        al.add(new Mailbox("local2", "foo.example.com"));
        assertEquals(2, ml.size());
        
        // cloned arraylist
        MailboxList mlcopy = new MailboxList(al, false);
        assertEquals(2, mlcopy.size());
        al.add(new Mailbox("local3", "bar.example.com"));
        assertEquals(2, mlcopy.size());
        
        mlcopy.print();
    }
    
    public void testGroupSerialization() {
        ArrayList al = new ArrayList();
        al.add(new Mailbox("test", "example.com"));
        al.add(new NamedMailbox("Foo!", "foo", "example.com"));
        DomainList dl = new DomainList(new ArrayList(Arrays.asList(new String[] {"foo.example.com"})), true);
        NamedMailbox namedMailbox = new NamedMailbox("Foo Bar", dl, "foo2", "example.com");
        assertSame(dl, namedMailbox.getRoute());
        al.add(namedMailbox);
        Group g = new Group("group", new MailboxList(al, false));
        assertEquals("group:<test@example.com>,Foo! <foo@example.com>,Foo Bar <foo2@example.com>;", g.toString());
    }
    
    public void testEmptyQuotedStringBeforeDotAtomInLocalPart() throws Exception {
        /*
         * This used to give a StringIndexOutOfBoundsException instead of the expected
         * ParseException
         */
        try {
            AddressList.parse("\"\"bar@bar.com");
            fail("ParseException expected");
        } catch (ParseException pe) {
        }
    }
}