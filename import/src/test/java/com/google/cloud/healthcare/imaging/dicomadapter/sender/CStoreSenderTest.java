package com.google.cloud.healthcare.imaging.dicomadapter.sender;

import com.google.cloud.healthcare.LogUtil;
import com.google.cloud.healthcare.imaging.dicomadapter.AetDictionary;
import com.google.cloud.healthcare.imaging.dicomadapter.CMoveServiceTest;
import com.google.cloud.healthcare.imaging.dicomadapter.TestUtils;
import com.google.cloud.healthcare.imaging.dicomadapter.cstore.multipledest.sender.CStoreSender;
import com.google.cloud.healthcare.imaging.dicomadapter.util.DimseRSPAssert;
import com.google.common.io.CountingInputStream;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

@RunWith(JUnit4.class)
public class CStoreSenderTest {
    // Server properties.
    final static String serverAET = "SERVER";
    final static String serverHostname = "localhost";

    final static String clientAET = "CLIENT";

    final static String moveDestinationAET = "MOVE_DESTINATION";
    final static String moveDestinationHostname = "localhost";
    final static String nameAet = "DEVICE_A";

    // Client properties.
    ApplicationEntity clientAE;
    CStoreSender cStoreSender;
    AetDictionary.Aet validAet;
    InputStream inputStream;
    DicomInputStream dicomInputStream;
    int port;
    Association association;

    @Before
    public void setUp() throws Exception {
        LogUtil.Log4jToStdout("DEBUG");

        Device device = new Device(clientAET);
        Connection conn = new Connection();
        device.addConnection(conn);
        clientAE = new ApplicationEntity(clientAET);
        device.addApplicationEntity(clientAE);
        clientAE.addConnection(conn);
        device.setExecutor(Executors.newSingleThreadExecutor());
        device.setScheduledExecutor(Executors.newSingleThreadScheduledExecutor());

        port = new CMoveServiceTest().createDicomServer(
                new TestUtils.DicomWebClientTestBase() {
                    @Override
                    public JSONArray qidoRs(String path) throws DicomWebException {
                        JSONArray instances = new JSONArray();
                        instances.put(TestUtils.dummyQidorsInstance());
                        return instances;
                    }
                },
                CMoveServiceTest.CStoreSenderTest::new
        );

        association =
                associate(serverHostname, port,
                        UID.StudyRootQueryRetrieveInformationModelMOVE, UID.ExplicitVRLittleEndian);

        Attributes moveDataset = new Attributes();
        moveDataset.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
        moveDataset.setString(Tag.StudyInstanceUID, VR.UI, "");

        DimseRSPAssert rspAssert = new DimseRSPAssert(association, Status.Success);
        association.cmove(
                UID.StudyRootQueryRetrieveInformationModelMOVE,
                1,
                moveDataset,
                UID.ExplicitVRLittleEndian,
                serverAET,
                rspAssert);
        association.waitForOutstandingRSP();

        // Close the association.
        association.release();
        association.waitForSocketClose();

        rspAssert.assertResult(43009);

        inputStream = Files.newInputStream(Paths.get("..\\integration_test\\data\\1.2.840.113543.6.6.1.1.2.2415947926921624359.201235357587280"));

        CountingInputStream countingStream = new CountingInputStream(inputStream);
        dicomInputStream = new DicomInputStream(countingStream);
        validAet = new AetDictionary.Aet(serverAET, serverHostname, port);
        cStoreSender = new CStoreSender(clientAE);
    }

    @Test
    public void sendFile() throws IOException, InterruptedException {
        try {
            cStoreSender.cstore(validAet,
                    UID.StudyRootQueryRetrieveInformationModelMOVE,
                    UID.ExplicitVRLittleEndian,
                    dicomInputStream);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        // Close the association.
        try {
            association.release();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        association.waitForSocketClose();

        cStoreSender.close();
    }

    public Association associate(
            String serverHostname, int serverPort, String sopClass, String syntax) throws Exception {
        AAssociateRQ rq = new AAssociateRQ();
        rq.addPresentationContext(new PresentationContext(1, sopClass, syntax));
        rq.setCalledAET(serverAET);
        Connection remoteConn = new Connection();
        remoteConn.setHostname(serverHostname);
        remoteConn.setPort(serverPort);
        return clientAE.connect(remoteConn, rq);
    }
}
