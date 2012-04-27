/**
 * Copyright (c) 2009-2012, Netbout.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are PROHIBITED without prior written permission from
 * the author. This product may NOT be used anywhere and on any computer
 * except the server platform of netBout Inc. located at www.netbout.com.
 * Federal copyright law prohibits unauthorized reproduction by any means
 * and imposes fines up to $25,000 for violation. If you received
 * this code occasionally and without intent to use it, please report this
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
package com.netbout.inf.ray;

import com.netbout.inf.Cursor;
import com.netbout.inf.Msg;
import com.netbout.inf.Ray;
import com.netbout.inf.TermBuilder;
import com.ymock.util.Logger;
import java.io.File;
import java.io.IOException;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * In-memory implementation of {@link Ray}.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
public final class MemRay implements Ray {

    /**
     * List of messages.
     */
    private final transient SortedSet<Msg> messages =
        new ConcurrentSkipListSet<Msg>();

    /**
     * Public ctor.
     * @param dir The directory to work with
     */
    public MemRay(final File dir) throws IOException {
        // load them from file
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        Logger.info(this, "#close(): closed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor cursor() {
        return new MemCursor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Msg msg(final long number) {
        Msg msg = new Msg() {
            @Override
            public long number() {
                return number;
            }
            @Override
            public void add(String name, String value) {
                throw new UnsupportedOperationException();
            }
            @Override
            public void replace(String name, String value) {
                throw new UnsupportedOperationException();
            }
            @Override
            public String first(String name) {
                throw new UnsupportedOperationException();
            }
            @Override
            public void delete(String name) {
                throw new UnsupportedOperationException();
            }
            @Override
            public void delete(String name, String value) {
                throw new UnsupportedOperationException();
            }
        };
        if (!this.messages.contains(msg)) {
            this.messages.add(new MemMsg());
        }
        return this.messages.tailSet(msg).first();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TermBuilder builder() {
        return new MemTermBuilder();
    }

}
