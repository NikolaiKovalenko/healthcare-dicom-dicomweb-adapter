package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.FileInputStream;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class GcpBackupUploaderTest {
    private static GcpBackupUploader gcpBackupUploader;
    private static byte[] BYTE_SEQ_1 = new byte[]{0, 1, 2, 5, 4, 3, 5, 4, 2, 0, 4, 5, 4, 7};
    private static byte[] BYTE_SEQ_2 = new byte[]{1, 5, 7, 3, 5, 4, 0, 1, 3};
    private static byte[] BYTES_SEQ = new byte[]{0, 1, 2, 5, 4, 3, 5, 4, 2, 0, 4, 5, 4, 7};

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
    private static final String UPLOAD_PATH = "gs://dev-idg-uvs/staging.dev-idg-uvs.appspot.com/test-backup";
    private static final String UPLOAD_PATH_EMPTY_PROGECT_NAME = "gs:/// ";
    private static final String UPLOAD_PATH_SPACE_PROGECT_NAME = "gs:// / ";
    private static final String UPLOAD_PATH_EMPTY_BUCKET_NAME = "gs:///some// ";
    private static final String UPLOAD_PATH_SPACE_BUCKET_NAME = "gs://some/ / ";
    private static final String UPLOAD_PATH_EMPTY_UPLOAD_OBJECT = "gs:///some/some//";
    private static final String UPLOAD_PATH_SPACE_UPLOAD_OBJECT = "gs://some/some/ ";
    private static final String PROJECT_NAME = "dev-idg-uvs";
    private static final String BUCKET_NAME = "staging.dev-idg-uvs.appspot.com";
    private static final String UPLOAD_OBJECT = "test-backup";

    @BeforeClass
    public static void setUp() throws Exception {
        gcpBackupUploader = new GcpBackupUploader(UPLOAD_PATH);
        try {
            createObject(BYTES_SEQ, UNIQ_NAME_READ);
            createObject(BYTES_SEQ, UNIQ_NAME_REMOVE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDown() {
        try {
            deleteObject(UNIQ_NAME_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void parseUri() throws GcpBackupUploader.GcpUriParseException {
        gcpBackupUploader = new GcpBackupUploader(UPLOAD_PATH);

        assertThat(gcpBackupUploader.getProjectName()).isEqualTo(PROJECT_NAME);
        assertThat(gcpBackupUploader.getBucketName()).isEqualTo(BUCKET_NAME);
        assertThat(gcpBackupUploader.getUploadObject()).isEqualTo(UPLOAD_OBJECT);
    }

    @Test
    public void parseUri_Failed_OnInvalidUploadPath() throws GcpBackupUploader.GcpUriParseException {
        exceptionRule.expect(GcpBackupUploader.GcpUriParseException.class);
        exceptionRule.expectMessage("Invalid upload path");

        new GcpBackupUploader("");
    }

    @Test
    public void parseUri_Failed_OnSpaceUploadPath() throws GcpBackupUploader.GcpUriParseException {
        exceptionRule.expect(GcpBackupUploader.GcpUriParseException.class);
        exceptionRule.expectMessage("Invalid upload path");

        new GcpBackupUploader(" ");
    }

    @Test
    public void parseUri_Failed_OnInvalidProjectName() throws GcpBackupUploader.GcpUriParseException {
        exceptionRule.expect(GcpBackupUploader.GcpUriParseException.class);
        exceptionRule.expectMessage("Invalid upload path");

        new GcpBackupUploader(UPLOAD_PATH_EMPTY_PROGECT_NAME);
    }

    @Test
    public void parseUri_Failed_OnSpaceProjectName() throws GcpBackupUploader.GcpUriParseException {
        exceptionRule.expect(GcpBackupUploader.GcpUriParseException.class);
        exceptionRule.expectMessage("Invalid upload path");

        new GcpBackupUploader(UPLOAD_PATH_SPACE_PROGECT_NAME);
    }

    @Test
    public void parseUri_Failed_OnInvalidBucketName() throws GcpBackupUploader.GcpUriParseException {
        exceptionRule.expect(GcpBackupUploader.GcpUriParseException.class);
        exceptionRule.expectMessage("Invalid upload path");

        new GcpBackupUploader(UPLOAD_PATH_EMPTY_BUCKET_NAME);
    }

    @Test
    public void parseUri_Failed_OnSpaceBucketName() throws GcpBackupUploader.GcpUriParseException {
        exceptionRule.expect(GcpBackupUploader.GcpUriParseException.class);
        exceptionRule.expectMessage("Invalid upload path");

        new GcpBackupUploader(UPLOAD_PATH_SPACE_BUCKET_NAME);
    }

    @Test
    public void parseUri_Failed_OnInvalidUploadObject() throws GcpBackupUploader.GcpUriParseException {
        exceptionRule.expect(GcpBackupUploader.GcpUriParseException.class);
        exceptionRule.expectMessage("Invalid upload path");

        new GcpBackupUploader(UPLOAD_PATH_EMPTY_UPLOAD_OBJECT);
    }

    @Test
    public void parseUri_Failed_OnSpaceUploadObject() throws GcpBackupUploader.GcpUriParseException {
        exceptionRule.expect(GcpBackupUploader.GcpUriParseException.class);
        exceptionRule.expectMessage("Invalid upload path");

        new GcpBackupUploader(UPLOAD_PATH_SPACE_UPLOAD_OBJECT);
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
    public void getUploadFilePath() {
        assertThat(gcpBackupUploader.getUploadFilePath()).isNotNull();
        assertThat(gcpBackupUploader.getUploadFilePath()).isNotEmpty();
        assertThat(gcpBackupUploader.getUploadFilePath()).isEqualTo(UPLOAD_PATH);
    }

    @Test
    public void doWriteBackup() throws IBackupUploader.BackupException {
        gcpBackupUploader.doWriteBackup(BYTES_SEQ, UNIQ_NAME);
    }

    @Test
    public void doWriteBackup_Failed_OnInvalidBackupData() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Backup data is null");

        gcpBackupUploader.doWriteBackup(null, UNIQ_NAME);
    }

    @Test
    public void doWriteBackup_Failed_OnEmptyUniquePath() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Invalid unique file name");

        gcpBackupUploader.doWriteBackup(BYTES_SEQ, "");
    }

    @Test
    public void doReadBackup() throws IBackupUploader.BackupException {
        byte[] buffer = gcpBackupUploader.doReadBackup(UNIQ_NAME_READ);

        assertThat(buffer.length).isEqualTo(BYTES_SEQ.length);
        assertThat(buffer).isEqualTo(BYTES_SEQ);
    }

    @Test
    public void doReadBackup_Failed_OnEmptyUniqueFileName() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Invalid unique file name");

        gcpBackupUploader.doReadBackup( "");
    }

    @Test
    public void removeBackup() throws IBackupUploader.BackupException {
        gcpBackupUploader.removeBackup(UNIQ_NAME_REMOVE);
    }

    @Test
    public void removeBackup_Failed_OnEmptyUniqueFileName() throws IBackupUploader.BackupException {
        exceptionRule.expect(IBackupUploader.BackupException.class);
        exceptionRule.expectMessage("Invalid unique file name");

        gcpBackupUploader.removeBackup("");
    }

    @Test
    public void removeBackup_Failed_OnNotExistsUploadPath() throws IBackupUploader.BackupException, GcpBackupUploader.GcpUriParseException {
       new GcpBackupUploader(NOT_EXISTS_UPLOAD_PATH).removeBackup(UNIQ_NAME_REMOVE);
    }

    @Test
    public void readWriteAndRemoveDifferentFiles() throws IBackupUploader.BackupException {
        gcpBackupUploader.doWriteBackup(BYTE_SEQ_1, UNIQUE_FILE_NAME_1);
        gcpBackupUploader.doWriteBackup(BYTE_SEQ_2, UNIQUE_FILE_NAME_2);
        byte[] expectedBytesFile1 = gcpBackupUploader.doReadBackup(UNIQUE_FILE_NAME_1);
        byte[] expectedBytesFile2 = gcpBackupUploader.doReadBackup(UNIQUE_FILE_NAME_2);

        assertThat(expectedBytesFile1).isEqualTo(BYTE_SEQ_1);
        assertThat(expectedBytesFile2).isEqualTo(BYTE_SEQ_2);

        gcpBackupUploader.removeBackup(UNIQUE_FILE_NAME_1);
        gcpBackupUploader.removeBackup(UNIQUE_FILE_NAME_2);
    }

    private static void createObject(byte[] data, String uniqName) throws IOException {
        Credentials creds = GoogleCredentials
                .fromStream(new FileInputStream(System.getenv(ENV_CREDS)));
        Storage storage = StorageOptions.newBuilder().setCredentials(creds)
                .setProjectId(gcpBackupUploader.getProjectName()).build().getService();
        BlobId blobId = BlobId.of(gcpBackupUploader.getBucketName(),
                gcpBackupUploader.getUploadObject().concat("/").concat(uniqName));
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, data);
    }

    private static void deleteObject(String uniqName) throws IOException {
        Credentials creds = GoogleCredentials
                .fromStream(new FileInputStream(System.getenv(ENV_CREDS)));
        Storage storage = StorageOptions.newBuilder().setCredentials(creds)
                .setProjectId(gcpBackupUploader.getProjectName()).build().getService();
        storage.delete(gcpBackupUploader.getBucketName(),
                gcpBackupUploader.getUploadObject().concat("/").concat(uniqName));
    }
}