package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LocalBackupUploader extends AbstractBackupUploader {

  public LocalBackupUploader(String uploadFilePath) {
    super(uploadFilePath);
  }

  @Override
  public void doWriteBackup(InputStream inputStream, String uniqueFileName) throws BackupException {
    try {
      validatePathParameter(uniqueFileName, "unique file name");
      Files.createDirectories(Paths.get(getUploadFilePath()));
      try (FileOutputStream fos =
               new FileOutputStream(Paths.get(getUploadFilePath(), uniqueFileName).toFile())) {
        byte[] bytes = inputStream.readAllBytes();
        fos.write(bytes, 0, bytes.length);
      }
    } catch (IOException ex) {
      throw new BackupException("Error with writing backup file.", ex);
    }
  }

  @Override
  public InputStream doReadBackup(String uniqueFileName) throws BackupException {
    try (FileInputStream fin =
                 new FileInputStream(Paths.get(getUploadFilePath(), uniqueFileName).toFile())) {
      byte[] buffer = new byte[fin.available()];
      fin.read(buffer, 0, fin.available());
      if (buffer.length == 0) {
        throw new BackupException("No data in backup file.");
      }
      return new ByteArrayInputStream(buffer);
    } catch (IOException ex) {
      throw new BackupException("Error with reading backup file : " + ex.getMessage() , ex);
    }
  }

  @Override
  public void doRemoveBackup(String uniqueFileName) throws BackupException {
    try {
      Files.delete(Paths.get(getUploadFilePath(), uniqueFileName));
    } catch (IOException e) {
      throw new BackupException("Error with removing backup file.", e);
    }
  }
}
