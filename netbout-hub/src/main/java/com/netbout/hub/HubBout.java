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
package com.netbout.hub;

import com.jcabi.log.Logger;
import com.jcabi.urn.URN;
import com.netbout.spi.Bout;
import com.netbout.spi.Friend;
import com.netbout.spi.Identity;
import com.netbout.spi.Message;
import com.netbout.spi.Participant;
import com.netbout.spi.Query;
import com.netbout.spi.xml.DomParser;
import java.net.URL;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

/**
 * Bout (cheap resource, created on demand and immediately destroyed by GC).
 *
 * <p>The class is thread-safe.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class HubBout implements Bout {

    /**
     * The hub.
     */
    private final transient PowerHub hub;

    /**
     * The viewer.
     */
    private final transient Identity viewer;

    /**
     * The data.
     */
    private final transient BoutDt data;

    /**
     * Public ctor.
     * @param ihub The hub
     * @param idnt The viewer
     * @param dat The data
     */
    public HubBout(final PowerHub ihub, final Identity idnt, final BoutDt dat) {
        this.hub = ihub;
        this.viewer = idnt;
        this.data = dat;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Bout bout) {
        return new Bout.Smart(this).updated()
            .compareTo(new Bout.Smart(bout).updated());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object bout) {
        return bout == this || (bout instanceof Bout
            && this.number().equals(((Bout) bout).number()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.number().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long number() {
        return this.data.getNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String title() {
        return this.data.getTitle();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date date() {
        return this.data.getDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void confirm() {
        this.data.confirm(this.viewer.name());
        Logger.info(
            this,
            "Participant '%s' confirmed participation in bout #%d",
            this.viewer,
            this.number()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void leave() {
        this.data.kickOff(this.viewer.name());
        Logger.info(
            this,
            "Participant '%s' just left bout #%d",
            this.viewer,
            this.number()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rename(final String text) {
        if (!new Bout.Smart(this).participant(this.viewer).confirmed()) {
            throw new IllegalStateException(
                String.format(
                    "You '%s' can't rename bout #%d until you join",
                    this.viewer,
                    this.number()
                )
            );
        }
        // @checkstyle MagicNumber (1 line)
        if (text.length() > 150) {
            throw new com.netbout.spi.IllegalOperationException(
                "Title can't be longer than 150 symbols"
            );
        }
        this.data.setTitle(text);
        Logger.info(
            this,
            "Bout #%d was successfully renamed to '%s'",
            this.number(),
            text
        );
    }

    /**
     * {@inheritDoc}
     * @checkstyle RedundantThrows (4 lines)
     */
    @Override
    public Participant invite(final Friend friend)
        throws Bout.DuplicateInvitationException {
        if (!new Bout.Smart(this).participant(this.viewer).confirmed()) {
            throw new IllegalStateException(
                String.format(
                    "You '%s' can't invite %s until you join bout #%d",
                    this.viewer,
                    friend,
                    this.number()
                )
            );
        }
        if (this.participants().contains(friend)) {
            throw new Bout.DuplicateInvitationException(
                String.format(
                    "Identity '%s' has already been invited to bout #%d",
                    friend,
                    this.number()
                )
            );
        }
        final Boolean allowed = this.hub.make("can-be-invited")
            .inBout(this)
            .arg(this.number())
            .arg(friend.name())
            .asDefault(true)
            .exec();
        if (!allowed) {
            throw new Bout.DuplicateInvitationException(
                String.format(
                    "Identity '%s' refused to be invited to bout #%d",
                    friend,
                    this.number()
                )
            );
        }
        final ParticipantDt dude = this.data.addParticipant(friend.name());
        Logger.info(this, "'%s' invited to bout #%d", friend, this.number());
        final Boolean confirm = this.hub.make("just-invited")
            .arg(this.number())
            .arg(friend.name())
            .asDefault(false)
            .exec();
        if (confirm) {
            dude.setConfirmed(true);
        }
        return new HubParticipant(this.hub, dude, this.data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Collection<Participant> participants() {
        final Collection<ParticipantDt> dudes =
            HubBout.this.data.getParticipants();
        // @checkstyle AnonInnerLength (50 lines)
        return new AbstractCollection<Participant>() {
            @Override
            public Iterator<Participant> iterator() {
                final Iterator<ParticipantDt> iter = dudes.iterator();
                return new Iterator<Participant>() {
                    @Override
                    public Participant next() {
                        return new HubParticipant(
                            HubBout.this.hub,
                            iter.next(),
                            HubBout.this.data
                        );
                    }
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
            @Override
            public int size() {
                return dudes.size();
            }
            @Override
            public boolean contains(final Object object) {
                boolean contains = false;
                for (ParticipantDt dude : dudes) {
                    if (dude.getIdentity().equals(object.toString())) {
                        contains = true;
                        break;
                    }
                }
                return contains;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Iterable<Message> messages(final Query query) {
        Iterable<Long> msgs;
        if ("(pos 0)".equals(query)) {
            final long latest = this.data.getLatestMessage();
            if (latest == 0) {
                msgs = Collections.<Long>emptyList();
            } else {
                msgs = Arrays.<Long>asList(latest);
            }
        } else {
            try {
                msgs = this.hub.infinity().messages(
                    new Query.Textual(
                        String.format(
                            "(and (equal $bout.number %d) %s)",
                            this.number(),
                            query
                        )
                    )
                );
            } catch (com.netbout.inf.InvalidSyntaxException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
        return new LazyMessages(msgs, this);
    }

    /**
     * {@inheritDoc}
     * @checkstyle RedundantThrows (4 lines)
     */
    @Override
    public Message message(final Long num)
        throws Bout.MessageNotFoundException {
        final Message message = new HubMessage(
            this.hub,
            this.viewer,
            this.data.findMessage(num)
        );
        Logger.debug(
            this,
            "#message(#%d): found",
            num
        );
        return message;
    }

    /**
     * {@inheritDoc}
     * @checkstyle RedundantThrows (4 lines)
     * @checkstyle ExecutableStatementCount (80 lines)
     */
    @Override
    public Message post(final String text) throws Bout.MessagePostException {
        if (text.isEmpty()) {
            throw new Bout.MessagePostException(
                "some message content is required"
            );
        }
        if (!new Bout.Smart(this).participant(this.viewer).confirmed()) {
            throw new IllegalStateException(
                String.format(
                    "You '%s' can't post to bout #%d until you join",
                    this.viewer,
                    this.number()
                )
            );
        }
        this.validate(text);
        final Long duplicate = this.hub.make("pre-post-ignore-duplicate")
            .synchronously()
            .inBout(this)
            .arg(this.number())
            .arg(text)
            .asDefault(0L)
            .exec();
        Message message;
        if (duplicate == 0L) {
            final MessageDt msg = this.data.addMessage();
            msg.setDate(new Date());
            msg.setAuthor(this.viewer.name());
            msg.setText(text);
            Logger.info(this, "Message posted to bout #%d", this.number());
            message = new HubMessage(
                this.hub,
                this.viewer,
                msg
            );
            message.text();
            this.hub.make("notify-bout-participants")
                .arg(this.number())
                .arg(message.number())
                .asDefault(false)
                .exec();
        } else {
            try {
                message = this.message(duplicate);
            } catch (Bout.MessageNotFoundException ex) {
                throw new Bout.MessagePostException(
                    String.format(
                        "duplicate found at msg #%d, but it's absent",
                        duplicate
                    ),
                    ex
                );
            }
        }
        return message;
    }

    /**
     * Validate incoming text and throw exception if not valid.
     * @param text The text to validate
     * @throws Bout.MessagePostException If failed to validate
     * @checkstyle RedundantThrows (3 lines)
     */
    private void validate(final String text) throws Bout.MessagePostException {
        final DomParser parser = new DomParser(text);
        try {
            parser.validate();
        } catch (com.netbout.spi.xml.DomValidationException ex) {
            throw new Bout.MessagePostException(ex);
        }
        if (parser.isXml()) {
            URN namespace;
            try {
                namespace = parser.namespace();
            } catch (com.netbout.spi.xml.DomValidationException ex) {
                throw new Bout.MessagePostException(ex);
            }
            URL def;
            try {
                def = new URL("http://localhost");
            } catch (java.net.MalformedURLException ex) {
                throw new IllegalStateException();
            }
            final URL url = this.hub.make("resolve-xml-namespace")
                .synchronously()
                .arg(namespace.pure())
                .asDefault(def)
                .exec();
            if (url.equals(def)) {
                throw new Bout.MessagePostException(
                    String.format(
                        "Namespace '%s' is not supported by helpers",
                        namespace
                    )
                );
            }
            URL schema;
            try {
                schema = parser.schemaLocation(namespace);
            } catch (com.netbout.spi.xml.DomValidationException ex) {
                throw new Bout.MessagePostException(ex);
            }
            if (!url.equals(schema)) {
                throw new Bout.MessagePostException(
                    String.format(
                        "Schema for namespace '%s' should be '%s' (not '%s')",
                        namespace,
                        url,
                        schema
                    )
                );
            }
            final String error = this.hub.make("pre-post-validate")
                .synchronously()
                .inBout(this)
                .arg(this.number())
                .arg(this.viewer.name())
                .arg(text)
                .asDefault("")
                .exec();
            if (!error.isEmpty()) {
                throw new Bout.MessagePostException(error);
            }
        }
    }

}