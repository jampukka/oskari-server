package fi.nls.oskari.control.statistics;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.nls.oskari.control.statistics.db.RegionSet;
import fi.nls.oskari.control.statistics.xml.Region;
import fi.nls.oskari.control.statistics.xml.WfsXmlParser;
import fi.nls.oskari.service.OskariComponent;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.IOHelper;

public abstract class RegionSetService extends OskariComponent {

    private static final String RESOURCES_URL_PREFIX = "resources://";

    public abstract List<RegionSet> getRegionSets();
    public abstract RegionSet getRegionSet(long id);

    public List<Region> getRegions(RegionSet regionset, String requestedSRS)
            throws IOException, ServiceException {
        String url = regionset.getFeaturesUrl();
        if (url.startsWith(RESOURCES_URL_PREFIX)) {
            return getRegionsResources(regionset, requestedSRS);
        } else {
            return getRegionsWFS(regionset, requestedSRS);
        }
    }

    private List<Region> getRegionsResources(RegionSet regionset, String requestedSRS) {
        // TODO: implement
        return null;
    }

    private List<Region> getRegionsWFS(RegionSet regionset, String requestedSRS)
            throws ServiceException, IOException {
        final String propId = regionset.getIdProperty();
        final String propName = regionset.getNameProperty();

        // For example: http://localhost:8080/geoserver/wfs?service=wfs&version=1.1.0&request=GetFeature&typeNames=oskari:kunnat2013
        Map<String, String> params = new HashMap<>();
        params.put("service", "wfs");
        params.put("version", "1.1.0");
        params.put("request", "GetFeature");
        params.put("typeName", regionset.getName());
        params.put("srsName", requestedSRS);

        final String url = IOHelper.constructUrl(regionset.getFeaturesUrl(), params);
        final HttpURLConnection connection = IOHelper.getConnection(url);
        return WfsXmlParser.parse(connection.getInputStream(), propId, propName);
    }

}
