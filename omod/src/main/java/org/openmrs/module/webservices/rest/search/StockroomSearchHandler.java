package org.openmrs.module.webservices.rest.search;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Location;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.inventory.api.IStockroomDataService;
import org.openmrs.module.openhmis.inventory.api.model.Stockroom;
import org.openmrs.module.openhmis.inventory.web.ModuleRestConstants;
import org.openmrs.module.webservices.rest.resource.AlreadyPagedWithLength;
import org.openmrs.module.webservices.rest.resource.PagingUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.api.SearchConfig;
import org.openmrs.module.webservices.rest.web.resource.api.SearchHandler;
import org.openmrs.module.webservices.rest.web.resource.api.SearchQuery;
import org.springframework.stereotype.Component;

@Component
public class StockroomSearchHandler implements SearchHandler {

    private final SearchConfig searchConfig = new SearchConfig("default", ModuleRestConstants.STOCKROOM_RESOURCE,
		    Arrays.asList("*"),
            Arrays.asList(
                    new SearchQuery.Builder("Find a stockroom by its name, optionally filtering by location")
                            .withRequiredParameters("q")
                            .withOptionalParameters("location_uuid").build()
            )
    );

    @Override
    public PageableResult search(RequestContext context) {
        String query = context.getParameter("q");
        String locationUuid = context.getParameter("location_uuid");
        query = query.isEmpty() ? null : query;
        locationUuid = StringUtils.isEmpty(locationUuid) ? null : locationUuid;
        IStockroomDataService service = Context.getService(IStockroomDataService.class);

        if (locationUuid == null) {
            // Do a name search
            PagingInfo pagingInfo = PagingUtil.getPagingInfoFromContext(context);
            List<Stockroom> stockrooms = service.getByNameFragment(query, context.getIncludeAll(), pagingInfo);
            AlreadyPagedWithLength<Stockroom> results = new AlreadyPagedWithLength<Stockroom>(context, stockrooms, pagingInfo.hasMoreResults(), pagingInfo.getTotalRecordCount());
            return results;
        }
        else {
            LocationService locationService = Context.getLocationService();
            Location location = locationService.getLocationByUuid(locationUuid);
            // Get all items in the department if no name query is given
            if (query == null) {
                return searchByLocation(locationUuid, context);
            }
            // Do a name + department search
            PagingInfo pagingInfo = PagingUtil.getPagingInfoFromContext(context);
            List<Stockroom> stockrooms = service.getStockrooms(location, query, context.getIncludeAll(), pagingInfo);
            PageableResult results = new AlreadyPagedWithLength<Stockroom>(context, stockrooms, pagingInfo.hasMoreResults(), pagingInfo.getTotalRecordCount());
            return results;
        }
    }

    public PageableResult searchByLocation(String locationUuid, RequestContext context) {
        LocationService locationService = Context.getLocationService();
        Location location = locationService.getLocationByUuid(locationUuid);
        IStockroomDataService service = Context.getService(IStockroomDataService.class);

        PagingInfo pagingInfo = PagingUtil.getPagingInfoFromContext(context);
        List<Stockroom> stockrooms = service.getStockroomsByLocation(location, context.getIncludeAll(), pagingInfo);
        PageableResult results = new AlreadyPagedWithLength<Stockroom>(context, stockrooms, pagingInfo.hasMoreResults(), pagingInfo.getTotalRecordCount());
        return results;
    }

    @Override
    public SearchConfig getSearchConfig() {
        return searchConfig;
    }
}
