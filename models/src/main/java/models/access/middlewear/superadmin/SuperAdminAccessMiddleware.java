package models.access.middlewear.superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.utils.RequestItem;
import helpers.utils.RequestZipped;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.BaseMiddleware;
import models.access.middlewear.user.UserLoginMiddleware;
import models.body.SuperAdminLoginRequest;
import models.enums.UserType;
import models.sql.User;
import rx.Single;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public enum SuperAdminAccessMiddleware implements BaseMiddleware {
    INSTANCE;

    public Single<SuperAdminLoginRequest> with(
            RoutingContext rc,
            List<RequestItem> items,
            Object clz
    ) {
        Class<?> targetClass = (clz instanceof Class) ? (Class<?>) clz : clz.getClass();
        SuperAdminRole role = null;
        for (Annotation annotation : targetClass.getAnnotations()) {
            if (annotation instanceof SuperAdminRole) {
                role = (SuperAdminRole) annotation;
            }
        }
        if (role == null) {
            throw new RoutingError(409, "Invalid api — missing @SuperAdminRole");
        }

        final SuperAdminRole finalRole = role;

        return UserLoginMiddleware.INSTANCE.authenticationObservable(rc)
                .map(context -> {
                    User user = context.get("user");

                    if (user.userType != UserType.SUPER_ADMIN) {
                        throw new RoutingError(403, "Access denied — super admin role required");
                    }

                    List<RequestItem> cloned = new ArrayList<>(items);
                    if (finalRole.request().length > 0) {
                        for (String s : finalRole.request()) {
                            cloned.add(convertRequestParam(s));
                        }
                    }

                    RequestZipped zipped = zip(context, cloned);

                    SuperAdminLoginRequest req = new SuperAdminLoginRequest();
                    req.setRoutingContext(context);
                    req.setRequest(zipped.getRequest());
                    req.setUser(user);
                    req.setIp(context.request().remoteAddress().host());
                    req.setUserAgent(context.request().getHeader("User-Agent"));
                    req.setReferer(context.request().getHeader("Referer"));
                    req.setHost(context.request().host());
                    return req;
                });
    }
}
