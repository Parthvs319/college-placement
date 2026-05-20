package drive;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

/**
 * Drive module is now empty — all controllers have been moved to their portal modules:
 *   - College portal: drive CRUD, rounds, results, offers (TPO/Admin operations)
 *   - Student portal: browse drives, apply (student self-service)
 *   - Company portal: view company drives (HR view)
 *
 * This module can be removed. Drive models and repositories remain in the models module.
 */
public enum DriveRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        return Router.router(vertx);
    }
}
