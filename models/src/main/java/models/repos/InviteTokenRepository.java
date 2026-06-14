package models.repos;

import helpers.sql.SqlFinder;
import models.sql.InviteToken;

import java.sql.Timestamp;
import java.util.List;

public enum InviteTokenRepository {
    INSTANCE;

    private final SqlFinder<Long, InviteToken> finder = new SqlFinder<>(InviteToken.class);

    public InviteToken byToken(String token) {
        return finder.query().where()
                .eq("token", token)
                .eq("deleted", false)
                .findOne();
    }

    public InviteToken byEmailAndCollege(String email, Long collegeId) {
        return finder.query().where()
                .eq("email", email)
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .findOne();
    }

    /**
     * Find a valid (unused, not expired) invite token.
     */
    public InviteToken findValidToken(String token) {
        return finder.query().where()
                .eq("token", token)
                .eq("used", false)
                .gt("expiresAt", new Timestamp(System.currentTimeMillis()))
                .eq("deleted", false)
                .findOne();
    }

    public List<InviteToken> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }
}
