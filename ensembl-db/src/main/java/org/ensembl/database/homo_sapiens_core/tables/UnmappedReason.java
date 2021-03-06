/*
 * This file is generated by jOOQ.
*/
package org.ensembl.database.homo_sapiens_core.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.ensembl.database.homo_sapiens_core.HomoSapiensCore_89_37;
import org.ensembl.database.homo_sapiens_core.Keys;
import org.ensembl.database.homo_sapiens_core.tables.records.UnmappedReasonRecord;
import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;
import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.5"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class UnmappedReason extends TableImpl<UnmappedReasonRecord> {

    private static final long serialVersionUID = -1884713798;

    /**
     * The reference instance of <code>homo_sapiens_core_89_37.unmapped_reason</code>
     */
    public static final UnmappedReason UNMAPPED_REASON = new UnmappedReason();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<UnmappedReasonRecord> getRecordType() {
        return UnmappedReasonRecord.class;
    }

    /**
     * The column <code>homo_sapiens_core_89_37.unmapped_reason.unmapped_reason_id</code>.
     */
    public final TableField<UnmappedReasonRecord, UInteger> UNMAPPED_REASON_ID = createField("unmapped_reason_id", org.jooq.impl.SQLDataType.INTEGERUNSIGNED.nullable(false), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.unmapped_reason.summary_description</code>.
     */
    public final TableField<UnmappedReasonRecord, String> SUMMARY_DESCRIPTION = createField("summary_description", org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * The column <code>homo_sapiens_core_89_37.unmapped_reason.full_description</code>.
     */
    public final TableField<UnmappedReasonRecord, String> FULL_DESCRIPTION = createField("full_description", org.jooq.impl.SQLDataType.VARCHAR.length(255), this, "");

    /**
     * Create a <code>homo_sapiens_core_89_37.unmapped_reason</code> table reference
     */
    public UnmappedReason() {
        this("unmapped_reason", null);
    }

    /**
     * Create an aliased <code>homo_sapiens_core_89_37.unmapped_reason</code> table reference
     */
    public UnmappedReason(String alias) {
        this(alias, UNMAPPED_REASON);
    }

    private UnmappedReason(String alias, Table<UnmappedReasonRecord> aliased) {
        this(alias, aliased, null);
    }

    private UnmappedReason(String alias, Table<UnmappedReasonRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return HomoSapiensCore_89_37.HOMO_SAPIENS_CORE_89_37;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<UnmappedReasonRecord, UInteger> getIdentity() {
        return Keys.IDENTITY_UNMAPPED_REASON;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<UnmappedReasonRecord> getPrimaryKey() {
        return Keys.KEY_UNMAPPED_REASON_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<UnmappedReasonRecord>> getKeys() {
        return Arrays.<UniqueKey<UnmappedReasonRecord>>asList(Keys.KEY_UNMAPPED_REASON_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnmappedReason as(String alias) {
        return new UnmappedReason(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public UnmappedReason rename(String name) {
        return new UnmappedReason(name, null);
    }
}
