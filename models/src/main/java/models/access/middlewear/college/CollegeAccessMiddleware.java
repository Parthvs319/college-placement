package models.access.middlewear.college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.utils.RequestItem;
import helpers.utils.RequestZipped;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.BaseMiddleware;
import models.access.middlewear.user.UserLoginMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.UserType;
import models.repos.PortalPermissionRepository;
import models.sql.College;
import models.sql.PortalPermission;
import models.sql.User;
import rx.Single;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public enum CollegeAccessMiddleware implements BaseMiddleware {
    INSTANCE;

    public Single<CollegeLoginRequest> with(
            RoutingContext rc,
            List<RequestItem> items,
            Object clz
    ) {
        Class<?> targetClass = (clz instanceof Class) ? (Class<?>) clz : clz.getClass();
        CollegeRole role = null;
        for (Annotation annotation : targetClass.getAnnotations()) {
            if (annotation instanceof CollegeRole) {
                role = (CollegeRole) annotation;
            }
        }
        if (role == null) {
            throw new RoutingError(409, "Invalid api — missing @CollegeRole");
        }

        final CollegeRole finalRole = role;

        return UserLoginMiddleware.INSTANCE.authenticationObservable(rc)
                .map(context -> {
                    User user = context.get("user");

                    if (user.userType == UserType.COLLEGE_ADMIN
                            || (finalRole.tpoAllowed() && user.userType == UserType.TPO)) {
                        // allowed user type
                    } else {
                        throw new RoutingError(403, "Access denied — college admin or TPO role required");
                    }

                    College college = user.college;
                    if (college == null) {
                        throw new RoutingError(404, "No college linked to this account");
                    }

                    if (!college.isVerified()) {
                        throw new RoutingError(403, "COLLEGE_NOT_VERIFIED");
                    }
                    if (!college.isActive()) {
                        throw new RoutingError(403, "COLLEGE_DEACTIVATED");
                    }
                    if (!user.active) {
                        throw new RoutingError(403, "USER_DEACTIVATED");
                    }

                    if (!user.isPrimary && !finalRole.module().isEmpty()) {
                        PortalPermission perm = PortalPermissionRepository.INSTANCE
                                .byUserAndCollege(user.getId(), college.getId());

                        if (perm == null) {
                            throw new RoutingError(403, "INSUFFICIENT_PERMISSIONS");
                        }

                        boolean allowed = "write".equals(finalRole.minAccess())
                                ? perm.canWrite(finalRole.module())
                                : perm.canRead(finalRole.module());

                        if (!allowed) {
                            throw new RoutingError(403, "INSUFFICIENT_PERMISSIONS");
                        }
                    }

                    List<RequestItem> cloned = new ArrayList<>(items);
                    if (finalRole.request().length > 0) {
                        for (String s : finalRole.request()) {
                            cloned.add(convertRequestParam(s));
                        }
                    }

                    RequestZipped zipped = zip(context, cloned);

                    CollegeLoginRequest req = new CollegeLoginRequest();
                    req.setRoutingContext(context);
                    req.setRequest(zipped.getRequest());
                    req.setUser(user);
                    req.setCollege(college);
                    req.setIp(context.request().remoteAddress().host());
                    req.setUserAgent(context.request().getHeader("User-Agent"));
                    req.setReferer(context.request().getHeader("Referer"));
                    req.setHost(context.request().host());
                    return req;
                });
    }
}
