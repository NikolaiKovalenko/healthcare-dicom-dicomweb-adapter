package com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure;

import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.BackupUploadService;

import java.io.IOException;

public interface IBackupUploadServiceFactory {
    public BackupUploadService create() throws IOException;
}
