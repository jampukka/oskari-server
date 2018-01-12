package fi.nls.oskari.map.layer.formatters;

import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.map.geometry.ProjectionHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.wmts.domain.TileMatrixSet;
import fi.nls.oskari.wmts.domain.TileMatrixLink;
import fi.nls.oskari.wmts.domain.WMTSCapabilities;
import fi.nls.oskari.wmts.domain.WMTSCapabilitiesLayer;

import org.json.JSONArray;
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

    @Override
    public JSONObject getJSON(OskariLayer layer,
            final String lang,
            final boolean isSecure) {

        final JSONObject layerJson = getBaseJSON(layer, lang, isSecure);

        JSONHelper.putValue(layerJson, "tileMatrixSetId",
                getTileMatrixSetId(layer.getCapabilities(), layer.getSrs_name()));

        // TODO: parse tileMatrixSetData for styles and set default style name from the one where isDefault = true
        String styleName = layer.getStyle();

        if(styleName == null || styleName.isEmpty()) {
            styleName = "default";
        }
        JSONHelper.putValue(layerJson, "style", styleName);
        JSONArray styles = new JSONArray();
        // currently supporting only one style (default style)
        styles.put(createStylesJSON(styleName, styleName, null));
        JSONHelper.putValue(layerJson, "styles", styles);

        // if options have urlTemplate -> use it (treat as a REST layer)
        final String urlTemplate = JSONHelper.getStringFromJSON(layer.getOptions(), "urlTemplate", null);
        final boolean needsProxy = useProxy(layer);
        if(urlTemplate != null) {
            if(needsProxy || isBeingProxiedViaOskariServer(layerJson.optString("url"))) {
                // remove requestEncoding so we always get KVP params when proxying
                JSONObject options = layerJson.optJSONObject("options");
                options.remove("requestEncoding");
            } else {
                // setup tileURL for REST layers
                final String originalUrl = layer.getUrl();
                layer.setUrl(urlTemplate);
                JSONHelper.putValue(layerJson, "tileUrl", layer.getUrl(isSecure));
                // switch back the original url in case it's used down the line
                layer.setUrl(originalUrl);
            }
        }
        return layerJson;
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
