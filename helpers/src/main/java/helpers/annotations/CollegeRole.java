package helpers.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CollegeRole {

    String[] request() default {};

    boolean tpoAllowed() default true;

    boolean apiRateLimit() default true;

    /**
     * The portal module this endpoint belongs to.
     * Values: "drives" | "students" | "companies" | "" (no module check)
     * Primary users always bypass this check.
     */
    String module() default "";

    /**
     * Minimum access level required for non-primary users.
     * Values: "read" | "write"
     */
    String minAccess() default "read";
}
