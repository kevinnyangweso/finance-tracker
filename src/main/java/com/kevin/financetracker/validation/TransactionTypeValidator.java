// validation/TransactionTypeValidator.java
package com.kevin.financetracker.validation;

import com.kevin.financetracker.model.TransactionType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class TransactionTypeValidator implements ConstraintValidator<ValidTransactionType, TransactionType> {

    private Set<TransactionType> allowedTypes;

    @Override
    public void initialize(ValidTransactionType constraintAnnotation) {
        // Initialize with all enum values
        this.allowedTypes = Arrays.stream(TransactionType.values())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(TransactionType value, ConstraintValidatorContext context) {
        // Null values are considered valid (use @NotNull for null checks)
        if (value == null) {
            return true;
        }

        return allowedTypes.contains(value);
    }
}