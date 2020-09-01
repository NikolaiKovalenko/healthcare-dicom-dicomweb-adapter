package com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest;

import com.google.cloud.healthcare.IDicomWebClient;
import com.google.cloud.healthcare.imaging.dicomadapter.AetDictionary;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.BackupState;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.BackupUploadService;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.IBackupUploader;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest.sender.CStoreSender;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest.sender.CStoreSenderFactory;
import com.google.common.collect.ImmutableList;

import java.io.InputStream;

public class MultipleDestinationSendService implements IMultipleDestinationSendService {

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
                    String sopInstanceUID) {
    CStoreSender cStoreSender = cStoreSenderFactory.create();
  //  cStoreSender.cstore();
    /*Aet target,
      String sopInstanceUid,
      String sopClassUid,
      CountingInputStream inputStream*/

    if (backupUploadService == null) {
      throw new IllegalArgumentException("backupUploadService is null. Some flags not set.");
    }
    try {
      BackupState backupState = backupUploadService.createBackup(inputStream, sopInstanceUID);
    } catch (IBackupUploader.BackupException e) {
      e.printStackTrace();
    }

  //  backupUploadService.startUploading();
  }
}
