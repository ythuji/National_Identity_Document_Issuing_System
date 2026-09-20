package org.nidis.national_identity_document_issuing_system.validation;

import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OverEighteenValidator implements ConstraintValidator<OverEighteen, LocalDate> {

    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        if (dateOfBirth == null) {
            return true;
        }

        return dateOfBirth.isBefore(LocalDate.now().minusYears(18));
    }
}