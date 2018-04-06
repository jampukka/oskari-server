package fi.nls.oskari.wfs.extension;

import fi.nls.oskari.service.ServiceRuntimeException;
import fi.nls.oskari.wfs.pojo.WFSLayerStore;
import fi.nls.oskari.work.WFSMapLayerJob;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserLayerProcessor {

    private static final String USERLAYER_PREFIX = "userlayer_";
    
    protected static final Set<String> EXCLUDED_PROPERTIES;
    static {
        EXCLUDED_PROPERTIES = new HashSet<>();
        EXCLUDED_PROPERTIES.add("property_json");
        EXCLUDED_PROPERTIES.add("uuid");
        EXCLUDED_PROPERTIES.add("user_layer_id");
        EXCLUDED_PROPERTIES.add("feature_id");
        EXCLUDED_PROPERTIES.add("created");
        EXCLUDED_PROPERTIES.add("updated");
        EXCLUDED_PROPERTIES.add("attention_text");
        EXCLUDED_PROPERTIES.add("id");
    }

    public static boolean isProcessable(WFSLayerStore layer) {
        return layer.getLayerId().startsWith(USERLAYER_PREFIX);
    }

    /**
     * Parse features' property_json attribute and add parsed attributes to features
     */
    public static FeatureCollection<SimpleFeatureType, SimpleFeature> process(FeatureCollection<SimpleFeatureType, SimpleFeature> features, WFSLayerStore layer) {
        try (FeatureIterator<SimpleFeature> iterator = features.features()) {
            DefaultFeatureCollection result = null;
            SimpleFeatureBuilder builder = null;
            while (iterator.hasNext()) {
                SimpleFeature simpleFeature = iterator.next();
                Map<String, Object> properties = getUserlayerFields(simpleFeature);
                if (result == null) {
                    SimpleFeatureType type = getFeatureType(simpleFeature.getFeatureType(), layer, properties);
                    result = new DefaultFeatureCollection(null, type);
                    builder = new SimpleFeatureBuilder(type);
                }
                for (Property property : simpleFeature.getProperties()) {
                    if (!EXCLUDED_PROPERTIES.contains(property.getName().getLocalPart())) {
                        builder.set(property.getName(), property.getValue());
                    }
                }
                // add new attribute values (from property_json)
                for (Map.Entry<String, Object> attribute : properties.entrySet()) {
                    builder.set(attribute.getKey(), attribute.getValue());
                }
                // buildFeature calls reset() internally so we are good to go for next round
                result.add(builder.buildFeature(simpleFeature.getID()));
            }
            return result;
        } catch (Exception ex) {
            throw new ServiceRuntimeException("Userlayer processing failed", ex);
        }
    }

    protected static Map<String, Object> getUserlayerFields(SimpleFeature simpleFeature) throws IOException {
        Property propertyJson = simpleFeature.getProperty("property_json");
        return WFSMapLayerJob.OM.readValue(propertyJson.getValue().toString(), WFSMapLayerJob.TYPE_REF_HASHMAP);
    }

    protected static SimpleFeatureType getFeatureType(SimpleFeatureType type, WFSLayerStore layer, Map<String, Object> jsonMap) {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(type.getName());
        typeBuilder.setNamespaceURI(layer.getFeatureNamespaceURI());
        typeBuilder.setSRS(layer.getSRSName());

        //copy feature's attributes to new feature type builder
        //do not add excludedProperties
        for (AttributeDescriptor desc : type.getAttributeDescriptors()) {
            if (!EXCLUDED_PROPERTIES.contains(desc.getLocalName())) {
                typeBuilder.add(desc);
            }
        }
        // add new parsed attributes from property_json to new type builder
        for (Map.Entry<String, Object> attribute : jsonMap.entrySet()) {
            typeBuilder.add(attribute.getKey(), attribute.getValue().getClass());
        }

        return typeBuilder.buildFeatureType();
    }

}