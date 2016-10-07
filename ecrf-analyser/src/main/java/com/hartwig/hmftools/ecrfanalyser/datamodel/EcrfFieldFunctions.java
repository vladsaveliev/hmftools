package com.hartwig.hmftools.ecrfanalyser.datamodel;

import org.jetbrains.annotations.NotNull;

public final class EcrfFieldFunctions {

    private static final String BIRTH_DATE_IDENTIFIER = "BIRTHDTC";

    private EcrfFieldFunctions() {
    }

    @NotNull
    public static String resolveValue(@NotNull EcrfField field, @NotNull String ecrfValue)
            throws EcrfResolveException {
        String value;
        if (field.codeList().size() > 0 && ecrfValue.length() > 0) {
            if (isInteger(ecrfValue)) {
                value = field.codeList().get(Integer.valueOf(ecrfValue));
                if (value == null) {
                    throw new EcrfResolveException(
                            "Could not find value in dropdown for " + field.name() + ": " + ecrfValue);
                }
            } else {
                throw new EcrfResolveException(
                        "Could not convert value from list to integer for " + field.name() + ": " + ecrfValue);
            }
        } else {
            value = ecrfValue;
        }

        if (field.itemOID().contains(BIRTH_DATE_IDENTIFIER) && value.length() > 0) {
            if (value.length() < 4) {
                throw new EcrfResolveException("Could not convert " + field.name() + ": " + value);
            } else {
                value = value.substring(0, 4) + "-01-01";
            }
        }

        return value;
    }

    private static boolean isInteger(@NotNull String integerString) {
        try {
            return Integer.valueOf(integerString) != null;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
