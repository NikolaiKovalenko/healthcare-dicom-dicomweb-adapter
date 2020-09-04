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
  private int attemptsAmount;

  public MultipleDestinationSendService(CStoreSenderFactory cStoreSenderFactory, BackupUploadService backupUploadService, int attemptsAmount) {
    this.cStoreSenderFactory = cStoreSenderFactory;
    this.backupUploadService = backupUploadService;
    this.attemptsAmount = attemptsAmount;
  }

  @Override
  public void start(ImmutableList<IDicomWebClient> healthcareDestinations,
                    ImmutableList<AetDictionary.Aet> dicomDestinations,
                    InputStream inputStream,
                    String sopClassUID,
                    String backupFileName) throws BackupException {
    CStoreSender cStoreSender = cStoreSenderFactory.create();

    if (backupUploadService == null) {
      throw new IllegalArgumentException("backupUploadService is null. Some flags not set.");
    }

    try {
      backupUploadService.createBackup(inputStream, backupFileName);

      for (IDicomWebClient healthcareDest: healthcareDestinations) {
        backupUploadService.startUploading(healthcareDest, new BackupState(backupFileName, attemptsAmount));
      }

      for (AetDictionary.Aet dicomDest: dicomDestinations) {
        backupUploadService.startUploading(cStoreSender, dicomDest, backupFileName, sopClassUID, new BackupState(backupFileName, attemptsAmount));
      }
    } catch (BackupException be) {
      log.error("MultipleDestinationSendService processing failed.", be);
      throw be;
    }
  }
}
