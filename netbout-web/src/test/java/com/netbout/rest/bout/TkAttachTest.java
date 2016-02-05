/**
 * Copyright (c) 2009-2016, netbout.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are PROHIBITED without prior written permission from
 * the author. This product may NOT be used anywhere and on any computer
 * except the server platform of netbout Inc. located at www.netbout.com.
 * Federal copyright law prohibits unauthorized reproduction by any means
 * and imposes fines up to $25,000 for violation. If you received
 * this code accidentally and without intent to use it, please report this
 * incident to the author by email.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package com.netbout.rest.bout;

import com.jcabi.urn.URN;
import com.netbout.mock.MkBase;
import com.netbout.spi.Alias;
import com.netbout.spi.Attachments;
import com.netbout.spi.Bout;
import com.netbout.spi.User;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.takes.Request;
import org.takes.facets.auth.RqWithAuth;
import org.takes.facets.forward.RsFailure;
import org.takes.facets.forward.RsForward;
import org.takes.facets.hamcrest.HmRsStatus;
import org.takes.rq.RqFake;
import org.takes.rq.RqLive;
import org.takes.rq.RqMultipart;
import org.takes.rq.RqWithHeaders;

/**
 * Test case for {@link TkAttach}.
 *
 * @author Endrigo Antonini (teamed@endrigo.com.br)
 * @version $Id$
 * @since 2.15.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class TkAttachTest {

    /**
     * HTTP POST with URL and protocol.
     */
    private static final String POST_URL = "POST /b/%d/attach HTTP/1.1";

    /**
     * TkAttach can send a message to the bout after an
     * attachment is uploaded.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void sendsMessageToBout() throws Exception {
        final MkBase base = new MkBase();
        final String urn = "urn:test:1";
        final User user = base.user(new URN(urn));
        user.aliases().add("jeff1");
        final Alias alias = user.aliases().iterate().iterator().next();
        final long number = alias.inbox().start();
        final Bout bout = alias.inbox().bout(number);
        bout.friends().invite(alias.name());
        final RqWithAuth request = new RqWithAuth(
            urn,
            new RqMultipart.Fake(
                TkAttachTest.fake(number),
                new RqWithHeaders(
                    TkAttachTest.body("non-zero"),
                    String.format(TkAttachTest.POST_URL, number),
                    //@checkstyle LineLengthCheck (1 line)
                    "Content-Disposition: form-data; name=\"file\"; filename=\"some.xml\"",
                    "Content-Type: application/xml",
                    "Content-Length: 8"
                )
            )
        );
        try {
            new FkBout(".+", new TkAttach(base)).route(request);
        } catch (final RsForward response) {
            MatcherAssert.assertThat(
                response,
                new HmRsStatus(HttpURLConnection.HTTP_SEE_OTHER)
            );
        }
        MatcherAssert.assertThat(
            bout.messages().iterate().iterator().next().text(),
            Matchers.containsString("attachment \"some.xml\"")
        );
    }

    /**
     * TkAttach can ignores wrong request.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = RsFailure.class)
    public void ignoresWrongRequest() throws Exception {
        final MkBase base = new MkBase();
        final String urn = "urn:test:3";
        final User user = base.user(new URN(urn));
        user.aliases().add("jeff3");
        final Alias alias = user.aliases().iterate().iterator().next();
        final long bout = alias.inbox().start();
        alias.inbox().bout(bout).friends().invite(alias.name());
        new FkBout(".*", new TkAttach(base)).route(
            new RqWithAuth(
                urn,
                new RqMultipart.Fake(
                    TkAttachTest.fake(bout),
                    new RqWithHeaders(
                        new RqLive(
                            new ByteArrayInputStream("content2".getBytes())
                        ),
                        String.format(TkAttachTest.POST_URL, bout),
                        //@checkstyle LineLengthCheck (1 line)
                        "Content-Disposition: form-data; name=\"file\"; filenam=\"aBaaPDF.pdf\"",
                        "Content-Type: application/pdf"
                    )
                )
            )
        );
    }

    /**
     * TkAttach can ignore wrong file without saving it.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void ignoresWrongFile() throws Exception {
        final MkBase base = new MkBase();
        final String urn = "urn:test:2";
        final User user = base.user(new URN(urn));
        user.aliases().add("jeff2");
        final Alias alias = user.aliases().iterate().iterator().next();
        final long bout = alias.inbox().start();
        alias.inbox().bout(bout).friends().invite(alias.name());
        final String file = "test.bin";
        final RqWithAuth request = new RqWithAuth(
            urn,
            new RqMultipart.Fake(
                TkAttachTest.fake(bout),
                new RqWithHeaders(
                    TkAttachTest.body(""),
                    String.format(TkAttachTest.POST_URL, bout),
                    //@checkstyle LineLengthCheck (1 line)
                    String.format("Content-Disposition: form-data; name=\"file\"; filename=\"%s\"", file),
                    "Content-Type: application/octet-stream"
                )
            )
        );
        try {
            new FkBout(".*$", new TkAttach(base)).route(request);
            Assert.fail("Expected RsFailure exception but nothing was thrown");
        } catch (final RsFailure ex) {
            MatcherAssert.assertThat(
                "file unexpectedly exists after attach failure",
                new Attachments.Search(
                    alias.inbox().bout(bout).attachments()
                ).exists(file),
                Matchers.is(false)
            );
        }
    }

    /**
     * Take attachement name from name field if value is set.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void setAttachementName() throws Exception {
        final MkBase base = new MkBase();
        final String urn = "urn:test:3";
        final User user = base.user(new URN(urn));
        user.aliases().add("jeff3");
        final Alias alias = user.aliases().iterate().iterator().next();
        final long number = alias.inbox().start();
        final Bout bout = alias.inbox().bout(number);
        bout.friends().invite(alias.name());
        final String name = "test.jpg";
        final String filename = "tt.jpg";
        final RqWithAuth request = new RqWithAuth(
            urn,
            new RqMultipart.Fake(
                TkAttachTest.fake(number),
                new RqWithHeaders(
                    TkAttachTest.body(""),
                    String.format(TkAttachTest.POST_URL, number),
                    //@checkstyle LineLengthCheck (1 line)
                    String.format("Content-Disposition: form-data; name=\"file\"; filename=\"%s\"", filename),
                    "Content-Type: application/octet-stream"
                ),
                new RqWithHeaders(
                    TkAttachTest.body(name),
                    String.format(TkAttachTest.POST_URL, number),
                    "Content-Disposition: form-data; name=\"name\""
                )
            )
        );
        try {
            new FkBout(".++", new TkAttach(base)).route(request);
        } catch (final RsForward response) {
            MatcherAssert.assertThat(
                response,
                new HmRsStatus(HttpURLConnection.HTTP_SEE_OTHER)
            );
        }
        MatcherAssert.assertThat(
            bout.messages().iterate().iterator().next().text(),
            Matchers.containsString(String.format("attachment \"%s\"", name))
        );
    }

    /**
     * Creates fake request for the provided bout number.
     * @param number Bout number
     * @return Fake request
     */
    private static RqFake fake(final long number) {
        return new RqFake(
            "POST",
            String.format("/b/%d/attach", number)
        );
    }

    /**
     * Creates request with body from given string.
     * @param body Body string
     * @return Request instance
     */
    private static Request body(final String body) {
        return new Request() {
            @Override
            public Iterable<String> head() throws IOException {
                return new LinkedList<>();
            }
            @Override
            public InputStream body() throws IOException {
                return new ByteArrayInputStream(body.getBytes());
            }
        };
    }
}
