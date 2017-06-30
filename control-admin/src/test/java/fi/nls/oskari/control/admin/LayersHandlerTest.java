package fi.nls.oskari.control.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.oskari.common.ServiceFactory;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceMemory;
import fi.nls.oskari.map.view.ViewException;
import fi.nls.oskari.user.IbatisRoleService;
import fi.nls.test.control.JSONActionRouteTest;

public class LayersHandlerTest extends JSONActionRouteTest {

    private OskariLayerService layerService;
    private LayersHandler handler;

    @Before
    public void init() throws ViewException {
        layerService = new OskariLayerServiceMemory();
        handler = new LayersHandler(layerService, 
                ServiceFactory.getInspireThemeService(),
                ServiceFactory.getLayerGroupService(),
                ServiceFactory.getPermissionsService(),
                new IbatisRoleService());
    }

    @Test
    public void whenNoEntriesAreFoundReturnsEmptyJSONArray() throws ActionException {
        ActionParameters params = getActionParametersWithAdminUser();
        params.setRequest(mockHttpServletRequest("GET"));
        byte[] response = sendRequestAndGetResponse(params);
        assertNotNull(response);
        assertEquals("[]", new String(response));
    }

    @Test
    public void whenNoIdIsSpecifiedReturnsAllEntries() throws ActionException, JSONException {
        // Insert two entries
        // Check that both are returned when no id filter is specified
        layerService.insert(getDummy());
        layerService.insert(getDummy());

        ActionParameters params = getActionParametersWithAdminUser();
        params.setRequest(mockHttpServletRequest("GET"));
        byte[] body = sendRequestAndGetResponse(params);
        List<OskariLayer> layers = handler.deserialize(handler.parseJSONObjects(body));
        assertEquals(2, layers.size());
    }

    @Test
    public void whenIdIsSpecifiedReturnsOnlyThatEntry() throws ActionException, JSONException {
        // Insert two entries
        // Check that both are returned when no id filter is specified
        OskariLayer l1 = getDummy();
        OskariLayer l2 = getDummy();
        layerService.insert(l1);
        layerService.insert(l2);
        assertNotEquals(-1, l1.getId());
        assertNotEquals(-1, l2.getId());
        assertNotEquals(l1.getId(), l2.getId());

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", "" + l1.getId());

        ActionParameters params = getActionParametersWithAdminUser();
        params.setRequest(mockHttpServletRequest("GET"));
        byte[] response = sendRequestAndGetResponse(params);
        List<OskariLayer> layers = handler.deserialize(handler.parseJSONObjects(response));
        assertEquals(1, layers.size());
        OskariLayer returned = layers.get(0);
        assertEquals(l1, returned);
    }

    @Test
    public void whenIdIsCommaSeparatedListReturnsQueriedEntries() throws ActionException, JSONException {
        // Insert three entries
        // Make sure the ids don't match
        // Query for entries #1 and #3
        // Check that we get two entries back and none of the entries have the id of #2
        OskariLayer l1 = getDummy();
        OskariLayer l2 = getDummy();
        OskariLayer l3 = getDummy();
        layerService.insert(l1);
        layerService.insert(l2);
        layerService.insert(l3);
        assertNotEquals(-1, l1.getId());
        assertNotEquals(-1, l2.getId());
        assertNotEquals(-1, l3.getId());
        assertNotEquals(l1.getId(), l2.getId());
        assertNotEquals(l2.getId(), l3.getId());
        assertNotEquals(l1.getId(), l3.getId());

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("id", l1.getId() + "," + l3.getId());

        ActionParameters params = getActionParametersWithAdminUser();
        params.setRequest(mockHttpServletRequest("GET"));
        byte[] response = sendRequestAndGetResponse(params);
        List<OskariLayer> layers = handler.deserialize(handler.parseJSONObjects(response));
        assertEquals(2, layers.size());
        for (OskariLayer layer : layers) {
            assertNotEquals(l2.getId(), layer.getId());
        }
    }

    private ActionParameters getActionParametersWithAdminUser() {
        ActionParameters params = new ActionParameters();
        params.setUser(getAdminUser());
        return params;
    }

    private byte[] sendRequestAndGetResponse(ActionParameters params) throws ActionException {
        ByteArrayOutputStream respOut = new ByteArrayOutputStream();
        params.setResponse(mockHttpServletResponse(respOut));
        handler.handleAction(params);
        return respOut.toByteArray();
    }

    private OskariLayer getDummy() {
        OskariLayer layer = new OskariLayer();
        layer.setName("foobar");
        layer.setType(OskariLayer.TYPE_WMTS);
        layer.setBaseMap(false);
        layer.setGroupId(1);
        layer.setUrl("http://www.foo.bar/wmts");
        try {
            layer.setLocale(new JSONObject("{"
                    + "\"fi\":{\"name\":\"Foo\",\"subtitle\":\"\"},"
                    + "\"sv\":{\"name\":\"Bar\",\"subtitle\":\"\"},"
                    + "\"en\":{\"name\":\"Baz\",\"subtitle\":\"\"}}"));
        } catch (JSONException ignore) {}
        return layer;
    }

}
