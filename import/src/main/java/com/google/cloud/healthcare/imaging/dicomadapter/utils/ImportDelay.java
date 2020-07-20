package com.google.cloud.healthcare.imaging.dicomadapter.utils;

public class ImportDelay {
    private static final double base = 2d;
    private static final int maxAttempt = 10;

    public static long SimpleExpDelay(int attempt){
        if (attempt <= maxAttempt){
            return Math.round(Math.pow(base, attempt) - base);
        }else{
            return Math.round(Math.pow(base, maxAttempt) - base);
        }
    }
}
