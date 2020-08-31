package com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest;

import com.google.cloud.healthcare.IDicomWebClient;
import com.google.cloud.healthcare.IDicomWebClient.DicomWebException;
import com.google.cloud.healthcare.imaging.dicomadapter.AetDictionary.Aet;
import com.google.cloud.healthcare.imaging.dicomadapter.DicomClient;
import com.google.cloud.healthcare.imaging.dicomadapter.sender.combined.ICombinedSender;
import com.google.common.io.CountingInputStream;
import org.dcm4che3.net.ApplicationEntity;

import java.io.IOException;
import java.io.InputStream;

public class CombinedSender implements ICombinedSender {

  private final ApplicationEntity applicationEntity;
  private final IDicomWebClient dicomWebClient;

  public CombinedSender(ApplicationEntity applicationEntity, IDicomWebClient dicomWebClient) {
    this.applicationEntity = applicationEntity;
    this.dicomWebClient = dicomWebClient;
  }

  @Override
  public void stowRs(InputStream in) throws DicomWebException {
    dicomWebClient.stowRs(in);
  }

  @Override
  public void cstore(Aet target,
                    String studyUid,
                    String seriesUid,
                    String sopInstanceUid,
                    String sopClassUid,
                    CountingInputStream inputStream)
      throws IOException, InterruptedException {
    DicomClient.connectAndCstore(
        sopClassUid,
        sopInstanceUid,
        inputStream,
        applicationEntity,
        target.getName(),
        target.getHost(),
        target.getPort());
  }

  @Override
  public void close() throws IOException {
    applicationEntity.getDevice().unbindConnections();
  }
}
