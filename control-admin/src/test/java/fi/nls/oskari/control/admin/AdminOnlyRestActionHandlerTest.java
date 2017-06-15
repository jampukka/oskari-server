package fi.nls.oskari.control.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.map.view.ViewException;
import fi.nls.test.control.JSONActionRouteTest;

public class AdminOnlyRestActionHandlerTest extends JSONActionRouteTest {

    private AdminOnlyRestActionHandler handler;

    @Before
    public void init() throws ViewException {
        handler = Mockito.mock(AdminOnlyRestActionHandler.class);
    }

    @Test
    public void whenUserIsGuestThrowsException() throws ActionException {
        ActionParameters params = new ActionParameters();
        params.setRequest(mockHttpServletRequest());
        params.setResponse(mockHttpServletResponse());
        params.setUser(getGuestUser());

        try {
            handler.handleAction(params);
            fail("ActionDeniedException should have been thrown");
        } catch (ActionDeniedException e) {
            assertEquals("Session expired", e.getMessage());
        }
    }

    @Test
    public void whenUserIsNotAdminThrowsActionException() throws ActionException {
        ActionParameters params = new ActionParameters();
        params.setRequest(mockHttpServletRequest());
        params.setResponse(mockHttpServletResponse());
        params.setUser(getNotAdminUser());
        try {
            handler.handleAction(params);
            fail("ActionDeniedException should have been thrown");
        } catch (ActionDeniedException e) {
            assertEquals("Admin only", e.getMessage());
        }
    }

}
