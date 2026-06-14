package com.college.placement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.sql.City;
import models.sql.States;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Public APIs for location data (no auth required).
 * GET /location/states - all states
 * GET /location/cities?stateId=X - cities for a state
 */
public enum LocationController {
    INSTANCE;

    private static final Gson gson = new GsonBuilder().create();

    // GET /location/states
    public void getStates(RoutingContext ctx) {
        try {
            List<States> states = DB.find(States.class).orderBy("name").findList();
            List<StateDto> dtos = states.stream()
                    .map(s -> new StateDto(s.getId(), s.name, s.code))
                    .collect(Collectors.toList());
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(gson.toJson(dtos));
        } catch (Exception e) {
            ctx.response().setStatusCode(500).end("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // GET /location/cities?stateId=X
    public void getCities(RoutingContext ctx) {
        try {
            String stateIdParam = ctx.request().getParam("stateId");
            if (stateIdParam == null || stateIdParam.isEmpty()) {
                ctx.response().setStatusCode(400).end("{\"error\":\"stateId is required\"}");
                return;
            }
            long stateId = Long.parseLong(stateIdParam);
            List<City> cities = DB.find(City.class).where().eq("stateId", stateId).orderBy("name").findList();
            List<CityDto> dtos = cities.stream()
                    .map(c -> new CityDto(c.getId(), c.name, c.stateId))
                    .collect(Collectors.toList());
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(gson.toJson(dtos));
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end("{\"error\":\"stateId must be a number\"}");
        } catch (Exception e) {
            ctx.response().setStatusCode(500).end("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // DTOs
    static class StateDto {
        long id;
        String name;
        String code;
        StateDto(long id, String name, String code) { this.id = id; this.name = name; this.code = code; }
    }

    static class CityDto {
        long id;
        String name;
        long stateId;
        CityDto(long id, String name, long stateId) { this.id = id; this.name = name; this.stateId = stateId; }
    }
}
