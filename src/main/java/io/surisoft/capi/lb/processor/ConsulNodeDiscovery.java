package io.surisoft.capi.lb.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.surisoft.capi.lb.cache.ApiCacheManager;
import io.surisoft.capi.lb.cache.StickySessionCacheManager;
import io.surisoft.capi.lb.configuration.SingleRouteProcessor;
import io.surisoft.capi.lb.schema.Api;
import io.surisoft.capi.lb.schema.ConsulObject;
import io.surisoft.capi.lb.schema.HttpMethod;
import io.surisoft.capi.lb.schema.Mapping;
import io.surisoft.capi.lb.utils.ApiUtils;
import io.surisoft.capi.lb.utils.RouteUtils;
import lombok.extern.slf4j.Slf4j;

import okhttp3.*;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.util.json.JsonObject;

import java.io.IOException;
import java.util.*;

@Slf4j
public class ConsulNodeDiscovery {

    private final String consulHost;
    private final ApiUtils apiUtils;
    private final RouteUtils routeUtils;
    private final StickySessionCacheManager stickySessionCacheManager;
    private final OkHttpClient client = new OkHttpClient.Builder().build();
    private static final String GET_ALL_SERVICES = "/v1/catalog/services";
    private static final String GET_SERVICE_BY_NAME = "/v1/catalog/service/";

    private final CamelContext camelContext;
    private final ApiCacheManager apiCacheManager;

    public ConsulNodeDiscovery(CamelContext camelContext, String consulHost, ApiUtils apiUtils, RouteUtils routeUtils, StickySessionCacheManager stickySessionCacheManager, ApiCacheManager apiCacheManager) {
        this.consulHost = consulHost;
        this.apiUtils = apiUtils;
        this.routeUtils = routeUtils;
        this.camelContext = camelContext;
        this.stickySessionCacheManager = stickySessionCacheManager;
        this.apiCacheManager = apiCacheManager;
    }

    public void processInfo() {
        getAllServices();
    }

    private void getAllServices() {
        Request request = new Request.Builder().url(consulHost + GET_ALL_SERVICES).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            public void onResponse(Call call, Response response) throws IOException {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonObject responseObject = objectMapper.readValue(response.body().string(), JsonObject.class);
                //We want to ignore the consul array for now...
                responseObject.remove("consul");
                Set<String> services = responseObject.keySet();
                try {
                    apiUtils.removeUnusedApi(camelContext, routeUtils, apiCacheManager, services.stream().toList());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for(String service : services) {
                    getServiceByName(service);
                }
            }

            public void onFailure(Call call, IOException e) {
                //TODO
                log.info(e.getMessage());
            }
        });
    }

    private void getServiceByName(String serviceName) {
        Request request = new Request.Builder().url(consulHost + GET_SERVICE_BY_NAME + serviceName).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            public void onResponse(Call call, Response response) throws IOException {
                ObjectMapper objectMapper = new ObjectMapper();
                ConsulObject[] consulResponse = objectMapper.readValue(response.body().string(), ConsulObject[].class);
                Map<String, List<Mapping>> servicesStructure = groupByServiceId(consulResponse);

                for (var entry : servicesStructure.entrySet()) {
                    String apiId = serviceName + ":" + entry.getKey();
                    Api incomingApi = new Api();
                    incomingApi.setId(apiId);
                    incomingApi.setName(serviceName);
                    incomingApi.setContext("/" + serviceName + "/" + entry.getKey());
                    incomingApi.setHttpMethod(HttpMethod.ALL);
                    incomingApi.setPublished(true);
                    incomingApi.setMappingList(new ArrayList<>());
                    incomingApi.setFailoverEnabled(true);
                    incomingApi.setRoundRobinEnabled(true);
                    incomingApi.setMatchOnUriPrefix(true);
                    incomingApi.setMappingList(entry.getValue());

                    Api existingApi = apiCacheManager.getApiById(apiId);
                    if(existingApi == null) {
                        apiCacheManager.createApi(incomingApi);
                        List<String> apiRouteIdList = routeUtils.getAllRouteIdForAGivenApi(incomingApi);
                        for(String routeId : apiRouteIdList) {
                            Route existingRoute = camelContext.getRoute(routeId);
                            if(existingRoute == null) {
                                try {
                                    camelContext.addRoutes(new SingleRouteProcessor(camelContext, incomingApi, routeUtils, routeId, stickySessionCacheManager));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        apiUtils.updateExistingApi(existingApi,incomingApi, apiCacheManager, routeUtils, camelContext, stickySessionCacheManager);
                    }
                }
            }

            public void onFailure(Call call, IOException e) {
                //TODO
                log.info(e.getMessage());
            }
        });
    }

    private Map<String, List<Mapping>> groupByServiceId(ConsulObject[] consulService) {
        Map<String, List<Mapping>> groupedService = new HashMap<>();
        Set<String> serviceIdList = new HashSet<>();
        for(ConsulObject serviceIdEntry : consulService) {
            String serviceNodeGroup = getServiceNodeGroup(serviceIdEntry);
            if(serviceNodeGroup != null) {
                serviceIdList.add(serviceNodeGroup);
            } else {
                log.trace("Service Tag group not present, service will not be deployed");
            }
        }
        Iterator<String> iterator = serviceIdList.iterator();
        while(iterator.hasNext()) {
            String id = iterator.next();
            List<Mapping> mappingList = new ArrayList<>();
            for(ConsulObject serviceIdToProcess : consulService) {
                String serviceNodeGroup = getServiceNodeGroup(serviceIdToProcess);
                if(id.equals(serviceNodeGroup)) {
                    Mapping entryMapping = apiUtils.consulObjectToMapping(serviceIdToProcess);
                    mappingList.add(entryMapping);
                }
            }
            groupedService.put(id, mappingList);
        }
        return groupedService;
    }

    private String getServiceNodeGroup(ConsulObject consulObject) {
        for(String serviceTag : consulObject.getServiceTags()) {
            if(serviceTag.contains("group=")) {
                return serviceTag.substring(serviceTag.lastIndexOf("=") + 1);
            }
        }
        return null;
    }
}
