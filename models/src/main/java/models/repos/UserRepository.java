package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import lombok.val;
import models.enums.UserType;
import models.sql.User;

import java.util.List;

public enum UserRepository {
    INSTANCE;

    private final SqlFinder<Long, User> finder = new SqlFinder<>(User.class);

    public User byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public User byEmail(String email) {
        return finder.query().where().eq("email", email).eq("deleted", false).findOne();
    }

    public User byEmailAndUserType(String email , UserType userType) {
        return finder.query().where().eq("email", email).eq("user_type" , userType.getValue()).eq("deleted", false).findOne();
    }

    public User byMobile(String mobile) {
        return finder.query().where().eq("mobile", mobile).eq("deleted", false).findOne();
    }

    public List<User> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .findList();
    }

    public List<User> byCollegeAndType(Long collegeId, UserType userType) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("userType", userType)
                .eq("deleted", false)
                .findList();
    }

    public List<User> findAll() {
        return finder.query().where().eq("deleted", false).findList();
    }

    public ExpressionList<User> where() {
        return finder.query().where().eq("deleted", false);
    }
}
