package com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.healthcare.DicomWebClientJetty;
import com.google.cloud.healthcare.IDicomWebClient;
import com.google.cloud.healthcare.StringUtil;
import com.google.cloud.healthcare.imaging.dicomadapter.AetDictionary;
import com.google.cloud.healthcare.imaging.dicomadapter.DestinationFilter;
import com.google.cloud.healthcare.imaging.dicomadapter.DestinationsConfig;
import com.google.cloud.healthcare.imaging.dicomadapter.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DestinationMapFactory implements IDestinationMapFactory{
    private static String studies;
    private static String destinationJsonInline;
    private static String destinationsJsonPath;
    private static String jsonEnvKey;
    private static GoogleCredentials credentials;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static Map<DestinationFilter, IDicomWebClient> configureHealthcareMapDestinationMap() throws IOException {
        DestinationsConfig conf = new DestinationsConfig(destinationJsonInline, destinationsJsonPath);
        Map<DestinationFilter, IDicomWebClient> result = new LinkedHashMap<>();
        for (String filterString : conf.getMap().keySet()) {
            String filterPath = StringUtil.trim(conf.getMap().get(filterString));
            result.put(
                    new DestinationFilter(filterString),
                    new DicomWebClientJetty(credentials,
                            filterPath.endsWith(studies)? filterPath : StringUtil.joinPath(filterPath, studies))
            );
        }
        return result.size() > 0 ? result : null;
    }

    public static Pair<Map<DestinationFilter, AetDictionary.Aet>, Map<DestinationFilter, IDicomWebClient>> configureMultipleDestinationTypesMap() throws IOException {

        HashMap<DestinationFilter, AetDictionary.Aet> dicomMap = new LinkedHashMap<>();
        HashMap<DestinationFilter, IDicomWebClient> healthcareMap = new LinkedHashMap<>();
        JSONArray jsonArray = JsonUtil.parseConfig(destinationJsonInline, destinationsJsonPath, jsonEnvKey);

        if (jsonArray != null) {
            for (Object elem : jsonArray) {
                JSONObject elemJson = (JSONObject) elem;
                String filter = elemJson.getString("filter");
                DestinationFilter destinationFilter = new DestinationFilter(StringUtil.trim(filter));

                // validate key in dicomMap
                validateKey(healthcareMap, filter);
                // try to create Aet instance
                if (elemJson.has("host")) {
                    dicomMap.put(destinationFilter,
                            new AetDictionary.Aet(elemJson.getString("name"),
                                    elemJson.getString("host"), elemJson.getInt("port")));
                } else {
                    // in this case to try create IDicomWebClient instance
                    // validate key in healthcareMap
                    validateKey(healthcareMap, filter);
                    String filterPath = elemJson.getString("dicomweb_destination");
                    healthcareMap.put(destinationFilter,
                            new DicomWebClientJetty(credentials,
                                    filterPath.endsWith(studies)? filterPath : StringUtil.joinPath(filterPath, studies)));
                }
            }
        }
        return new Pair<>(dicomMap, healthcareMap);
    }

    private static <T> void validateKey(Map<DestinationFilter, T> map, String key) throws IllegalArgumentException{
        if(map != null && map.containsKey(key)){
            throw new IllegalArgumentException("Duplicate filter in Destinations config");
        }
    }

    @Override
    public Map<DestinationFilter, IDicomWebClient> createHealthcareMap() {
        return createHealthcareMap();
    }

    @Override
    public Pair<Map<DestinationFilter, AetDictionary.Aet>, Map<DestinationFilter, IDicomWebClient>> createMultipleMap() {
        return createMultipleMap();
    }

    public DestinationMapFactory setStudies(String studies) {
        DestinationMapFactory.studies = studies;
        return this;
    }

    public DestinationMapFactory setDestinationJsonInline(String destinationJsonInline) {
        DestinationMapFactory.destinationJsonInline = destinationJsonInline;
        return this;
    }

    public DestinationMapFactory setDestinationsJsonPath(String destinationsJsonPath) {
        DestinationMapFactory.destinationsJsonPath = destinationsJsonPath;
        return this;
    }

    public DestinationMapFactory setJsonEnvKey(String jsonEnvKey) {
        DestinationMapFactory.jsonEnvKey = jsonEnvKey;
        return this;
    }

    public DestinationMapFactory setCredentials(GoogleCredentials credentials) {
        DestinationMapFactory.credentials = credentials;
        return this;
    }
}
