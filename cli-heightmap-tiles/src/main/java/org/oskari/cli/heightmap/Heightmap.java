package org.oskari.cli.heightmap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;

import org.oskari.wcs.capabilities.Capabilities;
import org.oskari.wcs.coverage.CoverageDescription;
import org.oskari.wcs.coverage.RectifiedGridCoverage;
import org.oskari.wcs.parser.CapabilitiesParser;
import org.oskari.wcs.parser.CoverageDescriptionsParser;
import org.oskari.wcs.request.DescribeCoverage;
import org.oskari.wcs.request.GetCapabilities;

import fi.nls.oskari.util.IOHelper;

public class Heightmap {

    public static void main(String[] args) throws Exception {
        String endPoint;
        String coverageId;
        String baseDir;
        double[] extent;
        int maxZoom;

        endPoint = args[0];
        coverageId = args[1];
        baseDir = args[2];
        extent = parseDoubleArray(args[3]);
        maxZoom = Integer.parseInt(args[4]);

        Capabilities caps = getCapabilities(endPoint);
        CoverageDescription tmp = describeCoverage(endPoint, coverageId);
        if (!(tmp instanceof RectifiedGridCoverage)) {
            throw new RuntimeException("Expected coverage of type RectifiedGridCoverage");
        }
        RectifiedGridCoverage desc = (RectifiedGridCoverage) tmp;

        File dir = new File(baseDir);

        new HeightmapProcessor(endPoint, caps, desc, extent, maxZoom, dir, 0, 0, 0).run();
        new HeightmapProcessor(endPoint, caps, desc, extent, maxZoom, dir, 0, 1, 0).run();
    }

    private static double[] parseDoubleArray(String csv) {
        String[] splitted = csv.split(",");
        double[] arr = new double[splitted.length];
        for (int i = 0; i < splitted.length; i++) {
            arr[i] = Double.parseDouble(splitted[i]);
        }
        return arr;
    }

    private static Capabilities getCapabilities(String endPoint)
            throws Exception {
        Map<String, String> params = GetCapabilities.toQueryParameters();
        String url = IOHelper.constructUrl(endPoint, params);
        HttpURLConnection conn = IOHelper.getConnection(url);
        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            return CapabilitiesParser.parse(in);
        }
    }

    private static CoverageDescription describeCoverage(String endPoint, String coverageId)
            throws Exception {
        Map<String, String> params = DescribeCoverage.toQueryParameters(coverageId);
        String url = IOHelper.constructUrl(endPoint, params);
        HttpURLConnection conn = IOHelper.getConnection(url);
        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            return CoverageDescriptionsParser.parse(in).get(0);
        }
    }

}
