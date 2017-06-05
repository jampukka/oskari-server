package fi.nls.oskari.map.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Before;
import org.junit.Test;

import fi.nls.oskari.domain.map.OskariLayer;

public class OskariLayerServiceMemoryTest {

    private static final String[] TYPES = new String[] {
        OskariLayer.TYPE_WFS,
        OskariLayer.TYPE_WMS,
        OskariLayer.TYPE_WMTS,
        OskariLayer.TYPE_ANALYSIS,
        OskariLayer.TYPE_ARCGIS93
    };

    private OskariLayerService service;

    @Before
    public void init() {
        service = new OskariLayerServiceMemory();
    }

    @Test
    public void testInsert() {
        // Should be empty at start
        List<OskariLayer> all = service.findAll();
        assertNotNull(all);
        assertEquals(0, all.size());

        OskariLayer layer = createRandomLayer();

        // id is unset
        assertEquals(-1, layer.getId());
        int id = service.insert(layer);

        // insert should set the id of the OskariLayer object
        assertEquals(layer.getId(), id);

        // After adding one OskariLayer number of Layers should be one
        all = service.findAll();
        assertNotNull(all);
        assertEquals(1, all.size());

        OskariLayer fromService = all.get(0);

        // Expect same reference
        assertTrue(layer == fromService);
    }

    @Test
    public void testUpdate() {
        // Insert random layer
        // Create another random layer, set its' id to
        // Call update(), check results
        OskariLayer random1 = createRandomLayer();
        int id = service.insert(random1);
        OskariLayer random2 = createRandomLayer();
        random2.setId(id);
        service.update(random2);
        OskariLayer found = service.find(id);

        // Simple reference checks
        assertTrue(found != random1);
        assertTrue(found == random2);
    }

    @Test
    public void whenUpdateWithUnknownIdNothingHappens() {
        // Insert random layer
        // Create another random layer, don't set the id to any known value
        // Call update(), check that nothing happened
        OskariLayer random1 = createRandomLayer();
        service.insert(random1);
        OskariLayer random2 = createRandomLayer();
        service.update(random2);

        List<OskariLayer> all = service.findAll();
        assertEquals(1, all.size());
        OskariLayer found = all.get(0);
        // Simple reference checks
        assertTrue(found == random1);
        assertTrue(found != random2);
    }

    @Test
    public void testDelete() {
        // 1. Insert n random layers
        // 2. Remove m of them
        // 3. Check that (n-m) layers remain
        // 4. Check that the ones we meant to delete are the ones we deleted

        final int n = 10;
        final int m = 3;
        final int[] ids = new int[n];
        final Set<Integer> indexesToRemove = new TreeSet<>();
        final ThreadLocalRandom r = ThreadLocalRandom.current();
        while (indexesToRemove.size() < m) {
            indexesToRemove.add(r.nextInt(0, n));
        }

        for (int i = 0; i < n; i++) {
            ids[i] = service.insert(createRandomLayer());
        }
        assertEquals(n, service.findAll().size());

        for (int idx : indexesToRemove) {
            int layerId = ids[idx];
            service.delete(layerId);
        }

        assertEquals(n-m, service.findAll().size());

        for (int idx : indexesToRemove) {
            int layerId = ids[idx];
            assertNull(service.find(layerId));
        }
    }

    @Test
    public void whenDeleteWithUnknownIdNothingHappens() {
        // Insert few layers
        // Try to delete id with id Integer.MIN_VALUE
        // Expect number of layers to remain the same

        final int n = 5;
        for (int i = 0; i < n; i++) {
            service.insert(createRandomLayer());
        }
        assertEquals(n, service.findAll().size());
        service.delete(Integer.MIN_VALUE);
        assertEquals(n, service.findAll().size());
    }

    private OskariLayer createRandomLayer() {
        final ThreadLocalRandom r = ThreadLocalRandom.current();
        final OskariLayer layer = new OskariLayer();
        layer.setType(TYPES[r.nextInt(0, TYPES.length)]);
        layer.setName(UUID.randomUUID().toString());
        layer.setUrl(UUID.randomUUID().toString());
        return layer;
    }

}
