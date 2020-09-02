package com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure;

import com.google.cloud.healthcare.deid.redactor.DicomRedactor;

public interface IDicomRedactorFactory {
    public DicomRedactor create();
}
