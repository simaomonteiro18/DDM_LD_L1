package pt.istec.ishopping.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PalavraPasseValida.Validador.class)
public @interface PalavraPasseValida {

    String message() default "A palavra-passe tem de ter entre 6 e 72 caracteres (os caracteres acentuados "
            + "podem contar como dois) e não pode conter apenas espaços.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<PalavraPasseValida, String> {

        private static final int TAMANHO_MINIMO = 6;
        private static final int TAMANHO_MAXIMO_BCRYPT = 72;

        @Override
        public boolean isValid(String password, ConstraintValidatorContext contexto) {
            if (password == null) {
                return true;
            }
            return !password.isBlank()
                    && password.length() >= TAMANHO_MINIMO
                    && password.getBytes(StandardCharsets.UTF_8).length <= TAMANHO_MAXIMO_BCRYPT;
        }
    }
}
