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
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
package com.netbout.rest.rexsl.setup

import com.netbout.spi.Urn
import com.netbout.spi.client.RestExpert
import com.netbout.spi.client.RestSession
import javax.ws.rs.core.UriBuilder
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers

def starter = new RestExpert(new RestSession(rexsl.home).authenticate(new Urn(), 'localhost'))
def mandatory = [
    'test' : '/mock-auth',
    'facebook': '/fb',
    'email': '/email',
]
mandatory.each {
    def url = UriBuilder.fromUri(rexsl.home).path(it.value).build().toURL()
    starter.namespaces().put(it.key, url)
    MatcherAssert.assertThat(starter.namespaces(), Matchers.hasEntry(it.key, url))
}
MatcherAssert.assertThat(
    starter.namespaces().size(),
    Matchers.not(Matchers.lessThan(mandatory.size()))
)

[
    'urn:test:dh' : 'file:com.netbout.dh',
    'urn:test:hh' : 'file:com.netbout.hub.hh',
    'urn:test:bh' : 'file:com.netbout.bus.bh',
    'urn:test:ih' : 'file:com.netbout.inf.ih',
    'urn:test:email' : 'file:com.netbout.notifiers.email',
].each {
    new RestExpert(
        new RestSession(rexsl.home).authenticate(new Urn(it.key), '')
    ).promote(new URL(it.value))
}
