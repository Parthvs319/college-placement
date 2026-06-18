package models.repos;


import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.City;

import java.util.List;

public enum CityRepository {

    INSTANCE;

    public SqlFinder<Long, City> citySqlFinder = new SqlFinder<>(City.class);

    public City byId(Long id) {
        return citySqlFinder.query().where().eq("id", id).findOne();
    }

    public List<City> finder() {
        return citySqlFinder.query().where().findList();
    }

    public ExpressionList<City> exprFinder() {
        return citySqlFinder.query().where();
    }

}
