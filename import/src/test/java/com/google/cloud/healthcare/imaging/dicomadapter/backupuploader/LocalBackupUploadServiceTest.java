package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.client.testing.http.HttpTesting;
import com.google.cloud.healthcare.DicomWebClient;
import com.google.cloud.healthcare.util.FakeWebServer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class LocalBackupUploadServiceTest {
    private LocalBackupUploadService localBackupUploadService;
    private DelayCalculator delayCalculator;
    private static byte[] bytes;
    private FakeWebServer fakeDicomWebServer;
    private DicomWebClient client;

    @Before
    public void setUp() throws Exception {
        delayCalculator = new DelayCalculator(5,100, 5000);
        localBackupUploadService = new LocalBackupUploadService("test", delayCalculator);
        fakeDicomWebServer = new FakeWebServer();
        client = new DicomWebClient(fakeDicomWebServer.createRequestFactory(), HttpTesting.SIMPLE_URL, "/studies");
    }

    @BeforeClass
    public static void initData() {
        bytes = new byte[]{0, 1, 2, 5, 4, 3, 5, 4, 2, 0, 4, 5, 4, 7};
        try (FileOutputStream tr = new FileOutputStream("test_read");
                FileOutputStream fw = new FileOutputStream("test_remove")){
            tr.write(bytes, 0, bytes.length);
            fw.write(bytes, 0, bytes.length);
        }catch (IOException ignored) {

        }
    }

    @AfterClass
    public static void clearData() {
        try {
            Files.delete(Paths.get("test_read"));
            Files.delete(Paths.get("test"));
            Files.delete(Paths.get("test_write"));
        }catch (Exception ignored){

        }
    }

    @Test
    public void doWriteBackup() throws IBackupUploader.BackupExeption {
        localBackupUploadService.doWriteBackup(bytes, "test_write");
    }

    @Test(expected = IBackupUploader.BackupExeption.class)
    public void doWriteBackupFailed() throws IBackupUploader.BackupExeption {
        localBackupUploadService.doWriteBackup(bytes, "");
    }

    @Test
    public void doReadBackup() throws IBackupUploader.BackupExeption {
        byte[] data = localBackupUploadService.doReadBackup("test_read");
        assertThat(data).hasLength(14);
    }

    @Test(expected = IBackupUploader.BackupExeption.class)
    public void doReadBackupFailed() throws IBackupUploader.BackupExeption {
        localBackupUploadService.doReadBackup("no_file");
    }

    @Test
    public void removeBackup() throws IBackupUploader.BackupExeption {
        localBackupUploadService.removeBackup("test_remove");
    }

    @Test(expected = IBackupUploader.BackupExeption.class)
    public void removeBackupFailed() throws IBackupUploader.BackupExeption {
        localBackupUploadService.removeBackup("some_file");
    }

    @Test
    public void createBackup() throws IBackupUploader.BackupExeption {
        BackupState backupState = localBackupUploadService.createBackup(bytes);
        assertThat(backupState).isNotNull();
        assertThat(backupState).isInstanceOf(BackupState.class);
    }

    @Test(expected = NullPointerException.class)
    public void createBackupFailed() throws IBackupUploader.BackupExeption {
        localBackupUploadService.createBackup(null);
    }

    @Test
    public void startUploading() throws IBackupUploader.BackupExeption {
        BackupState backupState = new BackupState("test_read", 5);
        localBackupUploadService.startUploading(client, backupState);
    }

    @Test(expected = IBackupUploader.BackupExeption.class)
    public void startUploadingFailed() throws IBackupUploader.BackupExeption {
        BackupState backupState = new BackupState("", -1);
        localBackupUploadService.startUploading(client, backupState);
    }
}