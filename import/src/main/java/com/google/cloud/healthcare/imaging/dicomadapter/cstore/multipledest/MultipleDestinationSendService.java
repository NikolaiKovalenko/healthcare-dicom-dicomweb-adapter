package com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest;

import com.google.cloud.healthcare.IDicomWebClient;
import com.google.cloud.healthcare.imaging.dicomadapter.AetDictionary;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.BackupState;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.BackupUploadService;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.IBackupUploader.BackupException;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest.sender.CStoreSender;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest.sender.CStoreSenderFactory;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class MultipleDestinationSendService implements IMultipleDestinationSendService {

  private Logger log = LoggerFactory.getLogger(MultipleDestinationSendService.class);

  private CStoreSenderFactory cStoreSenderFactory;
  private BackupUploadService backupUploadService;

  public MultipleDestinationSendService(CStoreSenderFactory cStoreSenderFactory, BackupUploadService backupUploadService) {
    this.cStoreSenderFactory = cStoreSenderFactory;
    this.backupUploadService = backupUploadService;
  }

  @Override
  public void start(ImmutableList<IDicomWebClient> healthcareDestinations,
                    ImmutableList<AetDictionary.Aet> dicomDestinations,
                    InputStream inputStream,
                    String sopClassUID,
                    String sopInstanceUID) throws BackupException {
    CStoreSender cStoreSender = cStoreSenderFactory.create();

    if (backupUploadService == null) {
      throw new IllegalArgumentException("backupUploadService is null. Some flags not set.");
    }

    try {
      BackupState backupState = backupUploadService.createBackup(inputStream, sopInstanceUID);

      for (IDicomWebClient healthcareDest: healthcareDestinations) {
        backupUploadService.startUploading(healthcareDest, backupState.clone());
      }

      for (AetDictionary.Aet dicomDest: dicomDestinations) {
        backupUploadService.startUploading(cStoreSender, dicomDest, sopInstanceUID, sopClassUID, backupState.clone());
      }
    } catch (BackupException be) {
      log.error("MultipleDestinationSendService processing failed.", be);
      throw be;
    }
  }
}
