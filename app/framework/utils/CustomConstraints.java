package framework.utils;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;

import play.data.validation.Constraints.Validator;
import play.libs.F.Tuple;

/**
 * The custom constraints for forms.
 * 
 * @author Johann Kohler
 *
 */
public class CustomConstraints {

    /**
     * The multi languages string required constraint annotation.
     * 
     * @author Johann Kohler
     */
    @Target({ FIELD })
    @Retention(RUNTIME)
    @Constraint(validatedBy = MultiLanguagesStringRequiredValidator.class)
    @play.data.Form.Display(name = "form.input.multi_languages_string.required")
    public static @interface MultiLanguagesStringRequired {

        /**
         * The error message.
         */
        String message() default MultiLanguagesStringRequiredValidator.MESSAGE;

        /**
         * The groups.
         */
        Class<?>[]groups() default {};

        /**
         * The payload.
         */
        Class<? extends Payload>[]payload() default {};
    }

    /**
     * The multi languages string required validator.
     * 
     * @author Johann Kohler
     *
     */
    public static class MultiLanguagesStringRequiredValidator extends Validator<MultiLanguagesString>
            implements ConstraintValidator<MultiLanguagesStringRequired, MultiLanguagesString> {

        public static final String MESSAGE = "form.input.multi_languages_string.required.error";

        @Override
        public void initialize(MultiLanguagesStringRequired multiLanguagesStringRequired) {
        }

        @Override
        public Tuple<String, Object[]> getErrorMessageKey() {
            return new Tuple<String, Object[]>(MESSAGE, new Object[] {});
        }

        /**
         * The validation itself.
         * 
         * @param multiLanguagesString
         *            the multiLanguagesString to validate
         */
        @Override
        public boolean isValid(MultiLanguagesString multiLanguagesString) {

            if (multiLanguagesString == null) {
                return false;
            }

            for (String value : multiLanguagesString.getValues()) {
                if (value != null && !value.equals("")) {
                    return true;
                }
            }

            return false;
        }

    }

    /**
     * Constructs the multi languages string required validator.
     */
    public static Validator<MultiLanguagesString> multiLanguagesStringRequired() {
        return new MultiLanguagesStringRequiredValidator();
    }

    /**
     * The multi languages string max length constraint annotation.
     * 
     * @author Johann Kohler
     */
    @Target({ FIELD })
    @Retention(RUNTIME)
    @Constraint(validatedBy = MultiLanguagesStringMaxLengthValidator.class)
    @play.data.Form.Display(name = "form.input.multi_languages_string.max_length", attributes = { "value" })
    public static @interface MultiLanguagesStringMaxLength {

        /**
         * The error message.
         */
        String message() default MultiLanguagesStringMaxLengthValidator.MESSAGE;

        /**
         * The max length for each string.
         */
        int value();

        /**
         * The groups.
         */
        Class<?>[]groups() default {};

        /**
         * The payload.
         */
        Class<? extends Payload>[]payload() default {};
    }

    /**
     * The multi languages string max length validator.
     * 
     * @author Johann Kohler
     *
     */
    public static class MultiLanguagesStringMaxLengthValidator extends Validator<MultiLanguagesString>
            implements ConstraintValidator<MultiLanguagesStringMaxLength, MultiLanguagesString> {

        public static final String MESSAGE = "form.input.multi_languages_string.max_length.error";

        private int maxLength;

        /**
         * Default constructor.
         */
        public MultiLanguagesStringMaxLengthValidator() {
        }

        /**
         * Construct with a max length.
         * 
         * @param value
         *            the max length
         */
        public MultiLanguagesStringMaxLengthValidator(int value) {
            this.maxLength = value;
        }

        @Override
        public void initialize(MultiLanguagesStringMaxLength multiLanguagesStringMaxLength) {
            this.maxLength = multiLanguagesStringMaxLength.value();
        }

        @Override
        public Tuple<String, Object[]> getErrorMessageKey() {
            return new Tuple<String, Object[]>(MESSAGE, new Object[] { this.maxLength });
        }

        /**
         * The validation itself.
         * 
         * @param multiLanguagesString
         *            the multiLanguagesString to validate
         */
        @Override
        public boolean isValid(MultiLanguagesString multiLanguagesString) {

            if (multiLanguagesString == null) {
                return true;
            }

            boolean isNotTooLong = true;
            for (String value : multiLanguagesString.getValues()) {
                if (value != null && !value.equals("")) {
                    isNotTooLong = isNotTooLong && (value.length() <= this.maxLength);
                }
            }
            return isNotTooLong;
        }

    }

    /**
     * Constructs the multi languages string max length validator.
     * 
     * @param value
     *            the max length
     */
    public static Validator<MultiLanguagesString> multiLanguagesStringMaxLength(int value) {
        return new MultiLanguagesStringMaxLengthValidator(value);
    }

}
