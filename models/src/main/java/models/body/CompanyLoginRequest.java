package models.body;

import helpers.utils.Request;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.sql.Company;
import models.sql.User;

@Data
public class CompanyLoginRequest {

    private Request request;
    private RoutingContext routingContext;
    private User user;
    private Company company;
    private String ip;
    private String userAgent;
    private String referer;
    private String host;
}
