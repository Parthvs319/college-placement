package helpers.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SuperAdminRole {

    String[] request() default {};

    boolean apiRateLimit() default true;
}
