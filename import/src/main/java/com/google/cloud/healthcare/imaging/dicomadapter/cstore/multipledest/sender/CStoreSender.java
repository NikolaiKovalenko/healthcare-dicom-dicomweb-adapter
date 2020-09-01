package com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest.sender;

import com.google.cloud.healthcare.imaging.dicomadapter.AetDictionary.Aet;
import com.google.cloud.healthcare.imaging.dicomadapter.DicomClient;
import com.google.common.io.CountingInputStream;
import java.io.IOException;
import org.dcm4che3.net.ApplicationEntity;

public class CStoreSender {

  private final ApplicationEntity applicationEntity;

  public CStoreSender(ApplicationEntity applicationEntity) {
    this.applicationEntity = applicationEntity;
  }

  public void cstore(Aet target,
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
}
