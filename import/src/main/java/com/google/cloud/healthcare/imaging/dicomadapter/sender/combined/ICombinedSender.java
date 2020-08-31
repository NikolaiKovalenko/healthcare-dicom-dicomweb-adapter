package com.google.cloud.healthcare.imaging.dicomadapter.sender.combined;

import com.google.cloud.healthcare.IDicomWebClient.DicomWebException;
import com.google.cloud.healthcare.imaging.dicomadapter.AetDictionary.Aet;
import com.google.common.io.CountingInputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public interface ICombinedSender extends Closeable {
  void stowRs(InputStream in) throws DicomWebException;
  void cstore(Aet target,
              String studyUid,
              String seriesUid,
              String sopInstanceUid,
              String sopClassUid,
              CountingInputStream countingStream)
      throws IOException, InterruptedException;
}
