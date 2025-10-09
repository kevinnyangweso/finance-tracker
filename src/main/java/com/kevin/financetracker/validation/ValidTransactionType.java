// validation/ValidTransactionType.java
package com.kevin.financetracker.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
@Constraint(validatedBy = TransactionTypeValidator.class)
@Documented
public @interface ValidTransactionType {
    String message() default "Invalid transaction type. Must be one of: INCOME, EXPENSE, TRANSFER";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}