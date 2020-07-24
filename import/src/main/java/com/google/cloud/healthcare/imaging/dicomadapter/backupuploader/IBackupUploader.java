package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import java.io.IOException;

public interface IBackupUploader {
  void doWriteBackup(byte[] backupData, String uploadFilePath) throws BackupException;

  byte[] doReadBackup(String uploadFilePath) throws BackupException;

  void removeBackup(String uploadFilePath);

  class BackupException extends IOException {
    public BackupException(String message, Throwable cause) {
      super(message, cause);
    }
    public BackupExeption(String message) {
      super(message);
    }
  }
}
