/*
 * This file is generated by jOOQ.
*/
package com.hartwig.hmftools.vicc.database;


import com.hartwig.hmftools.vicc.database.tables.Viccentry;
import com.hartwig.hmftools.vicc.database.tables.records.ViccentryRecord;

import javax.annotation.Generated;

import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.AbstractKeys;


/**
 * A class modelling foreign key relationships between tables of the <code>vicc_db</code> 
 * schema
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.5"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------

    public static final Identity<ViccentryRecord, Integer> IDENTITY_VICCENTRY = Identities0.IDENTITY_VICCENTRY;

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<ViccentryRecord> KEY_VICCENTRY_PRIMARY = UniqueKeys0.KEY_VICCENTRY_PRIMARY;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Identities0 extends AbstractKeys {
        public static Identity<ViccentryRecord, Integer> IDENTITY_VICCENTRY = createIdentity(Viccentry.VICCENTRY, Viccentry.VICCENTRY.ID);
    }

    private static class UniqueKeys0 extends AbstractKeys {
        public static final UniqueKey<ViccentryRecord> KEY_VICCENTRY_PRIMARY = createUniqueKey(Viccentry.VICCENTRY, "KEY_viccEntry_PRIMARY", Viccentry.VICCENTRY.ID);
    }
}