/**
 * Copyright (c) 2009-2015, netbout.com
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
package com.netbout.email;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.jcabi.email.Postman;
import com.netbout.spi.Bout;
import com.netbout.spi.Inbox;
import com.netbout.spi.Pageable;
import java.io.IOException;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Email Inbox.
 *
 * @author Yegor Bugayenko (yegor@teamed.io)
 * @version $Id$
 * @since 2.12
 */
@Immutable
@Loggable(Loggable.DEBUG)
@ToString(of = "origin")
@EqualsAndHashCode(of = "origin")
final class EmInbox implements Inbox {

    /**
     * Original.
     */
    private final transient Inbox origin;

    /**
     * Alias of myself.
     */
    private final transient String self;

    /**
     * Postman.
     */
    private final transient Postman postman;

    /**
     * Public ctor.
     * @param org Origin
     * @param slf Self alias
     * @param pst Postman
     */
    EmInbox(final Inbox org, final String slf, final Postman pst) {
        this.origin = org;
        this.self = slf;
        this.postman = pst;
    }

    @Override
    public long start() throws IOException {
        return this.origin.start();
    }

    @Override
    public long unread() throws IOException {
        return this.origin.unread();
    }

    @Override
    @Loggable(
        value = Loggable.DEBUG,
        ignore = Inbox.BoutNotFoundException.class
    )
    public Bout bout(final long number) throws Inbox.BoutNotFoundException {
        return new EmBout(this.origin.bout(number), this.self, this.postman);
    }

    @Override
    public Pageable<Bout> jump(final long number) throws IOException {
        return new EmPageable<>(
            this.origin.jump(number),
            this.self,
            this.postman
        );
    }

    @Override
    public Iterable<Bout> iterate() throws IOException {
        return Iterables.transform(
            this.origin.iterate(),
            new Function<Bout, Bout>() {
                @Override
                public Bout apply(final Bout input) {
                    return new EmBout(
                        input,
                        EmInbox.this.self,
                        EmInbox.this.postman
                    );
                }
            }
        );
    }
}
