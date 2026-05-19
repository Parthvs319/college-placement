package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.DriveRound;

import java.util.List;

public enum DriveRoundRepository {
    INSTANCE;

    private final SqlFinder<Long, DriveRound> finder = new SqlFinder<>(DriveRound.class);

    public DriveRound byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<DriveRound> byDrive(Long driveId) {
        return finder.query().where()
                .eq("drive.id", driveId)
                .eq("deleted", false)
                .orderBy("roundNumber asc")
                .findList();
    }

    public DriveRound byDriveAndRoundNumber(Long driveId, int roundNumber) {
        return finder.query().where()
                .eq("drive.id", driveId)
                .eq("roundNumber", roundNumber)
                .eq("deleted", false)
                .findOne();
    }

    public List<DriveRound> pendingByDrive(Long driveId) {
        return finder.query().where()
                .eq("drive.id", driveId)
                .eq("completed", false)
                .eq("deleted", false)
                .orderBy("roundNumber asc")
                .findList();
    }

    public ExpressionList<DriveRound> where() {
        return finder.query().where().eq("deleted", false);
    }
}
