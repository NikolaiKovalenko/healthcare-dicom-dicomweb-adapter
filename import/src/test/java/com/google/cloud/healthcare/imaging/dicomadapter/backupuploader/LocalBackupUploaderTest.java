package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class LocalBackupUploaderTest {
  private static byte[] bytes = new byte[] {0, 1, 2, 5, 4, 3, 5, 4, 2, 0, 4, 5, 4, 7};
  private static final String READ_FILENAME = "test_read";
  private static final String READ_FILENAME_EMPTY = "test_read_empty";
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
    try {
      if (!Files.exists(Paths.get(BACKUP_PATH))) {
        Files.createDirectory(Paths.get(BACKUP_PATH));
      }
      try (FileOutputStream tr =
              new FileOutputStream(Paths.get(BACKUP_PATH, READ_FILENAME).toString());
          FileOutputStream fw =
              new FileOutputStream(Paths.get(BACKUP_PATH, REMOVE_FILENAME).toString());
           FileOutputStream ew =
                   new FileOutputStream(Paths.get(BACKUP_PATH, READ_FILENAME_EMPTY).toString())) {
        tr.write(bytes, 0, bytes.length);
        fw.write(bytes, 0, bytes.length);
        ew.write(new byte[]{}, 0, 0);
      }
    } catch (IOException ex) {
      fail("On test file creation failed.");
      ex.printStackTrace();
    }
  }

  @AfterClass
  public static void clearData() {
    try {
      if (Files.exists(Paths.get(BACKUP_PATH))) {
        Files.delete(Paths.get(BACKUP_PATH, READ_FILENAME));
        Files.delete(Paths.get(BACKUP_PATH, READ_FILENAME_EMPTY));
        Files.delete(Paths.get(BACKUP_PATH, WRITE_FILENAME));
        Files.delete(Paths.get(BACKUP_PATH));
      }
    } catch (Exception ex) {
      fail("test file clear failed.");
      ex.printStackTrace();
    }
  }

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void doWriteBackup() throws IBackupUploader.BackupException {
    localBackupUploader.doWriteBackup(bytes, BACKUP_PATH, WRITE_FILENAME);
  }

  @Test
  public void doWriteBackup_Failed_OnInvalidPath() throws IBackupUploader.BackupException {
    exceptionRule.expect(IBackupUploader.BackupException.class);
    exceptionRule.expectMessage("Error with writing backup file");
    localBackupUploader.doWriteBackup(bytes, "", "");
  }

  @Test
  public void readAndWriteAndRemoveDiffFiles() throws IBackupUploader.BackupException {
      byte[] seq1 = new byte[]{1, 5, 7, 3, 5, 4, 0, 1, 3};
      byte[] seq2 = new byte[]{8, 5, 4, 9, 6, 7, 0, 2, 4, 4, 3, 5, 9};
      String uniqFilename1 = "uniq1";
      String uniqFilename2 = "uniq2";
      localBackupUploader.doWriteBackup(seq1, BACKUP_PATH, uniqFilename1);
      localBackupUploader.doWriteBackup(seq2, BACKUP_PATH, uniqFilename2);
      byte[] data1 = localBackupUploader.doReadBackup(BACKUP_PATH, uniqFilename1);
      byte[] data2 = localBackupUploader.doReadBackup(BACKUP_PATH, uniqFilename2);
      assertThat(data1).hasLength(seq1.length);
      assertThat(data1).isEqualTo(seq1);
      assertThat(data2).hasLength(seq2.length);
      assertThat(data2).isEqualTo(seq2);
      localBackupUploader.removeBackup(BACKUP_PATH, uniqFilename1);
      localBackupUploader.removeBackup(BACKUP_PATH, uniqFilename2);
  }

  @Test
  public void doReadBackup() throws IBackupUploader.BackupException {
    byte[] data = localBackupUploader.doReadBackup(BACKUP_PATH, READ_FILENAME);
    assertThat(data).hasLength(bytes.length);
    assertThat(data).isEqualTo(bytes);
  }

  @Test
  public void doReadBackup_Failed_OnInvalidPath() throws IBackupUploader.BackupException {
    exceptionRule.expect(IBackupUploader.BackupException.class);
    exceptionRule.expectMessage("Error with reading backup file");
    localBackupUploader.doReadBackup(BACKUP_PATH, "no_file");
  }

  @Test
  public void doReadBackup_Failed_OnEmptyFile() throws IBackupUploader.BackupException {
    exceptionRule.expect(IBackupUploader.BackupException.class);
    exceptionRule.expectMessage("No data in temporary file");
    localBackupUploader.doReadBackup(BACKUP_PATH, READ_FILENAME_EMPTY);
  }

  @Test
  public void removeBackup() throws IBackupUploader.BackupException {
    localBackupUploader.removeBackup(BACKUP_PATH, REMOVE_FILENAME);
  }

  @Test
  public void removeBackup_Failed_OnInvalidPath() throws IBackupUploader.BackupException {
    exceptionRule.expect(IBackupUploader.BackupException.class);
    exceptionRule.expectMessage("Error with removing temporary file");
    localBackupUploader.removeBackup(BACKUP_PATH, "some_file");
  }
}
