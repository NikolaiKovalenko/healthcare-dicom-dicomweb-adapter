package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

public class BackupState {
  private String downloadFilePath;
  private int attemptsCountdown;

  public BackupState(String downloadFilePath, int attemptsCountdown) {
    this.downloadFilePath = downloadFilePath;
    this.attemptsCountdown = attemptsCountdown;
  }

  public String getDownloadFilePath() {
    return downloadFilePath;
  }

  public int getAttemptsCountdown() {
    return attemptsCountdown;
  }

  /**
   * Decrements attemptsCountdown field value if it`s value more then zero.
   * @return true if decremented, false if not.
   */
  public boolean decrement() {
    if (attemptsCountdown > 0) {
      attemptsCountdown--;
      return true;
    }
    return false;
  }
}
