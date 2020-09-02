package com.google.cloud.healthcare.imaging.dicomadapter.cstore.configure;

import com.google.cloud.healthcare.IDicomWebClient;
import com.google.cloud.healthcare.imaging.dicomadapter.AetDictionary;
import com.google.cloud.healthcare.imaging.dicomadapter.DestinationFilter;
import com.google.cloud.healthcare.imaging.dicomadapter.ImportAdapter;

import java.util.Map;

public interface IDestinationMapFactory {
    public Map<DestinationFilter, IDicomWebClient> createHealthcareMap();
    public Pair<Map<DestinationFilter, AetDictionary.Aet>, Map<DestinationFilter, IDicomWebClient>> createMultipleMap();

    public static class Pair<A, D>{
        private final A left;
        private final D right;

        public Pair(A left, D right) {
            this.left = left;
            this.right = right;
        }

        public A getLeft() {
            return left;
        }

        public D getRight() {
            return right;
        }
    }
}
