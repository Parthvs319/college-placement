package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.SupportTicket;

import java.util.List;

public enum SupportTicketRepository {
    INSTANCE;

    private final SqlFinder<Long, SupportTicket> finder = new SqlFinder<>(SupportTicket.class);

    public SupportTicket byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<SupportTicket> all() {
        return finder.query().where().eq("deleted", false).orderBy("createdAt desc").findList();
    }

    public List<SupportTicket> byStatus(String status) {
        return finder.query().where().eq("status", status).eq("deleted", false).orderBy("createdAt desc").findList();
    }

    public List<SupportTicket> byCollege(Long collegeId) {
        return finder.query().where().eq("college.id", collegeId).eq("deleted", false).orderBy("createdAt desc").findList();
    }

    public List<SupportTicket> byPriority(String priority) {
        return finder.query().where().eq("priority", priority).eq("deleted", false).orderBy("createdAt desc").findList();
    }

    public ExpressionList<SupportTicket> where() {
        return finder.query().where().eq("deleted", false);
    }
}
