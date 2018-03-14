package fi.nls.oskari.control.statistics;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

import fi.nls.oskari.control.statistics.db.RegionSet;
import fi.nls.oskari.control.statistics.xml.Region;
import fi.nls.oskari.control.statistics.xml.WfsXmlParser;
import fi.nls.oskari.service.OskariComponent;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.IOHelper;

public abstract class RegionSetService extends OskariComponent {

    private static final String RESOURCES_URL_PREFIX = "resources://";
    private static final FeatureJSON FJ = new FeatureJSON();

    public abstract List<RegionSet> getRegionSets();
    public abstract RegionSet getRegionSet(long id);

    public List<Region> getRegions(RegionSet regionset, String requestedSRS)
            throws FactoryException, MismatchedDimensionException, TransformException, ServiceException, IOException {
        SimpleFeatureCollection fc = getFeatureCollection(regionset, requestedSRS);
        final String propId = regionset.getIdProperty();
        final String propName = regionset.getNameProperty();
        return WfsXmlParser.parse(fc, propId, propName);
    }

    private SimpleFeatureCollection getFeatureCollection(RegionSet regionset, String requestedSRS)
            throws FactoryException, MismatchedDimensionException, TransformException, ServiceException, IOException {
        String url = regionset.getFeaturesUrl();
        if (url.startsWith(RESOURCES_URL_PREFIX)) {
            return getRegionsResources(regionset, requestedSRS);
        } else {
            return getRegionsWFS(regionset, requestedSRS);
        }
    }

    private SimpleFeatureCollection getRegionsResources(RegionSet regionset, String requestedSRS)
            throws IOException, MismatchedDimensionException, TransformException, FactoryException {
        MathTransform transform = findMathTransform(regionset.getSrs_name(), requestedSRS);
        String url = regionset.getFeaturesUrl();
        String path = url.substring(RESOURCES_URL_PREFIX.length());
        try (InputStream in = RegionSetService.class.getResourceAsStream(path)) {
            DefaultFeatureCollection fc = new DefaultFeatureCollection();
            FeatureIterator<SimpleFeature> it = FJ.streamFeatureCollection(in);
            while (it.hasNext()) {
                SimpleFeature f = it.next();
                transform(f, transform);
                fc.add(it.next());
            }
            return fc;
        }
    }

    private MathTransform findMathTransform(String from, String to) throws FactoryException {
        if (from.equals(to)) {
            return null;
        }
        CoordinateReferenceSystem sourceCRS = CRS.decode(from);
        CoordinateReferenceSystem targetCRS = CRS.decode(to);
        return CRS.findMathTransform(sourceCRS, targetCRS, true);
    }

    private void transform(SimpleFeature f, MathTransform transform)
            throws MismatchedDimensionException, TransformException {
        if (transform != null) {
            Object geometry = f.getDefaultGeometry();
            if (geometry != null && geometry instanceof Geometry) {
                JTS.transform((Geometry) geometry, transform);
            }
        }
    }

    private SimpleFeatureCollection getRegionsWFS(RegionSet regionset, String requestedSRS)
            throws ServiceException, IOException {
        // For example: http://localhost:8080/geoserver/wfs?service=wfs&version=1.1.0&request=GetFeature&typeNames=oskari:kunnat2013
        Map<String, String> params = new HashMap<>();
        params.put("service", "wfs");
        params.put("version", "1.1.0");
        params.put("request", "GetFeature");
        params.put("typeName", regionset.getName());
        params.put("srsName", requestedSRS);
        final String url = IOHelper.constructUrl(regionset.getFeaturesUrl(), params);
        final HttpURLConnection connection = IOHelper.getConnection(url);
        try (InputStream in = connection.getInputStream()) {
            return WfsXmlParser.getFeatureCollection(in);
        }
    }

}
