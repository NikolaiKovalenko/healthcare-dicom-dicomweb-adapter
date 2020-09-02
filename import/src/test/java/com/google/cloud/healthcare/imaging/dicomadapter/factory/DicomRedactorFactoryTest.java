package com.google.cloud.healthcare.imaging.dicomadapter.factory;

import com.google.cloud.healthcare.deid.redactor.DicomRedactor;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.backup.BackupUploadService;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure.BackupUploadServiceFactory;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure.DicomRedactorFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class DicomRedactorFactoryTest {
  private static DicomRedactorFactory dicomRedactorFactory;
  private static DicomRedactor dicomRedactor;

  private static final String PROFILE = "true";
  private static final String TO_KEEP = "true";
  private static final String TO_REMOVE = "";

  @Before
  public static void init(){
    dicomRedactorFactory = new DicomRedactorFactory()
            .setTagsProfile(PROFILE)
            .setTagsToKeep(TO_KEEP)
            .setTagsToRemove(TO_REMOVE);
  }

  @Test
  public void createNewInstance() throws IOException {
   dicomRedactor = dicomRedactorFactory.create();

   assertThat(dicomRedactor).isNotNull();
   assertThat(dicomRedactor).isInstanceOf(DicomRedactor.class);
  }
}