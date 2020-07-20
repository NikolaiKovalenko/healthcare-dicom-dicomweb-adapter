package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import com.google.cloud.healthcare.IDicomWebClient;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class LocalBackupUploadService extends AbstractBackupUploadService {
    public LocalBackupUploadService(IDicomWebClient dicomWebClient, String uploadStorageLocation, int uploadRetryAmount) {
        super(dicomWebClient, uploadStorageLocation, uploadRetryAmount);
    }

    @Override
    public byte[] doReadBackupFile(String downloadPath) throws BackupExeption {
        try(FileInputStream fin=new FileInputStream(getUploadStorageLocation()))
        {
            byte[] buffer = new byte[fin.available()];
            fin.read(buffer, 0, fin.available());
            return buffer;
        }catch (IOException ex){
            throw new BackupExeption("Error with reading backup file", ex);
        }
    }


    @Override
    public BackupState createBackup(byte[] backupData) throws BackupExeption {
        String backupPath = getUploadStorageLocation();
        BackupState backupState = new BackupState(backupPath, getUploadRetryAmount());
        try(FileOutputStream fos=new FileOutputStream(backupPath))
        {
            fos.write(backupData, 0, backupData.length);
            return backupState;
        }catch (IOException ex){
            throw new BackupExeption("Error with writing backup file", ex);
        }
    }
}