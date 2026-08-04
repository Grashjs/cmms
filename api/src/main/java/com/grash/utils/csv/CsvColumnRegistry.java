package com.grash.utils.csv;

import com.grash.exception.CustomException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The exportable columns of one entity, in their default order.
 * <p>
 * Exists so that "which columns can be exported" is answered in exactly one place per
 * entity. Before this, the column list lived inline in {@code CsvFileGenerator} as an
 * {@code Arrays.asList(...)} of headers positionally matched to an argument list — correct
 * only as long as both stayed in step, and impossible to select a subset from.
 */
public class CsvColumnRegistry<T> {

    private final Map<String, CsvColumn<T>> columns = new LinkedHashMap<>();
    private final Set<String> defaultKeys = new LinkedHashSet<>();

    /**
     * A column in the default set: exported when the caller names no columns at all.
     * <p>
     * Order matters — {@link #all()} preserves insertion order, and that order is the file the
     * unfiltered export has always produced.
     */
    public CsvColumnRegistry<T> add(CsvColumn<T> column) {
        columns.put(column.key(), column);
        defaultKeys.add(column.key());
        return this;
    }

    /**
     * A column a caller must ask for by name. Kept out of the default set so that adding one
     * cannot silently widen the unfiltered export — which is a file people diff and reconcile
     * against, and which gained nine columns before this distinction existed.
     */
    public CsvColumnRegistry<T> addOptional(CsvColumn<T> column) {
        columns.put(column.key(), column);
        return this;
    }

    /** The default set, in order. */
    public List<CsvColumn<T>> all() {
        List<CsvColumn<T>> result = new ArrayList<>(defaultKeys.size());
        for (String key : defaultKeys) {
            result.add(columns.get(key));
        }
        return result;
    }

    /** Every selectable key, default and optional alike. */
    public Collection<String> keys() {
        return columns.keySet();
    }

    /**
     * Resolves a requested selection, preserving the requested order so the file matches the
     * column order the user arranged on screen. A null or empty selection means the default
     * set — i.e. an export request that says nothing about columns behaves exactly like the
     * unfiltered export did before.
     * <p>
     * An unknown key is rejected rather than skipped. Silently dropping it would produce a
     * file that looks complete and quietly lacks a column, which is the worst outcome for a
     * report someone forwards.
     */
    public List<CsvColumn<T>> resolve(List<String> requestedKeys) {
        if (requestedKeys == null || requestedKeys.isEmpty()) {
            return all();
        }
        List<CsvColumn<T>> result = new ArrayList<>(requestedKeys.size());
        for (String key : requestedKeys) {
            CsvColumn<T> column = columns.get(key);
            if (column == null) {
                throw new CustomException("Unknown export column: " + key, HttpStatus.NOT_ACCEPTABLE);
            }
            result.add(column);
        }
        return result;
    }
}
