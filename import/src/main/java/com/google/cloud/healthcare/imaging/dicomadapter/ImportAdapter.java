// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.healthcare.imaging.dicomadapter;

import com.beust.jcommander.JCommander;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.healthcare.DicomWebClientJetty;
import com.google.cloud.healthcare.IDicomWebClient;
import com.google.cloud.healthcare.StringUtil;
import com.google.cloud.healthcare.DicomWebValidation;
import com.google.cloud.healthcare.LogUtil;
import com.google.cloud.healthcare.DicomWebClient;
import com.google.cloud.healthcare.deid.redactor.DicomRedactor;
import com.google.cloud.healthcare.deid.redactor.protos.DicomConfigProtos;
import com.google.cloud.healthcare.deid.redactor.protos.DicomConfigProtos.DicomConfig;
import com.google.cloud.healthcare.deid.redactor.protos.DicomConfigProtos.DicomConfig.TagFilterProfile;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.BackupFlags;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.DelayCalculator;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.GcpBackupUploader;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.BackupUploadService;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.IBackupUploader;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.LocalBackupUploader;
import com.google.cloud.healthcare.imaging.dicomadapter.cmove.CMoveSenderFactory;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.destination.IDestinationClientFactory;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.destination.MultipleDestinationClientFactory;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.destination.SingleDestinationClientFactory;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest.MultipleDestinationSendService;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest.sender.CStoreSenderFactory;
import com.google.cloud.healthcare.imaging.dicomadapter.monitoring.Event;
import com.google.cloud.healthcare.imaging.dicomadapter.monitoring.MonitoringService;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ImportAdapter {

  private static final Logger log = LoggerFactory.getLogger(ImportAdapter.class);
  private static final String STUDIES = "studies";
  private static final String GCP_PATH_PREFIX = "gs://";

  public static void main(String[] args) throws IOException, GeneralSecurityException {
    Flags flags = new Flags();
    JCommander jCommander = new JCommander(flags);
    jCommander.parse(args);

    String dicomwebAddress = DicomWebValidation.validatePath(flags.dicomwebAddress, DicomWebValidation.DICOMWEB_ROOT_VALIDATION);

    if(flags.help){
      jCommander.usage();
      return;
    }

    // Adjust logging.
    LogUtil.Log4jToStdout(flags.verbose ? "DEBUG" : "ERROR");

    // Credentials, use the default service credentials.
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
    if (!flags.oauthScopes.isEmpty()) {
      credentials = credentials.createScoped(Arrays.asList(flags.oauthScopes.split(",")));
    }

    HttpRequestFactory requestFactory =
        new NetHttpTransport().createRequestFactory(new HttpCredentialsAdapter(credentials));

    // Initialize Monitoring
    if (!flags.monitoringProjectId.isEmpty()) {
      MonitoringService.initialize(flags.monitoringProjectId, Event.values(), requestFactory);
      MonitoringService.addEvent(Event.STARTED);
    } else {
      MonitoringService.disable();
    }

    // Dicom service handlers.
    DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();

    // Handle C-ECHO (all nodes which accept associations must support this).
    serviceRegistry.addDicomService(new BasicCEchoSCP());

    // Handle C-STORE
    String cstoreDicomwebAddr = dicomwebAddress;
    String cstoreDicomwebStowPath = STUDIES;
    if (cstoreDicomwebAddr.length() == 0) {
      cstoreDicomwebAddr = flags.dicomwebAddr;
      cstoreDicomwebStowPath = flags.dicomwebStowPath;
    }
    IDicomWebClient defaultCstoreDicomWebClient = null;
    if (flags.useHttp2ForStow) {
      defaultCstoreDicomWebClient =
        new DicomWebClientJetty(
            credentials,
            StringUtil.joinPath(cstoreDicomwebAddr, cstoreDicomwebStowPath));
    } else {
      defaultCstoreDicomWebClient =
        new DicomWebClient(requestFactory, cstoreDicomwebAddr, cstoreDicomwebStowPath);
    }

    Pair<Map<DestinationFilter, AetDictionary.Aet>, Map<DestinationFilter, IDicomWebClient>> destinationMapPair = configureMultipleDestinationTypesMap(
        flags.destinationConfigInline, flags.destinationConfigPath, null, credentials);

    BackupUploadService backupUploadService = configureBackupUploadService(flags);

    DicomRedactor redactor = configureRedactor(flags);

    final IDestinationClientFactory destinationClientFactory;
    if (flags.sendToAllMatchingDestinations) {
      destinationClientFactory = new MultipleDestinationClientFactory(
          destinationMapPair.getRight(),
          destinationMapPair.getLeft(),
          defaultCstoreDicomWebClient);
    } else {
      destinationClientFactory = new SingleDestinationClientFactory(
          configureDestinationMap(
              flags.destinationConfigInline, flags.destinationConfigPath, credentials),
          defaultCstoreDicomWebClient);
    }
    String cstoreSubAet = flags.dimseCmoveAET.equals("") ? flags.dimseAET : flags.dimseCmoveAET;

    CStoreSenderFactory cStoreSenderFactory = new CStoreSenderFactory(cstoreSubAet);
    MultipleDestinationSendService multipleDestinationSendService = new MultipleDestinationSendService(cStoreSenderFactory, backupUploadService);

    CStoreService cStoreService =
        new CStoreService(destinationClientFactory, redactor, flags.transcodeToSyntax, multipleDestinationSendService);
    serviceRegistry.addDicomService(cStoreService);

    // Handle C-FIND
    IDicomWebClient dicomWebClient =
        new DicomWebClient(requestFactory, dicomwebAddress, STUDIES);
    CFindService cFindService = new CFindService(dicomWebClient, flags);
    serviceRegistry.addDicomService(cFindService);

    // Handle C-MOVE
    CMoveSenderFactory cMoveSenderFactory = new CMoveSenderFactory(cstoreSubAet, dicomWebClient);
    AetDictionary aetDict = new AetDictionary(flags.aetDictionaryInline, flags.aetDictionaryPath);
    CMoveService cMoveService = new CMoveService(dicomWebClient, aetDict, cMoveSenderFactory);
    serviceRegistry.addDicomService(cMoveService);

    // Handle Storage Commitment N-ACTION
    serviceRegistry.addDicomService(new StorageCommitmentService(dicomWebClient, aetDict));

    // Start DICOM server
    Device device = DeviceUtil.createServerDevice(flags.dimseAET, flags.dimsePort, serviceRegistry);
    device.bindConnections();
  }

  private static BackupUploadService configureBackupUploadService(Flags flags) throws IOException {
    String uploadPath = flags.persistentFileStorageLocation;
    BackupFlags backupFlags = new BackupFlags(
        flags.persistentFileUploadRetryAmount,
        flags.minUploadDelay,
        flags.maxWaitingTimeBetweenUploads,
        flags.httpErrorCodesToRetry);

    if (!uploadPath.isBlank()) {
      final IBackupUploader backupUploader;
      if (uploadPath.startsWith(GCP_PATH_PREFIX)) {
        backupUploader = new GcpBackupUploader(uploadPath, flags.gcsBackupProjectId, flags.oauthScopes);
      } else {
        backupUploader = new LocalBackupUploader(uploadPath);
      }
      return new BackupUploadService(backupUploader, backupFlags, new DelayCalculator());
      }
    return null;
  }

  private static DicomRedactor configureRedactor(Flags flags) throws IOException{
    DicomRedactor redactor = null;
    int tagEditFlags = (flags.tagsToRemove.isEmpty() ? 0 : 1) +
        (flags.tagsToKeep.isEmpty() ? 0 : 1) +
        (flags.tagsProfile.isEmpty() ? 0 : 1);
    if (tagEditFlags > 1) {
      throw new IllegalArgumentException("Only one of 'redact' flags may be present");
    }
    if (tagEditFlags > 0) {
      DicomConfigProtos.DicomConfig.Builder configBuilder = DicomConfig.newBuilder();
      if (!flags.tagsToRemove.isEmpty()) {
        List<String> removeList = Arrays.asList(flags.tagsToRemove.split(","));
        configBuilder.setRemoveList(
            DicomConfig.TagFilterList.newBuilder().addAllTags(removeList));
      } else if (!flags.tagsToKeep.isEmpty()) {
        List<String> keepList = Arrays.asList(flags.tagsToKeep.split(","));
        configBuilder.setKeepList(
            DicomConfig.TagFilterList.newBuilder().addAllTags(keepList));
      } else if (!flags.tagsProfile.isEmpty()){
        configBuilder.setFilterProfile(TagFilterProfile.valueOf(flags.tagsProfile));
      }

      try {
        redactor = new DicomRedactor(configBuilder.build());
      } catch (Exception e) {
        throw new IOException("Failure creating DICOM redactor", e);
      }
    }

    return redactor;
  }

  private static Map<DestinationFilter, IDicomWebClient> configureDestinationMap(
      String destinationJsonInline,
      String destinationsJsonPath,
      GoogleCredentials credentials) throws IOException {
    DestinationsConfig conf = new DestinationsConfig(destinationJsonInline, destinationsJsonPath);
    Map<DestinationFilter, IDicomWebClient> result = new LinkedHashMap<>();
    for (String filterString : conf.getMap().keySet()) {
      String filterPath = StringUtil.trim(conf.getMap().get(filterString));
      result.put(
              new DestinationFilter(filterString),
              new DicomWebClientJetty(credentials,
                      filterPath.endsWith(STUDIES)? filterPath : StringUtil.joinPath(filterPath, STUDIES))
      );
    }
    return result.size() > 0 ? result : null;
  }

  public static Pair<Map<DestinationFilter, AetDictionary.Aet>, Map<DestinationFilter, IDicomWebClient>> configureMultipleDestinationTypesMap(
      String destinationJsonInline,
      String jsonPath,
      String jsonEnvKey,
      GoogleCredentials credentials) throws IOException {

    HashMap<DestinationFilter, AetDictionary.Aet> dicomMap = new LinkedHashMap<>();
    HashMap<DestinationFilter, IDicomWebClient> healthcareMap = new LinkedHashMap<>();
    JSONArray jsonArray = JsonUtil.parseConfig(destinationJsonInline, jsonPath, jsonEnvKey);

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
                  filterPath.endsWith(STUDIES)? filterPath : StringUtil.joinPath(filterPath, STUDIES)));
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

  public static class Pair<A, D>{
    private final A left;
    private final D right;

    public Pair(A left, D right) {
      this.left = left;
      this.right = right;
    }

    public A getLeft() {
      return left;
    }

    public D getRight() {
      return right;
    }
  }
}
