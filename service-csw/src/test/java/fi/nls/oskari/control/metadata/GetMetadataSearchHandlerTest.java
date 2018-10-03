package fi.nls.oskari.control.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import fi.mml.portti.service.search.ChannelSearchResult;
import fi.mml.portti.service.search.SearchCriteria;
import fi.mml.portti.service.search.SearchService;
import fi.mml.portti.service.search.SearchServiceImpl;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.search.channel.MetadataCatalogueChannelSearchService;
import fi.nls.oskari.service.OskariComponentManager;

public class GetMetadataSearchHandlerTest {

    @Test
    @Ignore("throws ExceptionInInitializerError deep inside")
    public void testSearch() {
        OskariLayerService mock = Mockito.mock(OskariLayerService.class);
        Mockito.when(mock.findByMetadataId(Mockito.anyString())).thenReturn(null);

        MetadataCatalogueChannelSearchService channel = new MetadataCatalogueChannelSearchService();
        channel.setMapLayerService(mock);

        OskariComponentManager.addComponent(channel);

        SearchService service = new SearchServiceImpl();
        service.addChannel(MetadataCatalogueChannelSearchService.ID, channel);

        GetMetadataSearchHandler handler = new GetMetadataSearchHandler();
        handler.setSearchService(service);

        SearchCriteria sc = new SearchCriteria();
        sc.setSearchString("k*");
        sc.setLocale(Locale.ENGLISH.getLanguage());
        sc.setSRS("EPSG:3067");

        ChannelSearchResult searchResult = handler.search(sc);
        assertEquals(MetadataCatalogueChannelSearchService.ID, searchResult.getChannelId());
        assertTrue(searchResult.isQueryFailed());
    }

}
