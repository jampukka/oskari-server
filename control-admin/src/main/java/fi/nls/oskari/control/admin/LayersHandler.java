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
import org.oskari.common.ServiceFactory;

import fi.mml.map.mapwindow.service.db.InspireThemeService;
import fi.mml.portti.service.db.permissions.PermissionsService;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.db.RoleHelper;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.LayerGroupService;
import fi.nls.oskari.map.layer.LayerImportExport;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.permission.domain.Permission;
import fi.nls.oskari.permission.domain.Resource;
import fi.nls.oskari.user.IbatisRoleService;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.RequestHelper;
import fi.nls.oskari.util.ResponseHelper;

@OskariActionRoute("Layers")
public class LayersHandler extends AdminOnlyRestActionHandler {

    private static final Logger LOG = LogFactory.getLogger(LayersHandler.class);

    private final OskariLayerService layerService;
    private final InspireThemeService inspireThemeService;
    private final LayerGroupService layerGroupService;
    private final PermissionsService permissionsService;
    private final IbatisRoleService roleService;

    public LayersHandler() {
        this(ServiceFactory.getMapLayerService(),
                ServiceFactory.getInspireThemeService(),
                ServiceFactory.getLayerGroupService(),
                ServiceFactory.getPermissionsService(),
                new IbatisRoleService());
    }

    public LayersHandler(OskariLayerService layerService,
            InspireThemeService inspireThemeService,
            LayerGroupService layerGroupService,
            PermissionsService permissionsService,
            IbatisRoleService roleService) {
        this.layerService = layerService;
        this.inspireThemeService = inspireThemeService;
        this.layerGroupService = layerGroupService;
        this.permissionsService = permissionsService;
        this.roleService = roleService;
    }

    @Override
    public void handleGet(ActionParameters params) throws ActionException {
        List<OskariLayer> layers = findLayers(params.getHttpParam("id"));
        JSONArray response = serialize(layers);
        ResponseHelper.writeResponse(params, HttpServletResponse.SC_OK, response);
    }

    @Override
    public void handlePost(ActionParameters params) throws ActionException {
        final HttpServletRequest req = params.getRequest();
        final String contentType = req.getContentType();
        if (contentType == null || !contentType.startsWith(IOHelper.CONTENT_TYPE_JSON)) {
            throw new ActionException("Expected JSON input!");
        }

        final byte[] body = RequestHelper.readRequestBody(req);
        final List<JSONObject> layerJSONs = parseJSONObjects(body);
        final List<OskariLayer> layers = deserialize(layerJSONs);
        insertLayers(layers);
        addPermissions(layers, layerJSONs);

        final JSONArray response = createResponse(layers);
        ResponseHelper.writeResponse(params, HttpServletResponse.SC_CREATED, response);
    }



    private void insertLayers(List<OskariLayer> layers) throws ActionException {
        final int count = layerService.insertAll(layers);
        if (count == -1 || count != layers.size()) {
            throw new ActionException("Failed to insert layers!");
        }
    }

    protected List<OskariLayer> findLayers(String id) {
        if (id == null || id.length() == 0) {
            // &id= missing or empty => return all
            return layerService.findAll();
        } else if (id.indexOf(',') >= 0) {
            // Comma separated list of ids
            return layerService.find(Arrays.asList(id.split(",")), null);
        } else {
            // Single id
            List<OskariLayer> layers = new ArrayList<>(1);
            OskariLayer layer = layerService.find(id);
            if (layer != null) {
                layers.add(layer);
            }
            return layers;
        }
    }

    protected List<JSONObject> parseJSONObjects(byte[] jsonBytes) throws ActionException {
        if (jsonBytes == null || jsonBytes.length == 0) {
            throw new ActionException("Missing input!");
        }
        String jsonString = new String(jsonBytes, StandardCharsets.UTF_8);
        JSONArray jsonArr;
        try {
            jsonArr = new JSONArray(jsonString);
        } catch (JSONException e) {
            throw new ActionException("Invalid input! Expected JSON Array!");
        }
        return JSONHelper.getArrayAsList(jsonArr);
    }

    protected JSONArray serialize(List<OskariLayer> layers) throws ActionException {
        try {
            JSONArray jsonArr = new JSONArray();
            for (OskariLayer layer : layers) {
                jsonArr.put(LayerImportExport.serializeLayer(layer));
            }
            return jsonArr;
        } catch (JSONException e) {
            LOG.warn(e);
            throw new ActionException("Failed to write layers as JSON!");
        }
    }

    protected List<OskariLayer> deserialize(List<JSONObject> layerJSONs) throws ActionException {
        if (layerJSONs == null) {
            return new ArrayList<OskariLayer>(0);
        }

        try {
            List<OskariLayer> layers = new ArrayList<>();
            for (JSONObject layerJSON : layerJSONs) {
                OskariLayer layer = LayerImportExport.deserializeLayer(
                        layerJSON,
                        inspireThemeService,
                        layerGroupService);
                layers.add(layer);
            }
            return layers;
        } catch (JSONException e) {
            LOG.warn(e);
            throw new ActionException("Invalid input! " + e.getMessage());
        }
    }

    protected JSONArray createResponse(List<OskariLayer> insertedLayers) throws ActionException {
        try {
            JSONArray array = new JSONArray();
            for (OskariLayer layer : insertedLayers) {
                JSONObject json = new JSONObject();
                json.put("id", layer.getId());
                json.put("url", layer.getUrl());
                array.put(json);
            }
            return array;
        } catch (JSONException e) {
            LOG.warn(e);
            throw new ActionException("Failed to create response JSON!");
        }
    }

    public void addPermissions(List<OskariLayer> layers, List<JSONObject> layerJSONs) {
        for (int i = 0; i < layerJSONs.size(); i++) {
            OskariLayer layer = layers.get(i);
            JSONObject layerJSON = layerJSONs.get(i);
            RoleHelper.savePermissions(layer, roleService,
                    layerJSON.optJSONObject("role_permissions"));
        }
    }


}
