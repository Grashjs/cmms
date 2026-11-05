package com.grash.dto.analytics.meters;

import java.util.Date;

/**
 * Date and value of a reading, without the rest of the entity: meters fed automatically can have many readings.
 */
public record ReadingPoint(Date date, double value) {
}
