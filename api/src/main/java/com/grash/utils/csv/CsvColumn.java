package com.grash.utils.csv;

import java.util.function.Function;

/**
 * One exportable column.
 *
 * @param key       stable identifier the client selects by. It is part of the API and is
 *                  stored inside saved views, so renaming one breaks every view that
 *                  references it — treat it like a column name, not like a label.
 * @param header    the already-translated header text. Resolved when the registry is built,
 *                  because that is where the request locale is known.
 * @param extractor produces the cell value. Returning null is fine and prints an empty cell.
 */
public record CsvColumn<T>(String key, String header, Function<T, Object> extractor) {
}
