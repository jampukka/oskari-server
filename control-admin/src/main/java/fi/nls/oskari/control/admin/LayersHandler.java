package fi.nls.oskari.control.admin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
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
import fi.nls.oskari.map.layer.formatters.LayerJSONFormatter;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.RequestHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("Layers")
public class LayersHandler extends AdminOnlyRestActionHandler {

    private static final Logger LOG = LogFactory.getLogger(LayersHandler.class);
    private static final LayerJSONFormatter FORMATTER = new LayerJSONFormatter();

    private final OskariLayerService service;

    public LayersHandler() {
        this(new OskariLayerServiceIbatisImpl());
    }

    public LayersHandler(OskariLayerService service) {
        this.service = service;
    }

    @Override
    public void handleGet(ActionParameters params) throws ActionException {
        List<OskariLayer> layers = getLayers(params.getHttpParam("id"));
        JSONArray array = new JSONArray();
        for (OskariLayer layer : layers) {
            array.put(toJSON(layer));
        }
        ResponseHelper.writeResponse(params, HttpServletResponse.SC_OK, array);
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

        final List<OskariLayer> layers = parseLayers(body);
        insertLayers(layers);

        try {
            JSONArray response = createResponse(layers);
            ResponseHelper.writeResponse(params, HttpServletResponse.SC_CREATED, response);
        } catch (JSONException e) {
            LOG.warn(e);
            throw new ActionException("Failed to create response JSON");
        }
    }

    private void insertLayers(List<OskariLayer> layers) throws ActionException {
        int[] ids = service.insertAll(layers);
        if (!checkIdsMatch(ids, layers)) {
            throw new ActionException("Failed to insert layers!");
        }
    }

    private boolean checkIdsMatch(int[] ids, List<OskariLayer> layers) {
        if (ids == null) {
            return false;
        }
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] != layers.get(i).getId()) {
                return false;
            }
        }
        return true;
    }

    protected List<OskariLayer> getLayers(String id) {
        if (id == null || id.length() == 0) {
            // &id= missing or empty => return all
            return service.findAll();
        } else if (id.indexOf(',') >= 0) {
            // Comma separated list of ids
            return service.find(Arrays.asList(id.split(",")), null);
        } else {
            // Single id
            List<OskariLayer> layers = new ArrayList<>(1);
            OskariLayer layer = service.find(id);
            if (layer != null) {
                layers.add(layer);
            }
            return layers;
        }
    }

    protected List<OskariLayer> parseLayers(byte[] body) throws ActionException {
        try {
            List<OskariLayer> layers = new ArrayList<>();
            String jsonString = new String(body, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                Object obj = array.get(i);
                if (obj instanceof JSONObject) {
                    layers.add(FORMATTER.parseLayer((JSONObject) obj));
                } else {
                    throw new ActionException("Invalid input! Array item " + i + " not an object!");
                }
            }
            return layers;
        } catch (JSONException e) {
            LOG.warn(e);
            throw new ActionException("Invalid input! " + e.getMessage());
        }
    }

    public static JSONObject toJSON(OskariLayer layer) throws ActionException {
        return FORMATTER.getJSON(layer, layer.getLanguages().get(0), false);
    }

    public static OskariLayer fromJSON(JSONObject layerJSON) throws ActionException, JSONException {
        return FORMATTER.parseLayer(layerJSON);
    }

    private static JSONArray createResponse(List<OskariLayer> layers) throws JSONException {
        JSONArray array = new JSONArray();
        for (OskariLayer layer : layers) {
            JSONObject json = new JSONObject();
            json.put("id", layer.getId());
            json.put("url", layer.getUrl());
            array.put(json);
        }
        return array;
    }

}
