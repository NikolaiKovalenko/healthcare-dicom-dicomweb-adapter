package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LocalBackupUploader implements IBackupUploader {

  @Override
  public void doWriteBackup(byte[] backupData, String uploadFilePath, String uniqueFileName)
      throws BackupException {
    createDir(uploadFilePath);
    try (FileOutputStream fos =
        new FileOutputStream(Paths.get(uploadFilePath, uniqueFileName).toFile())) {
      fos.write(backupData, 0, backupData.length);
    } catch (IOException ex) {
      throw new BackupException("Error with writing backup file", ex);
    }
  }

  @Override
  public byte[] doReadBackup(String uploadFilePath, String uniqueFileName) throws BackupException {
    try (FileInputStream fin =
        new FileInputStream(Paths.get(uploadFilePath, uniqueFileName).toFile())) {
      byte[] buffer = new byte[fin.available()];
      fin.read(buffer, 0, fin.available());
      if (buffer.length == 0) {
        throw new BackupException("No data in temporary file");
      }
      return buffer;
    } catch (IOException ex) {
      throw new BackupException("Error with reading backup file : " + ex.getMessage() , ex);
    }
  }

  @Override
  public void removeBackup(String uploadFilePath, String uniqueFileName) throws BackupException {
    try {
      Files.delete(Paths.get(uploadFilePath, uniqueFileName));
      deleteDir(uploadFilePath);
    } catch (IOException e) {
      throw new BackupException("Error with removing temporary file : " + e.getMessage() , e);
    }
  }

  public void createDir(String dir) throws BackupException {
    try {
      if (!Files.exists(Paths.get(dir))) {
        Files.createDirectory(Paths.get(dir));
      }
    }catch (IOException e) {
      throw new BackupException("Error with create directory : " + e.getMessage(), e);
    }
  }

  public void deleteDir(String dir) throws BackupException {
    try {
      if (Files.exists(Paths.get(dir)) && Files.list(Paths.get(dir)).count() == 0) {
        Files.delete(Paths.get(dir));
      }
    }catch (IOException e) {
      throw new BackupException("Error with delete directory : " + e.getMessage(), e);
    }
  }
}
