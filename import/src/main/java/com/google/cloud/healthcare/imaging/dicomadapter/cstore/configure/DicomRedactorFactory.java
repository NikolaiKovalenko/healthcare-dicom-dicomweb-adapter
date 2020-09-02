package com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure;

import com.google.cloud.healthcare.deid.redactor.DicomRedactor;
import com.google.cloud.healthcare.deid.redactor.protos.DicomConfigProtos;
import com.google.cloud.healthcare.imaging.dicomadapter.Flags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DicomRedactorFactory implements IDicomRedactorFactory {
    private static String tagsToRemove;
    private static String tagsToKeep;
    private static String tagsProfile;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private static DicomRedactor configureRedactor(Flags flags) throws IOException {
        DicomRedactor redactor = null;
        int tagEditFlags = (tagsToRemove.isEmpty() ? 0 : 1) +
                (tagsToKeep.isEmpty() ? 0 : 1) +
                (tagsProfile.isEmpty() ? 0 : 1);
        if (tagEditFlags > 1) {
            throw new IllegalArgumentException("Only one of 'redact' flags may be present");
        }
        if (tagEditFlags > 0) {
            DicomConfigProtos.DicomConfig.Builder configBuilder = DicomConfigProtos.DicomConfig.newBuilder();
            if (!tagsToRemove.isEmpty()) {
                List<String> removeList = Arrays.asList(tagsToRemove.split(","));
                configBuilder.setRemoveList(
                        DicomConfigProtos.DicomConfig.TagFilterList.newBuilder().addAllTags(removeList));
            } else if (!tagsToKeep.isEmpty()) {
                List<String> keepList = Arrays.asList(tagsToKeep.split(","));
                configBuilder.setKeepList(
                        DicomConfigProtos.DicomConfig.TagFilterList.newBuilder().addAllTags(keepList));
            } else if (!tagsProfile.isEmpty()){
                configBuilder.setFilterProfile(DicomConfigProtos.DicomConfig.TagFilterProfile.valueOf(tagsProfile));
            }

            try {
                redactor = new DicomRedactor(configBuilder.build());
            } catch (Exception e) {
                throw new IOException("Failure creating DICOM redactor", e);
            }
        }

        return redactor;
    }

    @Override
    public DicomRedactor create() {
        return null;
    }

    public DicomRedactorFactory setTagsToRemove(String tagsToRemove) {
        DicomRedactorFactory.tagsToRemove = tagsToRemove;
        return this;
    }

    public DicomRedactorFactory setTagsToKeep(String tagsToKeep) {
        DicomRedactorFactory.tagsToKeep = tagsToKeep;
        return this;
    }

    public DicomRedactorFactory setTagsProfile(String tagsProfile) {
        DicomRedactorFactory.tagsProfile = tagsProfile;
        return this;
    }
}
