package com.grash.model.enums;

/**
 * The list page a {@link com.grash.model.SavedView} belongs to.
 * <p>
 * Stored as a string, not as an ordinal like the older enums in this package: saved views
 * are configuration a user typed, not high-volume rows, and a readable value in the table
 * is worth more here than two bytes. It also means a constant may be inserted rather than
 * only appended — see the ordinal-storage note in CLAUDE.md for why that matters elsewhere.
 * <p>
 * A new constant needs a matching entry in
 * {@link com.grash.utils.csv.CsvColumnRegistries} before the filtered export can serve it;
 * saved views alone work without one.
 */
public enum SavedViewEntityType {
    WORK_ORDER,
    ASSET,
    LOCATION,
    PART,
    METER,
    PREVENTIVE_MAINTENANCE,
    REQUEST,
    PURCHASE_ORDER
}
