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
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure.BackupUploadServiceFactory;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure.DestinationMapFactory;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure.DicomRedactorFactory;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure.IDestinationMapFactory;
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

    IDestinationMapFactory.Pair<Map<DestinationFilter, AetDictionary.Aet>, Map<DestinationFilter, IDicomWebClient>> destinationMapPair =
            new DestinationMapFactory()
            .setDestinationJsonInline(flags.destinationConfigInline)
            .setDestinationsJsonPath(flags.destinationConfigPath)
            .setJsonEnvKey(null)
            .setCredentials(credentials)
            .createMultipleMap();

    BackupUploadService backupUploadService = new BackupUploadServiceFactory()
            .setPersistentFileStorageLocation(flags.persistentFileStorageLocation)
            .setPersistentFileUploadRetryAmount(flags.persistentFileUploadRetryAmount)
            .setMinUploadDelay(flags.minUploadDelay)
            .setMaxWaitingTimeBetweenUploads(flags.maxWaitingTimeBetweenUploads)
            .setGcpBackupPrefix(GCP_PATH_PREFIX)
            .setGcsBackupProjectId(flags.gcsBackupProjectId)
            .setHttpErrorCodesToRetry(flags.httpErrorCodesToRetry)
            .setOauthScopes(flags.oauthScopes)
            .create();

    DicomRedactor redactor = new DicomRedactorFactory()
            .setTagsProfile(flags.tagsProfile)
            .setTagsToKeep(flags.tagsToKeep)
            .setTagsToRemove(flags.tagsToRemove)
            .create();

    final IDestinationClientFactory destinationClientFactory;
    if (flags.sendToAllMatchingDestinations) {
      destinationClientFactory = new MultipleDestinationClientFactory(
          destinationMapPair.getRight(),
          destinationMapPair.getLeft(),
          defaultCstoreDicomWebClient);
    } else {
      destinationClientFactory = new SingleDestinationClientFactory(
          new DestinationMapFactory()
           .setDestinationJsonInline(flags.destinationConfigInline)
           .setDestinationsJsonPath(flags.destinationConfigPath)
           .setCredentials(credentials)
           .createSingleMap(),
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
}
