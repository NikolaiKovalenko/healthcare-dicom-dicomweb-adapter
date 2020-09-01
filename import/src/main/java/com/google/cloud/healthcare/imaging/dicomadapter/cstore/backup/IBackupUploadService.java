package com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup;

import com.google.cloud.healthcare.IDicomWebClient;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.IBackupUploader.BackupException;
import java.io.InputStream;

public interface IBackupUploadService {

  BackupState createBackup(InputStream inputStream, String uniqueFileName) throws IBackupUploader.BackupException;

  InputStream getBackupStream(String uniqueFileName) throws IBackupUploader.BackupException;

  void startUploading(IDicomWebClient webClient, BackupState backupState) throws BackupException;

  void removeBackup(String uniqueFileName);
}
