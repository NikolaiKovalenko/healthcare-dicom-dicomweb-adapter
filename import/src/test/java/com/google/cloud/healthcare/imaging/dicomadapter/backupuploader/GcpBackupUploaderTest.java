package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.google.auth.Credentials;

import org.junit.*;
import org.junit.rules.ExpectedException;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import static com.google.common.truth.Truth.assertThat;

public class GcpBackupUploaderTest {
    private static GcpBackupUploader gcpBackupUploader;
    private static byte[] BYTE_SEQ_1 = new byte[] {0, 1, 2, 5, 4, 3, 5, 4, 2, 0, 4, 5, 4, 7};
    private static byte[] BYTE_SEQ_2 = new byte[] {1, 5, 7, 3, 5, 4, 0, 1, 3};
    private static byte[] BYTES_SEQ = new byte[]{0, 1, 2, 5, 4, 3, 5, 4, 2, 0, 4, 5, 4, 7};
    private static String UPLOAD_PATH = "gs://dev-idg-uvs/staging.dev-idg-uvs.appspot.com/test-backup";

    private static final String UNIQUE_FILE_NAME_1 = "uniq1";
    private static final String UNIQUE_FILE_NAME_2 = "uniq2";
    private static final String NOT_EXISTS_UPLOAD_PATH = "gs://dev-idg-uvs/staging.dev-idg-uvs.appspot.com/some-backup";
    private static final String UNIQ_NAME = "uniq";
    private static final String UNIQ_NAME_READ = "uniq_read";
    private static final String UNIQ_NAME_REMOVE = "uniq_remove";
    private static final String ENV_CREDS = "GOOGLE_APPLICATION_CREDENTIALS";
    private static final String AUTH_TYPE = "OAuth2";
    private static final String NOT_EXISTS_ENV = "SOME_ENV";
    private static final String INVALID_ENV = "USER";


    @BeforeClass
    public static void setUp() {
        gcpBackupUploader = new GcpBackupUploader();
        try {
//            try {
//                UPLOAD_PATH = System.getenv("UPLOAD_PATH");
//            }catch (NullPointerException e) {
//                UPLOAD_PATH = "gs://dev-idg-uvs/staging.dev-idg-uvs.appspot.com/test-backup";
//            }
            createObject(BYTES_SEQ, UPLOAD_PATH, UNIQ_NAME_READ);
            createObject(BYTES_SEQ, UPLOAD_PATH, UNIQ_NAME_REMOVE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDown() {
        try {
            deleteObject(UPLOAD_PATH, UNIQ_NAME_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void getGcpUtil() throws IBackupUploader.BackupException {
        GcpUtil gcpUtil = gcpBackupUploader.getGcpUtil(UPLOAD_PATH, UNIQ_NAME);
        assertThat(gcpUtil).isInstanceOf(GcpUtil.class);
        assertThat(gcpUtil).isNotNull();
    }

    @Test
    public void getGcpUtil_Failed_OninvalidUploadPath() throws IBackupUploader.BackupException {
       exceptionRule.expect(IBackupUploader.BackupException.class);
       exceptionRule.expectMessage("Invalid upload path");

       gcpBackupUploader.getGcpUtil("", UNIQ_NAME);
    }

    @Test
    public void getGcpUtil_Failed_OninvalidUniqueName() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Invalid unique file name");

        gcpBackupUploader.getGcpUtil(UPLOAD_PATH, "");
    }

    @Test
    public void getCredential() throws IOException {
        Credentials creds = gcpBackupUploader.getCredential(ENV_CREDS);
        System.out.println(creds.getAuthenticationType());

        assertThat(creds).isInstanceOf(Credentials.class);
        assertThat(creds).isNotNull();
        assertThat(creds.getAuthenticationType()).isEqualTo(AUTH_TYPE);
    }

    @Test
    public void getCredential_Failed_OnEmptyEnv() throws IOException {
        exceptionRule.expect(NullPointerException.class);
        gcpBackupUploader.getCredential("");
    }

    @Test
    public void getCredential_Failed_OnNotExistsEnv() throws IOException {
        exceptionRule.expect(NullPointerException.class);
        gcpBackupUploader.getCredential(NOT_EXISTS_ENV);
    }

    @Test
    public void getCredential_Failed_OnInvalidEnv() throws IOException {
        exceptionRule.expect(NullPointerException.class);
        gcpBackupUploader.getCredential(INVALID_ENV);
    }

    @Test
    public void doWriteBackup() throws IBackupUploader.BackupException {
        gcpBackupUploader.doWriteBackup(BYTES_SEQ, UPLOAD_PATH, UNIQ_NAME);
    }

    @Test
    public void doWriteBackup_Failed_OnInvalidBackupData() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Backup data is null");
        gcpBackupUploader.doWriteBackup(null, UPLOAD_PATH, UNIQ_NAME);
    }

    @Test
    public void doWriteBackup_Failed_OnEmptyUploadPath() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Invalid upload path");
        gcpBackupUploader.doWriteBackup(BYTES_SEQ, "", UNIQ_NAME);
    }

    @Test
    public void doWriteBackup_Failed_OnEmptyUniquePath() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Invalid unique file name");
        gcpBackupUploader.doWriteBackup(BYTES_SEQ, UPLOAD_PATH, "");
    }

    @Test
    public void doReadBackup() throws IBackupUploader.BackupException {
        byte[] buffer = gcpBackupUploader.doReadBackup(UPLOAD_PATH, UNIQ_NAME_READ);

        assertThat(buffer.length).isEqualTo(BYTES_SEQ.length);
        assertThat(buffer).isEqualTo(BYTES_SEQ);
    }

    @Test
    public void doReadBackup_Failed_OnEmptyUploadPath() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Invalid upload path");

        gcpBackupUploader.doReadBackup("", UNIQ_NAME_READ);
    }

    @Test
    public void doReadBackup_Failed_OnEmptyUniqueFileName() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Invalid unique file name");

        gcpBackupUploader.doReadBackup(UPLOAD_PATH, "");
    }

    @Test
    public void doReadBackup_Failed_OnNotExistsUploadPath() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Error with reading backup file");

        gcpBackupUploader.doReadBackup(NOT_EXISTS_UPLOAD_PATH, UNIQ_NAME);
    }

    @Test
    public void removeBackup() throws IBackupUploader.BackupException {
        gcpBackupUploader.removeBackup(UPLOAD_PATH, UNIQ_NAME_REMOVE);
    }

    @Test
    public void removeBackup_Failed_OnEmptyUploadPath() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Invalid upload path");

        gcpBackupUploader.removeBackup("", UNIQ_NAME_REMOVE);
    }

    @Test
    public void removeBackup_Failed_OnEmptyUniqueFileName() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Invalid unique file name");

        gcpBackupUploader.removeBackup(UPLOAD_PATH, "");
    }

    @Test
    public void removeBackup_Failed_OnNotExistsUploadPath() throws IBackupUploader.BackupException {
        gcpBackupUploader.removeBackup(NOT_EXISTS_UPLOAD_PATH, UNIQ_NAME_REMOVE);
    }

    @Test
    public void readWriteAndRemoveDifferentFiles() throws IBackupUploader.BackupException {
        gcpBackupUploader.doWriteBackup(BYTE_SEQ_1, UPLOAD_PATH, UNIQUE_FILE_NAME_1);
        gcpBackupUploader.doWriteBackup(BYTE_SEQ_2, UPLOAD_PATH, UNIQUE_FILE_NAME_2);
        byte[] expectedBytesFile1 = gcpBackupUploader.doReadBackup(UPLOAD_PATH, UNIQUE_FILE_NAME_1);
        byte[] expectedBytesFile2 = gcpBackupUploader.doReadBackup(UPLOAD_PATH, UNIQUE_FILE_NAME_2);

        assertThat(expectedBytesFile1).isEqualTo(BYTE_SEQ_1);
        assertThat(expectedBytesFile2).isEqualTo(BYTE_SEQ_2);

        gcpBackupUploader.removeBackup(UPLOAD_PATH, UNIQUE_FILE_NAME_1);
        gcpBackupUploader.removeBackup(UPLOAD_PATH, UNIQUE_FILE_NAME_2);
    }


    private static void createObject(byte[] data, String uploadPath, String uniqName) throws IOException {
        GcpUtil util = new GcpUtil(uploadPath.concat("/").concat(uniqName));
        util.parse();
        Credentials creds = GoogleCredentials
                .fromStream(new FileInputStream(System.getenv(ENV_CREDS)));
        Storage storage = StorageOptions.newBuilder().setCredentials(creds)
                .setProjectId(util.getProject()).build().getService();
        BlobId blobId = BlobId.of(util.getBucket(), util.getObject());
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, data);
    }

    private static void deleteObject(String uploadPath, String uniqName) throws IOException {
        GcpUtil util = new GcpUtil(uploadPath.concat("/").concat(uniqName));
        util.parse();
        Credentials creds = GoogleCredentials
                .fromStream(new FileInputStream(System.getenv(ENV_CREDS)));
        Storage storage = StorageOptions.newBuilder().setCredentials(creds)
                .setProjectId(util.getProject()).build().getService();
        storage.delete(util.getBucket(), util.getObject());
    }
}