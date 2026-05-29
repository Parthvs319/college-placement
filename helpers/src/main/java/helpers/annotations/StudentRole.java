package helpers.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface StudentRole {

    String[] request() default {};

    boolean requireVerified() default true;

    boolean apiRateLimit() default true;
}
