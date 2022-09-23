package fi.nls.oskari.search.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import fi.mml.portti.service.search.SearchResultItem;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.wfs.WFSSearchChannelsConfiguration;

public class WFSSearchChannelTest {

    private static JSONObject dinagatIslands;
    private static JSONObject pointFeature;

    @BeforeClass
    public static void setup() throws Exception {
        String dinagat = ""
                + "{"
                + "  'type': 'Feature',"
                + "  'geometry': {"
                + "    'type': 'Point',"
                + "    'coordinates': [125.6, 10.1]"
                + "  },"
                + "  'properties': {"
                + "    'name': 'Dinagat Islands',"
                + "    'region': 'Caraga'"
                + "  }"
                + "}";
        dinagat.replace('\'', '"');
        dinagatIslands = new JSONObject(dinagat);

        String point = ""
                + "{"
                + "  'type': 'Feature',"
                + "  'geometry': {"
                + "    'type': 'Point',"
                + "    'coordinates': [102.0, 0.5]"
                + "  },"
                + "  'properties': {"
                + "    'prop0': 'value0'"
                + "  }"
                + "}";
        point.replace('\'', '"');
        pointFeature = new JSONObject(point);
    }

    @Test
    public void whenConfigNotSetRegionIsNotReadFromProperty() throws Exception {
        WFSSearchChannelsConfiguration cfg = new WFSSearchChannelsConfiguration();

        WFSSearchChannel ch = new WFSSearchChannel(cfg);

        assertNull(ch.getRegion(dinagatIslands));
        assertNull(ch.getRegion(pointFeature));
    }

    @Test
    public void whenConfigIsSetRegionIsReadFromProperty() throws Exception {
        WFSSearchChannelsConfiguration cfg = new WFSSearchChannelsConfiguration();

        cfg.setConfig(JSONHelper.createJSONObject(WFSSearchChannel.CONFIG_REGION_PROPERTY, "region"));

        WFSSearchChannel ch = new WFSSearchChannel(cfg);

        assertEquals("Caraga", ch.getRegion(dinagatIslands));
        assertNull(ch.getRegion(pointFeature));
    }

    @Test
    public void whenDefaultIsSetRegionIsOverriddenFromProperty() throws Exception {
        WFSSearchChannelsConfiguration cfg = new WFSSearchChannelsConfiguration();

        JSONObject config = new JSONObject();
        config.put(WFSSearchChannel.CONFIG_REGION_PROPERTY, "region");
        config.put("defaults", JSONHelper.createJSONObject("region", "My Default Region"));
        cfg.setConfig(config);

        WFSSearchChannel ch = new WFSSearchChannel(cfg);

        SearchResultItem item;

        item = ch.parseResultItem(dinagatIslands);
        assertEquals("Caraga", item.getRegion());

        item = ch.parseResultItem(pointFeature);
        assertEquals("My Default Region", item.getRegion());
    }

}
