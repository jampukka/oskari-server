package fi.nls.oskari.control.admin;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceIbatisImpl;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.RequestHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("Layers")
public class LayersHandler extends AdminOnlyRestActionHandler {

    private static final Logger LOG = LogFactory.getLogger(LayersHandler.class);

    private final OskariLayerService service;

    public LayersHandler() {
        this(new OskariLayerServiceIbatisImpl());
    }

    public LayersHandler(OskariLayerService service) {
        this.service = service;
    }

    @Override
    public void handleGet(ActionParameters params) throws ActionException {
        String id = params.getRequiredParam("id");
        OskariLayer layer = service.find(id);
        if (layer == null) {
            LOG.info("Could not find layer with id: ", id);
            throw new ActionException("Layer not found!");
        }

        try {
            JSONObject json = toJSON(layer);
            ResponseHelper.writeResponse(params, HttpServletResponse.SC_OK, json);
        } catch (JSONException e) {
            LOG.warn(e);
            ResponseHelper.writeError(params, "Server error!");
        }
    }

    @Override
    public void handlePost(ActionParameters params) throws ActionException {
        HttpServletRequest req = params.getRequest();
        String contentType = req.getContentType();
        if (contentType == null || !contentType.startsWith(IOHelper.CONTENT_TYPE_JSON)) {
            throw new ActionException("Expected JSON input!");
        }

        byte[] body = RequestHelper.readRequestBody(req);
        if (body == null || body.length == 0) {
            throw new ActionException("Failed to read request!");
        }

        final OskariLayer layer = parseLayer(body);
        if (layer == null) {
            throw new ActionException("Failed to parse Layer from request!");
        }

        int id = service.insert(layer);
        if (id == -1 || id != layer.getId()) {
            throw new ActionException("Failed to insert Layer!");
        }

        try {
            JSONObject json = createResponse(layer);
            ResponseHelper.writeResponse(params, HttpServletResponse.SC_CREATED, json);
        } catch (JSONException e) {
            LOG.warn(e);
            ResponseHelper.writeError(params, "Server error!");
        }
    }

    protected OskariLayer parseLayer(byte[] body) {
        try {
            String jsonString = new String(body, StandardCharsets.UTF_8);
            JSONObject layerJSON = new JSONObject(jsonString);
            return fromJSON(layerJSON);
        } catch (JSONException e) {
            LOG.warn(e);
            return null;
        }
    }

    public static JSONObject toJSON(OskariLayer layer) throws JSONException {
        return null;
    }

    public static OskariLayer fromJSON(JSONObject layerJSON) throws JSONException, IllegalArgumentException {
        return null;
    }

    private static JSONObject createResponse(OskariLayer layer) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", layer.getId());
        json.put("url", layer.getUrl());
        return json;
    }

}
