package fi.nls.oskari.map.layer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.util.ConversionHelper;

/**
 * Not thread-safe in Memory implementation of OskariLayerService
 */
public class OskariLayerServiceMemory extends OskariLayerService {

    private final List<OskariLayer> list = new ArrayList<>();

    @Override
    public OskariLayer find(int id) {
        return findById(id, list);
    }

    @Override
    public OskariLayer find(String idStr) {
        return find(idStr, list);
    }

    @Override
    public List<OskariLayer> find(List<String> idList, String crs) {
        final List<OskariLayer> toReturn = new ArrayList<>();

        final List<OskariLayer> list = findAll(crs);
        for (String idStr : idList) {
            OskariLayer found = find(idStr, list);
            if (found != null) {
                toReturn.add(found);
            }
        }
        return toReturn;
    }

    @Override
    public List<OskariLayer> findAll() {
        return new ArrayList<>(list);
    }

    @Override
    public List<OskariLayer> findAll(String crs) {
        if (crs == null || crs.length() == 0) {
            return findAll();
        }

        final List<OskariLayer> filtered = new ArrayList<>();
        for (OskariLayer layer : list) {
            Set<String> supportedCRSs = layer.getSupportedCRSs();
            // If supportedCRS is null => anything goes
            if (supportedCRSs == null || supportedCRSs.contains(crs)) {
                filtered.add(layer);
            }
        }
        return filtered;
    }

    @Override
    public List<OskariLayer> findByUrlAndName(String url, String name) {
        // WHERE
        // l.name = #name#
        // AND l.url = #url#
        // AND l.parentId = -1
        final List<OskariLayer> filtered = new ArrayList<>();
        for (OskariLayer layer : list) {
            if (layer.getParentId() == -1
                    && name.equals(layer.getName())
                    && url.equals(layer.getUrl())) {
                filtered.add(layer);
            }
        }
        return filtered;
    }

    @Override
    public List<OskariLayer> findByMetadataId(String uuid) {
        // m.metadataid = #id# AND l.parentId = -1
        final List<OskariLayer> filtered = new ArrayList<>();
        for (OskariLayer layer : list) {
            if (layer.getParentId() != -1
                    && uuid.equals(layer.getMetadataId())) {
                filtered.add(layer);
            }
        }
        return filtered;
    }

    @Override
    public int insert(OskariLayer layer) {
        int id = list.size();
        list.add(layer);
        layer.setId(id);
        return id;
    }

    @Override
    public int[] insertAll(List<OskariLayer> layers) {
        int id = list.size();
        int[] ids = new int[layers.size()];
        for (int i = 0; i < layers.size(); i++) {
            OskariLayer layer = layers.get(i);
            layer.setId(id);
            ids[i] = id;
            list.add(layer);
            id++;
        }
        return ids;
    }

    @Override
    public void update(OskariLayer layer) {
        int id = layer.getId();
        if (id < 0) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == id) {
                list.set(i, layer);
                break;
            }
        }
    }

    @Override
    public void delete(int idToRemove) {
        // delete from oskari_maplayer where parentId=#id# AND parentId <> -1;
        Iterator<OskariLayer> iter = list.iterator();
        while (iter.hasNext()) {
            OskariLayer layer = iter.next();
            int parentId = layer.getParentId();
            if (parentId != -1 && parentId == idToRemove) {
                iter.remove();
            }
        }

        // delete from oskari_maplayer where id=#id#;
        iter = list.iterator();
        while (iter.hasNext()) {
            OskariLayer layer = iter.next();
            if (layer.getId() == idToRemove) {
                iter.remove();
                // id is unique
                break;
            }
        }
    }

    private static OskariLayer find(String idStr, List<OskariLayer> list) {
        final int id = ConversionHelper.getInt(idStr, -1);
        if (id >= 0) {
            return findById(id, list);
        }
        return findByExternalId(idStr, list);
    }

    private static OskariLayer findById(int id, List<OskariLayer> list) {
        for (OskariLayer layer : list) {
            if (layer.getId() == id) {
                return layer;
            }
        }
        return null;
    }

    private static OskariLayer findByExternalId(String externalId,
            List<OskariLayer> list) {
        for (OskariLayer layer : list) {
            if (layer.getExternalId().equals(externalId)) {
                return layer;
            }
        }
        return null;
    }

}
