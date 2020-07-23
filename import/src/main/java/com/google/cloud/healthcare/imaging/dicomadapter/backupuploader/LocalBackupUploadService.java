package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class LocalBackupUploadService extends AbstractBackupUploadService {
  public LocalBackupUploadService(String uploadFilePath, DelayCalculator delayCalculator) {
    super(uploadFilePath, delayCalculator);
  }

  @Override
  public void doWriteBackup(byte[] backupData, String uploadFilePath) throws BackupException {
    try (FileOutputStream fos = new FileOutputStream(uploadFilePath)) {
      fos.write(backupData, 0, backupData.length);
    } catch (IOException ex) {
      throw new BackupException("Error with writing backup file", ex);
    }
  }

  @Override
  public byte[] doReadBackup(String uploadFilePath) throws BackupException {
    try (FileInputStream fin = new FileInputStream(uploadFilePath)) {
      byte[] buffer = new byte[fin.available()];
      fin.read(buffer, 0, fin.available());
      return buffer;
    } catch (IOException ex) {
      throw new BackupException("Error with reading backup file", ex);
    }
  }

  @Override
  public void removeBackup(String uploadFilePath) {
    // todo: implement_me
  }
}
