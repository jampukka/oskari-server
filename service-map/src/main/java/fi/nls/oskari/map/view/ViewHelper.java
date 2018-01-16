package fi.nls.oskari.map.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;

import fi.nls.oskari.domain.map.view.View;
import fi.nls.oskari.service.ServiceException;

public class ViewHelper {
    
    public static List<View> getSystemViews(ViewService viewService)
            throws ServiceException {
        List<Long> viewIds = viewService.getSystemDefaultViewIds();
        List<View> views = new ArrayList<>(viewIds.size());
        for (long viewId : viewIds) {
            views.add(viewService.getViewWithConf(viewId));
        }
        return views;
    }
    
    public static Set<String> getSystemCRSs(ViewService viewService)
            throws ServiceException, JSONException {
        List<View> views = getSystemViews(viewService);
        Set<String> crss = new HashSet<>();
        for (View view : views) {
            crss.add(view.getSrsName());
        }
        return crss;
    }

}
