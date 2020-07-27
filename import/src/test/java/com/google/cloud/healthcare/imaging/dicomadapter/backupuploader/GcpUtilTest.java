package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.truth.Truth.assertThat;

public class GcpUtilTest {
    private static final String PATH = "gs://project-id/artifacts.dev-idg-uvs.appspot.com/containers/images/sha256:00456d7378cb0eee5d3e3dca3d8b6386bf7d82816dc4506dbe1c54eee728deda";

    private static GcpUtil gcpUtil;

    @BeforeClass
    public static void setUp() throws Exception {
        gcpUtil = new GcpUtil(PATH);
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void getProject() throws GcpUtil.GcpParseException {
        gcpUtil.parse();
        assertThat(gcpUtil.getProject()).isNotEmpty();
        assertThat(gcpUtil.getProject()).isEqualTo("project-id");
    }

    @Test
    public void getProject_Failed_OnInvalidGcpPath() throws GcpUtil.GcpParseException {
        exceptionRule.expect(GcpUtil.GcpParseException.class);
        exceptionRule.expectMessage("Invalid GCP path");
        GcpUtil gcpUtil = new GcpUtil("gs://");
        gcpUtil.parse();
    }

    @Test
    public void getProject_Failed_OnSpaceProjectName() throws GcpUtil.GcpParseException {
        exceptionRule.expect(GcpUtil.GcpParseException.class);
        exceptionRule.expectMessage("Invalid name for GCP project");
        GcpUtil gcpUtil = new GcpUtil("gs:// / ");
        gcpUtil.parse();
    }

    @Test
    public void getProject_Failed_OnEmptyProjectName() throws GcpUtil.GcpParseException {
        exceptionRule.expect(GcpUtil.GcpParseException.class);
        exceptionRule.expectMessage("Invalid name for GCP project");
        GcpUtil gcpUtil = new GcpUtil("gs://// ");
        gcpUtil.parse();
    }

    @Test
    public void getBucket() throws GcpUtil.GcpParseException {
        gcpUtil.parse();
        assertThat(gcpUtil.getBucket()).isNotEmpty();
        assertThat(gcpUtil.getBucket()).isEqualTo("artifacts.dev-idg-uvs.appspot.com");
    }

    @Test
    public void getBucket_Failed_OnSpaceBucketName() throws GcpUtil.GcpParseException {
        exceptionRule.expect(GcpUtil.GcpParseException.class);
        exceptionRule.expectMessage("Invalid name for GCS bucket");
        GcpUtil gcpUtil = new GcpUtil("gs://Some_project/ / ");
        gcpUtil.parse();
    }

    @Test
    public void getBucket_Failed_OnEmptyBucketName() throws GcpUtil.GcpParseException {
        exceptionRule.expect(GcpUtil.GcpParseException.class);
        exceptionRule.expectMessage("Invalid name for GCS bucket");
        GcpUtil gcpUtil = new GcpUtil("gs://Some_project// ");
        gcpUtil.parse();
    }

    @Test
    public void getObject() throws GcpUtil.GcpParseException {
        gcpUtil.parse();
        assertThat(gcpUtil.getObject()).isNotEmpty();
        assertThat(gcpUtil.getObject()).isEqualTo("containers/images/sha256:00456d7378cb0eee5d3e3dca3d8b6386bf7d82816dc4506dbe1c54eee728deda");
    }

    @Test
    public void getObject_Failed_OnSpaceObjectName() throws GcpUtil.GcpParseException {
        exceptionRule.expect(GcpUtil.GcpParseException.class);
        exceptionRule.expectMessage("Invalid name for GCS object");
        GcpUtil gcpUtil = new GcpUtil("gs://Some_project/Some_bucket/ ");
        gcpUtil.parse();
    }

    @Test
    public void getObject_Failed_OnEmptyObjectName() throws GcpUtil.GcpParseException {
        exceptionRule.expect(GcpUtil.GcpParseException.class);
        exceptionRule.expectMessage("Invalid name for GCS object");
        GcpUtil gcpUtil = new GcpUtil("gs://Some_project/Some_bucket/");
        gcpUtil.parse();
    }
}