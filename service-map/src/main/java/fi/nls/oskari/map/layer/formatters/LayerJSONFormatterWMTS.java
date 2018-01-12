package fi.nls.oskari.map.layer.formatters;

import fi.nls.oskari.map.geometry.ProjectionHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.wmts.domain.TileMatrixSet;
import fi.nls.oskari.wmts.domain.TileMatrixLink;
import fi.nls.oskari.wmts.domain.WMTSCapabilities;
import fi.nls.oskari.wmts.domain.WMTSCapabilitiesLayer;
import org.json.JSONObject;

public class LayerJSONFormatterWMTS extends LayerJSONFormatter {

    public static final String KEY_TILEMATRIXIDS = "tileMatrixIds";

    /**
     * @deprecated replaced by {@link #createCapabilitiesJSON(WMTSCapabilitiesLayer layer)}
     */
    @Deprecated
    public static JSONObject createCapabilitiesJSON(final WMTSCapabilities wmts,final WMTSCapabilitiesLayer layer) {
        return createCapabilitiesJSON(layer);
    }

    public static JSONObject createCapabilitiesJSON(WMTSCapabilitiesLayer layer) {
        JSONObject capabilities = new JSONObject();

        JSONHelper.putValue(capabilities, KEY_TILEMATRIXIDS, createTileMatrixMap(layer));

        return capabilities;
    }

    private static JSONObject createTileMatrixMap(WMTSCapabilitiesLayer layer) {
        JSONObject epsgToTileMatrixSetId = new JSONObject();
        for (TileMatrixLink link : layer.getLinks()) {
            TileMatrixSet tms = link.getTileMatrixSet();
            String crs = tms.getCrs();
            String epsg = ProjectionHelper.shortSyntaxEpsg(crs);
            JSONHelper.putValue(epsgToTileMatrixSetId, epsg, tms.getId());
        }
        return epsgToTileMatrixSetId;
    }

    /**
     * Get matrix id by current crs
     */
    public static String getTileMatrixSetId(final JSONObject capabilities, final String crs) {
        JSONObject epsgToTileMatrixSetId = JSONHelper.getJSONObject(capabilities, KEY_TILEMATRIXIDS);
        return JSONHelper.getStringFromJSON(epsgToTileMatrixSetId, crs, null);
    }

}
