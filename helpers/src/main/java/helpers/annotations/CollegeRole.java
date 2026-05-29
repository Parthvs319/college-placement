package helpers.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CollegeRole {

    String[] request() default {};

    boolean tpoAllowed() default true;

    boolean apiRateLimit() default true;
}
