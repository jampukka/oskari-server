package fi.nls.oskari.control.admin;

import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.RestActionHandler;

public abstract class AdminOnlyRestActionHandler extends RestActionHandler {

    @Override
    public void preProcess(ActionParameters params) throws ActionDeniedException {
        if (!params.getUser().isAdmin()) {
            throw new ActionDeniedException("Admin only");
        }
    }

}
