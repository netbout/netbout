/**
 * Copyright (c) 2009-2011, netBout.com
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
package com.netbout.db;

import com.netbout.spi.Urn;
import com.netbout.spi.UrnMocker;
import java.util.Random;

/**
 * Mocker of {@code ALIAS} row in a database.
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
public final class AliasRowMocker {

    /**
     * The identity it is related to.
     */
    private final transient Urn identity;

    /**
     * The alias.
     */
    private transient String alias;

    /**
     * Public ctor.
     * @param name The identity
     */
    public AliasRowMocker(final Urn name) {
        this.identity = name;
        this.alias = String.format(
            "Captain William Bones no.%d",
            Math.abs(new Random().nextLong())
        );
    }

    /**
     * With this name.
     * @param name The alias
     * @return This object
     */
    public AliasRowMocker namedAs(final String name) {
        this.alias = name;
        return this;
    }

    /**
     * Mock it and return its text.
     */
    public String mock() {
        final AliasFarm afarm = new AliasFarm();
        try {
            afarm.addedIdentityAlias(this.identity, this.alias);
        } catch (java.sql.SQLException ex) {
            throw new IllegalArgumentException(ex);
        }
        return this.alias;
    }

}