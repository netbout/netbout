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
package com.netbout.db;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Utc;
import com.jcabi.jdbc.VoidHandler;
import com.jcabi.urn.URN;
import com.netbout.spi.cpa.Farm;
import com.netbout.spi.cpa.Operation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Manipulations with bout participants.
 *
 * @author Yegor Bugayenko (yegor@netbout.com)
 * @version $Id$
 */
@Farm
public final class ParticipantFarm {

    /**
     * Can this person be invited to the bout?
     *
     * <p>We can't accept names longer than 250 chars, because this is the
     * length of NAME field in IDENTITY table. We don't return TRUE here
     * allowing other helpers to dispatch this event.
     *
     * @param bout The bout
     * @param identity The name of the person
     * @return Should it be rejected (FALSE) or it can be (NULL)
     */
    @Operation("can-be-invited")
    public Boolean canBeInvited(final Long bout, final URN identity) {
        Boolean can = null;
        // @checkstyle MagicNumber (1 line)
        if (identity.toString().length() > 250) {
            can = Boolean.FALSE;
        }
        return can;
    }

    /**
     * Get list of names of bout participants.
     * @param bout The number of the bout
     * @return List of names
     * @throws SQLException If fails
     */
    @Operation("get-bout-participants")
    public List<URN> getBoutParticipants(final Long bout) throws SQLException {
        return new JdbcSession(Database.source())
            // @checkstyle LineLength (1 line)
            .sql("SELECT identity FROM participant JOIN bout ON bout.number = participant.bout WHERE bout = ?")
            .set(bout)
            .select(
                new JdbcSession.Handler<List<URN>>() {
                    @Override
                    public List<URN> handle(final ResultSet rset)
                        throws SQLException {
                        final List<URN> names = new LinkedList<URN>();
                        while (rset.next()) {
                            names.add(URN.create(rset.getString(1)));
                        }
                        return names;
                    }
                }
            );
    }

    /**
     * Added new participant to the bout.
     * @param bout The bout
     * @param identity The name of the person
     * @throws SQLException If fails
     */
    @Operation("added-bout-participant")
    public void addedBoutParticipant(final Long bout, final URN identity)
        throws SQLException {
        new JdbcSession(Database.source())
            // @checkstyle LineLength (1 line)
            .sql("INSERT INTO participant (bout, identity, date) VALUES (?, ?, ?)")
            .set(bout)
            .set(identity)
            .set(new Utc())
            .insert(new VoidHandler());
    }

    /**
     * Removed participant of the bout.
     * @param bout The bout
     * @param identity The name of the person
     * @throws SQLException If fails
     */
    @Operation("removed-bout-participant")
    public void removedBoutParticipant(final Long bout, final URN identity)
        throws SQLException {
        new JdbcSession(Database.source())
            .sql("DELETE FROM participant WHERE bout = ? AND identity = ?")
            .set(bout)
            .set(identity)
            .update();
    }

    /**
     * Get participant status.
     * @param bout The number of the bout
     * @param identity The participant
     * @return Status of the participant
     * @throws SQLException If fails
     */
    @Operation("get-participant-status")
    public Boolean getParticipantStatus(final Long bout, final URN identity)
        throws SQLException {
        return new JdbcSession(Database.source())
            // @checkstyle LineLength (1 line)
            .sql("SELECT confirmed FROM participant WHERE bout = ? AND identity = ?")
            .set(bout)
            .set(identity)
            .select(new BooleanHandler());
    }

    /**
     * Changed participant status.
     * @param bout The number of the bout
     * @param identity The participant
     * @param status The status to set
     * @throws SQLException If fails
     */
    @Operation("changed-participant-status")
    public void changedParticipantStatus(final Long bout,
        final URN identity, final Boolean status) throws SQLException {
        new JdbcSession(Database.source())
            // @checkstyle LineLength (1 line)
            .sql("UPDATE participant SET confirmed = ? WHERE bout = ? AND identity = ?")
            .set(status)
            .set(bout)
            .set(identity)
            .update();
    }

    /**
     * Get participant leadership status.
     * @param bout The number of the bout
     * @param identity The participant
     * @return Status of the participant
     * @throws SQLException If fails
     */
    @Operation("get-participant-leadership")
    public Boolean getParticipantLeadership(final Long bout,
        final URN identity) throws SQLException {
        return new JdbcSession(Database.source())
            // @checkstyle LineLength (1 line)
            .sql("SELECT leader FROM participant WHERE bout = ? AND identity = ?")
            .set(bout)
            .set(identity)
            .select(new BooleanHandler());
    }

    /**
     * Changed participant leadership.
     * @param bout The number of the bout
     * @param identity The participant
     * @param status The status to set
     * @throws SQLException If fails
     */
    @Operation("changed-participant-leadership")
    public void changedParticipantLeadership(final Long bout,
        final URN identity, final Boolean status) throws SQLException {
        new JdbcSession(Database.source())
            // @checkstyle LineLength (1 line)
            .sql("UPDATE participant SET leader = ? WHERE bout = ? AND identity = ?")
            .set(status)
            .set(bout)
            .set(identity)
            .update();
    }

    private static final class BooleanHandler
        implements JdbcSession.Handler<Boolean> {
        @Override
        public Boolean handle(final ResultSet rset)
            throws SQLException {
            if (!rset.next()) {
                throw new IllegalArgumentException(
                    "participant not found in bout"
                );
            }
            return rset.getBoolean(1);
        }
    }

}