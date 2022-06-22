package com.comptel.bytelalarmhandler;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.ConfigurationException;

import com.comptel.cc.event.client.exception.EventException;
import com.comptel.database.DBConnectionParams;
import com.comptel.database.DBService;
import com.nokia.calm.model.ELEvent;
import com.comptel.eventlink.core.Nodebase;
import com.comptel.mc.node.BusinessLogic;
import com.comptel.mc.node.EventRecord;
import com.comptel.mc.node.EventRecordService;
import com.comptel.mc.node.NodeContext;
import com.comptel.mc.node.Schedulable;
import com.nokia.calm.unifiedlog.alarm.generator.UnifiedLogAlarmHandler;

public class NodeApplication implements BusinessLogic, Schedulable {

    private static final Logger logger = Logger.getLogger(NodeApplication.class.getCanonicalName());
    private DBService dbServiceEL;

    private long lastProcessedEventid = 0;
    private final int OFFSET = 1000;
    private long sleepPerAlarmInMs = 0;
    //private Properties propertiesForEM;
    private AtomicBoolean isStillSendingAlarms = new AtomicBoolean(false);
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String hostServer;
    private String message;

    private String ackuser = "alarmFiltered";

    /*
     * Calm changes
     */
    UnifiedLogAlarmHandler handler;

    @Override
    public void init(NodeContext nodeContext) throws Exception {
        initDBService(nodeContext);

        hostServer = nodeContext.getParameter("HostServer");
        handler = new UnifiedLogAlarmHandler(hostServer);
        handler.initialise("/opt/comptel/eventlink/config/unifiedlogmapping.conf", true, true, true, true);

        sleepPerAlarmInMs = Long.parseLong(nodeContext.getParameter("sleepPerAlarmInMs"));

        lastProcessedEventid = getLastProcessedEventId(hostServer);

    }

    private void initDBService(NodeContext nodeContext) {
        String dbConnectionUrl = nodeContext.getParameter("ELJDBC.URL");
        String dbUser = nodeContext.getParameter("ELJDBC.User");
        String dbPassword = nodeContext.getParameter("ELJDBC.Password");
        String dbDriver = nodeContext.getParameter("ELJDBC.Driver");
        DBConnectionParams paramsEL = new DBConnectionParams(dbConnectionUrl, dbUser, dbPassword, dbDriver);
        dbServiceEL = new DBService(paramsEL);

    }

    @Override
    public void schedule() throws Exception {
        if (isStillSendingAlarms.compareAndSet(false, true)) {
            logger.info("Schedule starts sending alarms");
            Callable<Object> callable = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        organizeFetchAndSendAlarms();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "An exception caught in schedule(). Node will continue where it left off within next schedule time. The exception message is:\n" + e.getMessage(), e);
                        throw e;
                    } finally {
                        isStillSendingAlarms.set(false);
                    }

                    logger.info("Schedule finished sending alarms");
                    return "this return is not used by any method";
                }
            };
            executorService.submit(callable);
        } else {
            logger.info("Schedule skipped as node is still busy sending alarms");
        }
    }

    private void organizeFetchAndSendAlarms() throws SQLException, EventException, NumberFormatException, UnknownHostException, InterruptedException, ConfigurationException {

        List<ELEvent> alarmEventsList = fetchAndSendAlarms();
        while (!alarmEventsList.isEmpty()) {
            logger.info(alarmEventsList.size() + " alarms sent to EM");
            alarmEventsList = fetchAndSendAlarms();
        }
        logger.finest("organizeFetchAndSendAlarms(): End");
    }

    private List<ELEvent> fetchAndSendAlarms() throws EventException, NumberFormatException, SQLException, InterruptedException, ConfigurationException {
        logger.info("fetchAndSendAlarms() enter");
        List<ELEvent> alarmEventsList = dbServiceEL.selectAllFromEL(lastProcessedEventid, OFFSET);
        if (alarmEventsList.isEmpty()) {
            logger.info("No new alarm in EL db");
        } else {
            for (ELEvent event : alarmEventsList) {
                logger.info("Alarm: ");
                message = event.getMessage();
                lastProcessedEventid = event.getEventid();

                // Limit message to first 500 bytes to prevent dbDownTime issue
                if (message.length() > 500) {
                    message = message.substring(0, 500);
                }
                logger.info("Additionalid: " + event.getEventid() + "# Alarmid(Key): " + event.getKey() + "# " +
                        "Clientid: " + event.getClientId() +
                        "# ComponenetName: " + event.getComponentName() + "# " +
                        "ModuleName: " + event.getModuleName() +
                        "# Message: " + message + "# " +
                        "Severity: " + event.getSeverity() +
                        "EventId: " + event.getEventid());
                logger.finest("Sending Alarm ...");
                try {

                    handler.sendAlarm(event);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "", e);
                    throw new RuntimeException(e);
                }
                logger.finest("Sent Alarm ...");
                lastProcessedEventid = event.getEventid();
                logger.finest("Last processed EventId = " + lastProcessedEventid);
            }
        }
        logger.finest("All alarms are finished. Waiting for next schedule...");
        return alarmEventsList;
    }


    public long getLastProcessedEventId(String host) {
        logger.info("getLastProcessedEventId(): start");
        String key = "EID_KEY_" + host;
        String eventId = Nodebase.nb_store_get(key);
        if (eventId.isEmpty()) {
            return 0L;
        }
        logger.info("getLastProcessedEventId(): Last stored (processed) EventID = '" + eventId + "'");
        return Long.parseLong(eventId);
    }

    @Override
    public void process(EventRecord er) throws Exception {
        logger.info("Processing record");
    }

    @Override
    public void setService(EventRecordService eventRecordService) {
    }

    @Override
    public void flush() throws Exception {
    }

    @Override
    public void pause(int reason) throws Exception {
    }

    @Override
    public void request(String requestString) throws Exception {
    }

    @Override
    public void resume(int reason) throws Exception {
    }

    @Override
    public void end() throws Exception {
        executorService.shutdownNow();
    }

}
