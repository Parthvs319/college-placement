package models.access.middlewear.student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.utils.RequestItem;
import helpers.utils.RequestZipped;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.BaseMiddleware;
import models.access.middlewear.user.UserLoginMiddleware;
import models.body.StudentLoginRequest;
import models.enums.UserType;
import models.repos.StudentRepository;
import models.sql.Student;
import models.sql.User;
import rx.Single;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public enum StudentAccessMiddleware implements BaseMiddleware {
    INSTANCE;

    public Single<StudentLoginRequest> with(
            RoutingContext rc,
            List<RequestItem> items,
            Object clz
    ) {
        Class<?> targetClass = (clz instanceof Class) ? (Class<?>) clz : clz.getClass();
        StudentRole role = null;
        for (Annotation annotation : targetClass.getAnnotations()) {
            if (annotation instanceof StudentRole) {
                role = (StudentRole) annotation;
            }
        }
        if (role == null) {
            throw new RoutingError(409, "Invalid api — missing @StudentRole");
        }

        final StudentRole finalRole = role;

        return UserLoginMiddleware.INSTANCE.authenticationObservable(rc)
                .map(context -> {
                    User user = context.get("user");

                    if (user.userType != UserType.STUDENT) {
                        throw new RoutingError(403, "Access denied — student role required");
                    }

                    Student student = StudentRepository.INSTANCE.byUserId(user.getId());
                    if (student == null) {
                        throw new RoutingError(404, "Student profile not found. Please complete onboarding.");
                    }

                    if (finalRole.requireVerified() && !user.verified) {
                        throw new RoutingError(403, "Account not verified. Contact your college placement cell.");
                    }

                    List<RequestItem> cloned = new ArrayList<>(items);
                    if (finalRole.request().length > 0) {
                        for (String s : finalRole.request()) {
                            cloned.add(convertRequestParam(s));
                        }
                    }

                    RequestZipped zipped = zip(context, cloned);

                    StudentLoginRequest req = new StudentLoginRequest();
                    req.setRoutingContext(context);
                    req.setRequest(zipped.getRequest());
                    req.setUser(user);
                    req.setStudent(student);
                    req.setIp(context.request().remoteAddress().host());
                    req.setUserAgent(context.request().getHeader("User-Agent"));
                    req.setReferer(context.request().getHeader("Referer"));
                    req.setHost(context.request().host());
                    return req;
                });
    }
}
