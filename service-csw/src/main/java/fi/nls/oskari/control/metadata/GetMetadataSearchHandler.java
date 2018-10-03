package fi.nls.oskari.control.metadata;

import fi.mml.portti.service.search.*;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.*;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.search.channel.MetadataCatalogueChannelSearchService;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import static fi.nls.oskari.control.ActionConstants.*;

/**
 * Forwards call to search service and returns results as JSON.
 * <pre>
 * {@code
 *   {
 *      "results" : [
 *      {
 *          "name" : "[result name]"
 *          "organization" : "[optional result producer]",
 *          "id" : "[optional metadata id]"
 *      }
 *      ]
 *   }
 * }
 * </pre>
 */
@OskariActionRoute("GetMetadataSearch")
public class GetMetadataSearchHandler extends RestActionHandler {

    private static final Logger log = LogFactory.getLogger(GetMetadataSearchHandler.class);

    private static final String PARAM_SEARCH = "search";
    private static final String KEY_RESULTS = "results";

    private SearchService service;

    @Override
    public void init() {
        if (service == null) {
            service = OskariComponentManager.getComponentOfType(SearchService.class);
        }
    }

    public void setSearchService(SearchService service) {
        this.service = service;
    }

    @Override
    public void handlePost(ActionParameters params) throws ActionException {
        final SearchCriteria sc = new SearchCriteria();

        sc.setSearchString(params.getHttpParam(PARAM_SEARCH));
        sc.setSRS(params.getHttpParam(PARAM_SRS));
        sc.setLocale(params.getLocale().getLanguage());
        for (MetadataField field : MetadataCatalogueChannelSearchService.getFields()) {
            field.getHandler().handleParam(params.getHttpParam(field.getName()), sc);
        }

        // validate will throw exception if we can't make the query
        validateRequest(sc);

        ChannelSearchResult searchResult = search(sc);
        JSONObject responseJSON = toJSON(searchResult);
        ResponseHelper.writeResponse(params, responseJSON);
    }

    protected ChannelSearchResult search(SearchCriteria sc) {
        sc.addChannel(MetadataCatalogueChannelSearchService.ID);
        Query query = service.doSearch(sc);
        return query.findResult(MetadataCatalogueChannelSearchService.ID);
    }

    private JSONObject toJSON(ChannelSearchResult searchResult) {
        JSONArray results = new JSONArray();
        for (SearchResultItem item : searchResult.getSearchResultItems()) {
            results.put(item.toJSON());
        }
        return JSONHelper.createJSONObject(KEY_RESULTS, results);
    }

    private void validateRequest(final SearchCriteria sc) throws ActionParamsException {
        // check free input field content
        if(sc.getSearchString() != null && !sc.getSearchString().isEmpty()) {
            // ok if user has written anything
            return;
        }
        // check advanced options, NOT OK if we get this far and don't have any selections so throw an exception
        log.debug("No free input, params are:", sc.getParams());
        if(sc.getParams().isEmpty()) {
            throw new ActionParamsException("No search string and no additional selections");
        }
    }
}
