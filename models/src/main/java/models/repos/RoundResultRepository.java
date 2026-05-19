package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.enums.Status;
import models.sql.RoundResult;

import java.util.List;

public enum RoundResultRepository {
    INSTANCE;

    private final SqlFinder<Long, RoundResult> finder = new SqlFinder<>(RoundResult.class);

    public RoundResult byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public RoundResult byRoundAndStudent(Long roundId, Long studentId) {
        return finder.query().where()
                .eq("round.id", roundId)
                .eq("student.id", studentId)
                .eq("deleted", false)
                .findOne();
    }

    public List<RoundResult> byRound(Long roundId) {
        return finder.query().where()
                .eq("round.id", roundId)
                .eq("deleted", false)
                .findList();
    }

    public List<RoundResult> byRoundAndStatus(Long roundId, Status status) {
        return finder.query().where()
                .eq("round.id", roundId)
                .eq("status", status)
                .eq("deleted", false)
                .findList();
    }

    public List<RoundResult> byStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    /** Students who cleared a round (APPROVED) — feed into the next round */
    public List<RoundResult> clearedByRound(Long roundId) {
        return finder.query().where()
                .eq("round.id", roundId)
                .eq("status", Status.APPROVED)
                .eq("deleted", false)
                .findList();
    }

    public ExpressionList<RoundResult> where() {
        return finder.query().where().eq("deleted", false);
    }
}
