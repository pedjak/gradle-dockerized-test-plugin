package com.jfrog.bintray.gradle;

// nu.studer.plugindev plugin is not compatible with the latest bintray plugin,
// i.e. this class has been moved to a new package
// Unfortunatelly, the latest version of bintray plugin is needed for uploading
// and this will trick nu.studer.plugindev not to fail
public class BintrayUploadTask {

    public static final String NAME = "bintrayUpload";
}