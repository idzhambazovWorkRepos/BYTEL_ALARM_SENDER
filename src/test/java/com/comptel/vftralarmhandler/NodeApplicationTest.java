package com.comptel.vftralarmhandler;

import com.comptel.cc.event.client.exception.EventException;
import com.comptel.cc.event.client.vo.AlarmEvent;
import com.comptel.cc.event.client.vo.AlarmSeverity;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import com.comptel.mc.node.EventRecordService;
import com.comptel.mc.node.Field;
import com.comptel.mc.node.NodeContext;
import com.comptel.mc.node.EventRecord;
import com.nokia.calm.model.Alarm;
import com.nokia.calm.model.AlarmWithPage;
import com.nokia.calm.rest.CalmRestClient;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import junit.framework.Assert;
import org.apache.commons.configuration.ConfigurationException;


/**
 * Unit test node application
 */
public class NodeApplicationTest {
  
    private NodeApplication app;

    @Before
    public void setUp() throws Exception {
        app = new NodeApplication();
    }

    @After
    public void cleanUp() throws Exception {
        // Clean up after test
    }


}
