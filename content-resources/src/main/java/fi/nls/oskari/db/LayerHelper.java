package fi.nls.oskari.db;

import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.LayerImportExport;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.user.IbatisRoleService;
import fi.nls.oskari.util.IOHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.oskari.common.ServiceFactory;

import java.io.IOException;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: SMAKINEN
 * Date: 27.6.2014
 * Time: 15:26
 * To change this template use File | Settings | File Templates.
 */
public class LayerHelper {

    private static final Logger log = LogFactory.getLogger(LayerHelper.class);

    public static int setupLayer(final String layerfile) throws IOException, JSONException {
        final String jsonStr = IOHelper.readString(DBHandler.getInputStreamFromResource("/json/layers/" + layerfile));
        final JSONObject json =  new JSONObject(jsonStr);
        final OskariLayer layer = LayerImportExport.deserializeLayer(json,
                ServiceFactory.getInspireThemeService(),
                ServiceFactory.getLayerGroupService());

        final OskariLayerService layerService = ServiceFactory.getMapLayerService();

        final List<OskariLayer> dbLayers = layerService.findByUrlAndName(layer.getUrl(), layer.getName());
        // Check if a layer with the same url and name already exists
        if(!dbLayers.isEmpty()) {
            if(dbLayers.size() > 1) {
                log.warn("Found multiple layers with same url and name. Using first one. Url:", layer.getUrl(), "- name:", layer.getName());
            }
            return dbLayers.get(0).getId();
        }
        // else Layer doesn't exist, insert it

        if (OskariLayer.TYPE_WFS.equals(layer.getType())) {
            // TODO: parse WFS related SLD, template_model and configuration
            // Only insert wfs layer if these are ok
        }

        layerService.insert(layer);
        RoleHelper.savePermissions(layer, new IbatisRoleService(), json.optJSONObject("role_permissions"));
        return layer.getId();
    }


}
