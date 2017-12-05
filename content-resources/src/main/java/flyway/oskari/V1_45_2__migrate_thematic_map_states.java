package flyway.oskari;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;

/**
 * Migrate conf and state in portti_view_bundle_seq for
 * statsgrid and publishedgrid bundles
 *
 * @see https://github.com/oskariorg/oskari-frontend/blob/master/bundles/statistics/statsgrid/plugin/ManageClassificationPlugin.js
 */
public class V1_45_2__migrate_thematic_map_states {

    private static final Logger LOG = LogFactory.getLogger(V1_45_2__migrate_thematic_map_states.class);

    private static final String BUNDLE_NAME_STATSGRID = "statsgrid";
    private static final String BUNDLE_NAME_PUBLISHEDGRID = "publishedgrid";

    private static final String PROP_LAYER_KUNTA = "flyway.1_45_2.layer.name.kunta";
    private static final String PROP_LAYER_ALUEHALLINTOVIRASTO = "flyway.1_45_2.layer.name.aluehallintovirasto";
    private static final String PROP_LAYER_MAAKUNTA = "flyway.1_45_2.layer.name.maakunta";
    private static final String PROP_LAYER_NUTS1 = "flyway.1_45_2.layer.name.nuts1";
    private static final String PROP_LAYER_SAIRAANHOITOPIIRI = "flyway.1_45_2.layer.name.sairaanhoitopiiri";
    private static final String PROP_LAYER_SEUTUKUNTA = "flyway.1_45_2.layer.name.seutukunta";
    private static final String PROP_LAYER_ERVA = "flyway.1_45_2.layer.name.erva";
    private static final String PROP_LAYER_ELY_KESKUS= "flyway.1_45_2.layer.name.elykeskus";

    // Old regionCategory => property that tells the name of the layer (oskari_maplayer.name)
    private static final Map<String, String> REGION_CATEGORY_TO_PROP;
    static {
        REGION_CATEGORY_TO_PROP = new HashMap<>();
        REGION_CATEGORY_TO_PROP.put("KUNTA", PROP_LAYER_KUNTA);
        REGION_CATEGORY_TO_PROP.put("ALUEHALLINTOVIRASTO", PROP_LAYER_ALUEHALLINTOVIRASTO);
        REGION_CATEGORY_TO_PROP.put("MAAKUNTA", PROP_LAYER_MAAKUNTA);
        REGION_CATEGORY_TO_PROP.put("NUTS1", PROP_LAYER_NUTS1);
        REGION_CATEGORY_TO_PROP.put("SAIRAANHOITOPIIRI", PROP_LAYER_SAIRAANHOITOPIIRI);
        REGION_CATEGORY_TO_PROP.put("SEUTUKUNTA", PROP_LAYER_SEUTUKUNTA);
        REGION_CATEGORY_TO_PROP.put("ERVA", PROP_LAYER_ERVA);
        REGION_CATEGORY_TO_PROP.put("ELY-KESKUS", PROP_LAYER_ELY_KESKUS);
    }

    class ConfigNState {
        long view_id;
        long bundle_id;
        int seqno;
        String config;
        String state;
    }

    private void foo(Connection conn) throws SQLException, JSONException {
        final boolean oldAutoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            Map<String, Integer> regionToLayerId = getRegionToLayerId(conn);
            migrate(conn, regionToLayerId, BUNDLE_NAME_STATSGRID);
            migrate(conn, regionToLayerId, BUNDLE_NAME_PUBLISHEDGRID);
            // conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    private Map<String, Integer> getRegionToLayerId(Connection conn) throws SQLException {
        Map<String, String> categoryToLayerName = getCategoryToLayerNameFromProperties();

        Map<String, Integer> categoryToLayerId = new HashMap<>();
        String sql = "SELECT id FROM oskari_maplayer WHERE type = 'statslayer' AND name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> categoryLayerName : categoryToLayerName.entrySet()) {
                String category = categoryLayerName.getKey();
                String layerName = categoryLayerName.getValue();
                ps.setString(1, layerName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("id");
                        LOG.debug("Found layerId:", id,
                                "for category:", category,
                                "from database with layerName:", layerName);
                        categoryToLayerId.put(category, id);
                    } else {
                        LOG.warn("Could not find layerId for category:", category,
                                "from database with layerName:", layerName);
                    }
                }
            }
        }
        return categoryToLayerId;
    }

    private Map<String, String> getCategoryToLayerNameFromProperties() {
        Map<String, String> categoryToLayerName = new HashMap<>();
        for (Map.Entry<String, String> categoryToProperty : REGION_CATEGORY_TO_PROP.entrySet()) {
            String category = categoryToProperty.getKey();
            String property = categoryToProperty.getValue();
            String layerName = PropertyUtil.getOptional(property);
            if (layerName == null || layerName.isEmpty()) {
                LOG.warn("Could not find layerName for category:", category,
                        "from property:", property);
            } else {
                LOG.debug("Found layerName:", layerName,
                        "for category:", category,
                        "from property:", property);
                categoryToLayerName.put(category, layerName);
            }
        }
        return categoryToLayerName;
    }

    private void migrate(Connection conn, Map<String, Integer> regionToLayerId, String bundleName)
            throws SQLException, JSONException {
        long bundleId = getBundleId(conn, bundleName);
        if (bundleId < 0) {
            LOG.warn("Could not find bundle by name:", bundleName);
            return;
        }
        List<ConfigNState> configsAndStates = getConfigsAndStates(conn, bundleId);
        for (ConfigNState configAndState : configsAndStates) {
            migrate(configAndState, regionToLayerId);
        }
        update(conn, configsAndStates);
    }

    private long getBundleId(Connection conn, String name) throws SQLException {
        String sql = "SELECT id FROM portti_bundle WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
                return -1L;
            }
        }
    }

    private List<ConfigNState> getConfigsAndStates(Connection conn, long bundleId) throws SQLException {
        String sql = "SELECT view_id, bundle_id, seqno, config, state"
                + " FROM portti_view_bundle_seq WHERE bundle_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bundleId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ConfigNState> configsAndStates = new ArrayList<>();
                while (rs.next()) {
                    ConfigNState cfg = new ConfigNState();
                    cfg.view_id = rs.getLong("view_id");
                    cfg.bundle_id = rs.getLong("bundle_id");
                    cfg.seqno = rs.getInt("seqno");
                    cfg.config = rs.getString("config");
                    cfg.state = rs.getString("state");
                    configsAndStates.add(cfg);
                }
                return configsAndStates;
            }
        }
    }

    private void migrate(ConfigNState configAndState, Map<String, Integer> regionToLayerId)
            throws JSONException {
        JSONObject config = new JSONObject(configAndState.config);
        JSONObject state = new JSONObject(configAndState.state);

        config = migrateConfig(config, state);
        state = migrateState(config, state, regionToLayerId);

        configAndState.config = config.toString();
        configAndState.state = state.toString();
    }

    private JSONObject migrateConfig(JSONObject config, JSONObject state)
            throws JSONException {
        JSONObject newConfig = new JSONObject();
        newConfig.put("vectorViewer", false);
        newConfig.put("grid", true);
        newConfig.put("legendLocation", "top right");
        newConfig.put("allowClassification", false);
        return newConfig;
    }

    private JSONObject migrateState(JSONObject config, JSONObject state,
            Map<String, Integer> regionToLayerId) throws JSONException {
        JSONObject newState = new JSONObject();

        // regionset
        String regionCategory = JSONHelper.getStringFromJSON(state, "regionCategory", "KUNTA");
        Integer regionsLayerId = regionToLayerId.get(regionCategory);
        if (regionsLayerId == null) {
            LOG.error("Could not find layerId for category:", regionCategory);
            throw new IllegalArgumentException("Could not find layerId for category:" + regionCategory);
        }
        newState.put("regionset", regionsLayerId);

        // Old thematic map supports only one indicator at a time
        // so the same classification will be used for all indicators
        JSONObject classification = migrateClassification(state);

        // Indicators
        JSONArray indicators = state.getJSONArray("indicators");
        List<JSONObject> newIndicators = migrateIndicators(indicators);
        for (JSONObject indicator : newIndicators) {
            indicator.put("classification", classification);
        }
        // Active indicator

        return newState;
    }

    private List<JSONObject> migrateIndicators(JSONArray indicators) {
        // TODO Auto-generated method stub
        return null;
    }

    private JSONObject migrateClassification(JSONObject state) throws JSONException {
        int methodId = Integer.parseInt(state.getString("methodId"));
        String method = getMethodFromMethodId(methodId);

        int numberOfClasses = 0;
        if (state.has("numberOfClasses")) {
            try {
                numberOfClasses = state.getInt("numberOfClasses");
            } catch (JSONException ignore) {}
        }

        String classificationMode = state.getString("classificationMode");

        JSONObject classification = new JSONObject();
        classification.put("method", method);
        classification.put("count", numberOfClasses);
        classification.put("mode", classificationMode);

        if (state.has("colors")) {
            JSONObject colors = state.getJSONObject("colors");
            String set = colors.getString("set");
            int index = colors.getInt("index");
            boolean flipped = false;
            if (colors.has("flipped")) {
                flipped = colors.getBoolean("flipped");
            }
            classification.put("type", set);
            classification.put("name", getColorNameFromIndex(set, index));
            classification.put("reverseColors", flipped);
        }

        return classification;
    }

    private String getMethodFromMethodId(int methodId) {
        // method : ['jenks', 'quantile', 'equal'], // , 'manual'
        switch (methodId) {
        case 1: return "jenks";
        case 2: return "quantile";
        case 3: return "equal";
        case 4: return "manual";
        default: return null;
        }
    }

    private static final String[] COLORSETS_DIV = {
            "BrBG",
            "PiYG",
            "PRGn",
            "PuOr",
            "RdBu",
            "RdGy",
            "RdYlBu",
            "RdYlGn",
            "Spectral"
    };

    private static final String[] COLORSETS_SEQ = {
            "Blues",
            "BuGn",
            "BuPu",
            "GnBu",
            "Greens",
            "Greys",
            "Oranges",
            "OrRd",
            "PuBu",
            "PuBuGn",
            "PuRd",
            "Purples",
            "RdPu",
            "Reds",
            "YlGn",
            "YlGnBu",
            "YlOrBr",
            "YlOrRd"
    };

    private static final String[] COLORSETS_QUAL = {
            "Accent",
            "Dark2",
            "Paired",
            "Pastel1",
            "Pastel2",
            "Set1",
            "Set2",
            "Set3"
    };

    private String getColorNameFromIndex(String set, int colorIndex) {
        switch (set) {
        case "div": return COLORSETS_DIV[colorIndex];
        case "seq": return COLORSETS_SEQ[colorIndex];
        case "qual": return COLORSETS_QUAL[colorIndex];
        default: return null;
        }
    }

    private void update(Connection conn, List<ConfigNState> configsAndStates) throws SQLException {
        String sql = "UPDATE portti_view_bundle_seq SET"
                + " config = ?, state = ?"
                + " WHERE view_id = ? AND bundle_id = ? AND seqno = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ConfigNState configAndState : configsAndStates) {
                ps.setString(1, configAndState.config);
                ps.setString(2, configAndState.state);
                ps.setLong(3, configAndState.view_id);
                ps.setLong(4, configAndState.bundle_id);
                ps.setInt(5, configAndState.seqno);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

}
