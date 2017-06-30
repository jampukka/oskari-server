package fi.nls.oskari.map.layer;

import java.util.Map;
import java.util.NoSuchElementException;

import org.json.JSONException;
import org.json.JSONObject;

import fi.mml.map.mapwindow.service.db.InspireThemeService;
import fi.nls.oskari.domain.map.InspireTheme;
import fi.nls.oskari.domain.map.LayerGroup;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;

public class LayerImportExport {

    private static final Logger LOG = LogFactory.getLogger(LayerImportExport.class);

    /**
     * Minimal implementation for converting OskariLayer to JSON
     * @see #parseLayer(final JSONObject json)
     * @param json
     * @return
     */
    public static JSONObject serializeLayer(final OskariLayer layer) throws JSONException {
        final JSONObject json = new JSONObject();

        json.put("type", layer.getType());
        json.put("url", layer.getUrl());
        json.put("name", layer.getName());
        json.put("locale", layer.getLocale());

        if (layer.getGroup() != null) {
            String name = getRandomValue(layer.getGroup().getNames());
            json.put("organization", name != null ? name : "");
        }

        if (layer.getInspireTheme() != null) {
            String name = getRandomValue(layer.getInspireTheme().getNames());
            json.put("inspiretheme", name != null ? name : "");
        }

        json.put("base_map", layer.isBaseMap());
        json.put("opacity", layer.getOpacity());
        json.put("style", layer.getStyle());
        json.put("minscale", layer.getMinScale());
        json.put("maxscale", layer.getMaxScale());
        json.put("legend_image", layer.getLegendImage());
        json.put("metadataid", layer.getMetadataId());
        json.put("tile_matrix_set_id", layer.getTileMatrixSetId());
        json.put("gfi_type", layer.getGfiType());
        json.put("gfi_xslt", layer.getGfiXslt());
        json.put("gfi_content", layer.getGfiContent());
        json.put("geometry", layer.getGeometry());
        json.put("realtime", layer.getRealtime());
        json.put("refresh_rate", layer.getRefreshRate());
        json.put("srs_name", layer.getSrs_name());
        json.put("version", layer.getVersion());
        json.put("params", layer.getParams());
        json.put("options", layer.getOptions());

        return json;
    }

    public static void serializeTypeSpecificInfo(final OskariLayer layer, final JSONObject json) {
        switch (layer.getType()) {
        case OskariLayer.TYPE_WFS:
            serializeWFSSpecificInfo(layer, json);
            break;
        case OskariLayer.TYPE_WMS:
            serializeWMSSpecificInfo(layer, json);
            break;
        case OskariLayer.TYPE_WMTS:
            serializeWMTSSpecificInfo(layer, json);
            break;
        }
    }

    private static void serializeWFSSpecificInfo(final OskariLayer layer, final JSONObject json) {
        // TODO
    }

    private static void serializeWMSSpecificInfo(final OskariLayer layer, final JSONObject json) {
        // TODO
    }

    private static void serializeWMTSSpecificInfo(final OskariLayer layer, final JSONObject json) {
        // TODO
    }

    /**
     * Minimal implementation for parsing layer in json format.
     * @see serializeLayer(final OskariLayer layer)
     * @param json
     * @return
     */
    public static OskariLayer deserializeLayer(final JSONObject json, 
            final InspireThemeService inspireThemeService, 
            final LayerGroupService layerGroupService) throws JSONException {
        final OskariLayer layer = new OskariLayer();

        // read mandatory values, an JSONException is thrown if these are missing
        layer.setType(json.getString("type"));
        layer.setUrl(json.getString("url"));
        layer.setName(json.getString("name"));
        final String orgName = json.getString("organization");
        final String themeName = json.getString("inspiretheme");
        layer.setLocale(json.getJSONObject("locale"));

        // read optional values
        layer.setBaseMap(json.optBoolean("base_map", layer.isBaseMap()));
        layer.setOpacity(json.optInt("opacity", layer.getOpacity()));
        layer.setStyle(json.optString("style", layer.getStyle()));
        layer.setMinScale(json.optDouble("minscale", layer.getMinScale()));
        layer.setMaxScale(json.optDouble("maxscale", layer.getMaxScale()));
        layer.setLegendImage(json.optString("legend_image", layer.getLegendImage()));
        layer.setMetadataId(json.optString("metadataid", layer.getMetadataId()));
        layer.setTileMatrixSetId(json.optString("tile_matrix_set_id", layer.getTileMatrixSetId()));
        layer.setGfiType(json.optString("gfi_type", layer.getGfiType()));
        layer.setGfiXslt(json.optString("gfi_xslt", layer.getGfiXslt()));
        layer.setGfiContent(json.optString("gfi_content", layer.getGfiContent()));
        layer.setGeometry(json.optString("geometry", layer.getGeometry()));
        layer.setRealtime(json.optBoolean("realtime", layer.getRealtime()));
        layer.setRefreshRate(json.optInt("refresh_rate", layer.getRefreshRate()));
        layer.setSrs_name(json.optString("srs_name", layer.getSrs_name()));
        layer.setVersion(json.optString("version", layer.getVersion()));
        // omit permissions, these are handled by LayerHelper

        // handle params, check for null to avoid overwriting empty JS Object Literal
        final JSONObject params = json.optJSONObject("params");
        if (params != null) {
            layer.setParams(params);
        }

        // handle options, check for null to avoid overwriting empty JS Object Literal
        final JSONObject options = json.optJSONObject("options");
        if (options != null) {
            layer.setOptions(options);
        }

        // handle inspiretheme
        final InspireTheme theme = inspireThemeService.findByName(themeName);
        if (theme == null) {
            LOG.warn("Didn't find match for theme:", themeName);
        } else {
            layer.addInspireTheme(theme);
        }

        // setup data producer/layergroup
        final LayerGroup group = layerGroupService.findByName(orgName);
        if(group == null) {
            LOG.warn("Didn't find match for layergroup:", orgName);
        } else {
            layer.addGroup(group);
        }

        return layer;
    }

    public static void deserializeTypeSpecificInfo(final OskariLayer layer, final JSONObject json) {
        switch (layer.getType()) {
        case OskariLayer.TYPE_WFS:
            deserializeWFSSpecificInfo(layer, json);
            break;
        case OskariLayer.TYPE_WMS:
            deserializeWMSSpecificInfo(layer, json);
            break;
        case OskariLayer.TYPE_WMTS:
            deserializeWMTSSpecificInfo(layer, json);
            break;
        }
    }

    private static void deserializeWFSSpecificInfo(final OskariLayer layer, final JSONObject json) {
        // TODO
    }

    private static void deserializeWMSSpecificInfo(final OskariLayer layer, final JSONObject json) {
        // TODO
    }

    private static void deserializeWMTSSpecificInfo(final OskariLayer layer, final JSONObject json) {
        // TODO
    }

    /**
     * Get the first value of a map
     * The value returned depends on the implementation of the map
     * @param map
     * @return one value, null if map is null or if it contains no values
     */
    private static <K,V> V getRandomValue(Map<K, V> map) {
        if (map != null) {
            try {
                return map.values().iterator().next();
            } catch (NullPointerException | NoSuchElementException ignore) {}
        }
        return null;
    }

}
