package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import static com.google.common.truth.Truth.assertThat;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RunWith(JUnit4.class)
public class LocalBackupUploaderTest {
    private static byte[] bytes = new byte[]{0, 1, 2, 5, 4, 3, 5, 4, 2, 0, 4, 5, 4, 7};
    private static final String READ_FILENAME = "test_read";
    private static final String WRITE_FILENAME = "test_write";
    private static final String REMOVE_FILENAME = "test_remove";
    private static final String BACKUP_PATH = "backupPath";

    private IBackupUploader localBackupUploader;

    @Before
    public void setUp() throws Exception {
        localBackupUploader = new LocalBackupUploader();
    }

    @BeforeClass
    public static void initData() {
        try (FileOutputStream tr = new FileOutputStream(READ_FILENAME);
                FileOutputStream fw = new FileOutputStream(REMOVE_FILENAME)){
            tr.write(bytes, 0, bytes.length);
            fw.write(bytes, 0, bytes.length);
        }catch (IOException ignored) {

        }
    }

    @AfterClass
    public static void clearData() {
        try {
            Files.delete(Paths.get(READ_FILENAME));
            Files.delete(Paths.get(WRITE_FILENAME));
        }catch (Exception ignored){

        }
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void doWriteBackup() throws IBackupUploader.BackupExeption {
        localBackupUploader.doWriteBackup(bytes, BACKUP_PATH, WRITE_FILENAME);
    }

    @Test
    public void doWriteBackup_Failed_OnInvalidPath() throws IBackupUploader.BackupExeption {
        exceptionRule.expect(IBackupUploader.BackupExeption.class);
        exceptionRule.expectMessage("Error with writing backup file");
        localBackupUploader.doWriteBackup(bytes, "","");
        //todo: add test: pathNotEmpty but FileIsEmpty
        //todo: add test: write and read two different files with two unic names
    }

    @Test
    public void doReadBackup() throws IBackupUploader.BackupExeption {
        byte[] data = localBackupUploader.doReadBackup(BACKUP_PATH, READ_FILENAME);
        assertThat(data).hasLength(14);
        assertThat(data).isEqualTo(bytes);
    }

    @Test
    public void doReadBackup_Failed_OnInvalidPath() throws IBackupUploader.BackupExeption {
        exceptionRule.expect(IBackupUploader.BackupExeption.class);
        exceptionRule.expectMessage("Error with reading backup file");
        localBackupUploader.doReadBackup(BACKUP_PATH,"no_file");
    }

    @Test
    public void removeBackup() throws IBackupUploader.BackupExeption {
        localBackupUploader.removeBackup(BACKUP_PATH, REMOVE_FILENAME);
    }

    @Test
    public void removeBackup_Failed_OnInvalidPath() throws IBackupUploader.BackupExeption {
        exceptionRule.expect(IBackupUploader.BackupExeption.class);
        exceptionRule.expectMessage("Error with removing temporary file");
        localBackupUploader.removeBackup(BACKUP_PATH,"some_file");
    }
}