package com.showroom.controller;

import com.showroom.exception.BadRequestException;

import java.util.Arrays;

/** Parses status strings from request bodies with a readable 400 on bad input. */
final class EnumParser {

    private EnumParser() {
    }

    static <E extends Enum<E>> E parse(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status '" + value + "'. Allowed: "
                    + Arrays.toString(type.getEnumConstants()));
        }
    }
}
