package models.access.middlewear.company;

import helpers.annotations.CompanyRole;
import helpers.customErrors.RoutingError;
import helpers.utils.RequestItem;
import helpers.utils.RequestZipped;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.BaseMiddleware;
import models.access.middlewear.user.UserLoginMiddleware;
import models.body.CompanyLoginRequest;
import models.enums.UserType;
import models.sql.Company;
import models.sql.User;
import rx.Single;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public enum CompanyAccessMiddleware implements BaseMiddleware {
    INSTANCE;

    public Single<CompanyLoginRequest> with(
            RoutingContext rc,
            List<RequestItem> items,
            Object clz
    ) {
        Class<?> targetClass = (clz instanceof Class) ? (Class<?>) clz : clz.getClass();
        CompanyRole role = null;
        for (Annotation annotation : targetClass.getAnnotations()) {
            if (annotation instanceof CompanyRole) {
                role = (CompanyRole) annotation;
            }
        }
        if (role == null) {
            throw new RoutingError(409, "Invalid api — missing @CompanyRole");
        }

        final CompanyRole finalRole = role;

        return UserLoginMiddleware.INSTANCE.authenticationObservable(rc)
                .map(context -> {
                    User user = context.get("user");

                    if (user.userType != UserType.COMPANY_HR) {
                        throw new RoutingError(403, "Access denied — company HR role required");
                    }

                    List<RequestItem> cloned = new ArrayList<>(items);
                    if (finalRole.request().length > 0) {
                        for (String s : finalRole.request()) {
                            cloned.add(convertRequestParam(s));
                        }
                    }

                    RequestZipped zipped = zip(context, cloned);

                    CompanyLoginRequest req = new CompanyLoginRequest();
                    req.setRoutingContext(context);
                    req.setRequest(zipped.getRequest());
                    req.setUser(user);
                    req.setCompany(null); // company looked up by controller as needed
                    req.setIp(context.request().remoteAddress().host());
                    req.setUserAgent(context.request().getHeader("User-Agent"));
                    req.setReferer(context.request().getHeader("Referer"));
                    req.setHost(context.request().host());
                    return req;
                });
    }
}
