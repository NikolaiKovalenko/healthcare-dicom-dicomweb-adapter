package com.google.cloud.healthcare.imaging.dicomadapter.backupuploader;

import java.io.IOException;
import java.util.Arrays;

public class GcpUtil {
    private String path;
    private String project;
    private String bucket;
    private String obj;

    public GcpUtil(String path) {
        this.path = path;
    }

    public void parse() throws GcpParseException {
        try {
            String route = this.path.replaceAll("gs://", "");
            String[] ar = route.split("/");
            this.project = ar[0];
            this.bucket = ar[1];
            this.obj = String.join("/", Arrays.copyOfRange(ar, 2, ar.length));

            if (this.project.isBlank() || this.project.isEmpty()) {
                throw new GcpParseException("Invalid name for GCP project");
            }
            if (this.bucket.isBlank() || this.bucket.isEmpty()) {
                throw new GcpParseException("Invalid name for GCS bucket");
            }
            if (this.obj.isBlank() || this.obj.isEmpty()) {
                throw new GcpParseException("Invalid name for GCS object");
            }
        }catch (ArrayIndexOutOfBoundsException e) {
            throw new GcpParseException("Invalid GCP path", e);
        }
    }

    public String getProject() {
        return project;
    }

    public String getBucket() {
        return bucket;
    }

    public String getObject() {
        return obj;
    }

    class GcpParseException extends IOException {
        public GcpParseException(String message, Throwable cause) {
            super(message, cause);
        }
        public GcpParseException(String message) {
            super(message);
        }
    }
}
