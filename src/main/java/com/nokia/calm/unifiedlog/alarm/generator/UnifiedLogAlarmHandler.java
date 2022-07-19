/*
 * Nokia Software 2020
 * Created at: 19-Dec-2020 12:13:38
 * Created by: Caglar Kilincoglu
 */
package com.nokia.calm.unifiedlog.alarm.generator;

import com.comptel.mc.node.logging.NodeLoggerFactory;
import com.comptel.mc.node.logging.TxeLogger;
import com.nokia.calm.model.ELEvent;
import com.nokia.logging.AbstractMessage;
import com.nokia.logging.Alarm;
import com.nokia.logging.Logger;
import com.nokia.logging.alarm.AlarmTask;
import com.nokia.logging.alarm.PerceivedSeverity;
import com.nokia.logging.exceptions.InvalidKeyException;
import com.nokia.logging.exceptions.MissingMandatoryField;
import com.nokia.logging.syslog.SyslogLevel;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class UnifiedLogAlarmHandler {

    private final TxeLogger LOGGER = NodeLoggerFactory.getNodeLogger(UnifiedLogAlarmHandler.class.getCanonicalName());

    Logger unifiedLogger = new Logger(UnifiedLogAlarmHandler.class.getName());

    private String configFileName;
    private boolean sendCrit;
    private boolean sendMajor;
    private boolean sendMinor;
    private boolean sendWarning;

    private String host;
    private String system = "DR";
    private String service = "NM";

    private Map<String, UnifiedLogAlarmMapping> alarmMappings;

    public UnifiedLogAlarmHandler(String host) {
        this.alarmMappings = new HashMap<>();
        this.host = host;
    }

    public void initialise(String unifiedLoggingConfig, boolean ulSendCrit, boolean ulSendMajor, boolean ulSendMinor, boolean ulSendWarning) {
        LOGGER.info("initialise():...start");
        this.configFileName = unifiedLoggingConfig;
        this.sendCrit = ulSendCrit;
        this.sendMajor = ulSendMajor;
        this.sendMinor = ulSendMinor;
        this.sendWarning = ulSendWarning;
        if (!"".equals(this.configFileName)) {
            parseConfig();
        } else {
            LOGGER.error("No unified alarm config is provided.");
        }

        LOGGER.info("initialise():...end");
    }

    public void sendAlarm(ELEvent event) throws InvalidKeyException, MissingMandatoryField {
        LOGGER.info("sendAlarm():...start");
        UnifiedLogAlarmMapping am = this.alarmMappings.get(event.getKey());
        if (am == null) {
            LOGGER.info("sendAlarm(): Unable to send due to no matching alarm found in mapping config for '" + event.getKey() + "'.");
            return;
        }
        if (am.getAlarmId() == 0) {
            LOGGER.info("sendAlarm(): Skip sending alarm for disabled error code '" + event.getKey() + "'");
            return;
        }
        if (!this.sendCrit && am.getSeverity().equals(PerceivedSeverity.CRIT)) {
            return;
        }
        if (!this.sendMajor && am.getSeverity().equals(PerceivedSeverity.MAJOR)) {
            return;
        }
        if (!this.sendMinor && am.getSeverity().equals(PerceivedSeverity.MINOR)) {
            return;
        }
        if (!this.sendWarning && am.getSeverity().equals(PerceivedSeverity.WARNING)) {
            return;
        }
        LOGGER.info("sendAlarm(): Sending alarm for error code: '" + event.getKey() + "' with severity '" + am.getSeverity() + "'");

        Alarm alarm = (new Alarm(SyslogLevel.INFO)).setAlarmMandatoryFields(am.getAlarmTask(), event.getMessage(), am.getSeverity(), am.getKey(), am.getText(), am.getEventType(), am.getProbableCause());
        alarm.setHost(this.host);
        alarm.setSystem(this.system);
        alarm.setService(this.service);
        alarm.setAlarmId(am.getAlarmId());
        alarm.setProcess(event.getNodeid() + "-" + event.getKey() + "-EventId:" + event.getEventid());
        alarm.setAlarmData(event.getKey());
        alarm.setExtension("transient_alarms", Boolean.valueOf(true));
        this.unifiedLogger.error((AbstractMessage) alarm);
        LOGGER.info("sendAlarm(): Alarm sent for errorcode = " + event.getKey());
        LOGGER.info("sendAlarm(): Host: " + this.host + " System: " + this.system + " Service: " + this.service);
        LOGGER.info("sendAlarm(): StreamId:" + event.getStreamid() + "-NodeId:" + event.getNodeid() + "-ErrorCode:" + event.getKey() + "-EventId:" + event.getEventid());
        LOGGER.info("sendAlarm():...end");
    }

    private void parseConfig() {
        LOGGER.info("parseConfig():...start");
        String line = "";
        String errorCode = "";
        try {
            FileReader cFile = new FileReader(this.configFileName);
            BufferedReader cFileBr = new BufferedReader(cFile);
            while ((line = cFileBr.readLine()) != null) {
                if (line.trim().startsWith("#")) {
                    continue;
                }
                if (line.trim().length() < 1) {
                    continue;
                }
                Scanner scan = new Scanner(line);
                scan.useDelimiter("\\|\\|");
                //System.out.println("parseConfig(): Read line: <" + line + ">");
                UnifiedLogAlarmMapping am = new UnifiedLogAlarmMapping();
                errorCode = scan.next();
                am.setErrorCode(errorCode);
                am.setAlarmId(Integer.parseInt(scan.next()));
                am.setAlarmTask(AlarmTask.valueOf(scan.next()));
                am.setSeverity(PerceivedSeverity.valueOf(scan.next()));
                am.setKey(scan.next());
                am.setEventType(scan.next());
                am.setProbableCause(scan.next());
                am.setText("");
                scan.next();
                scan.close();
                this.alarmMappings.put(am.getErrorCode(), am);
                //System.out.println("Found alarm definition: " + am.toString());
            }
            cFile.close();
        } catch (FileNotFoundException fnfe) {
            LOGGER.error("parseConfig(): Config file " + this.configFileName + " does not exist.");
        } catch (IllegalArgumentException iae) {
            LOGGER.error("parseConfig(): Please confirm mapping file " + this.configFileName + " definition for error code " + errorCode + " is correct: " + iae);
        } catch (NoSuchElementException nse) {
            LOGGER.error("parseConfig(): Please confirm all definition for error code " + errorCode + " in mapping file  " + this.configFileName + " is defined: " + nse);
        } catch (Exception e) {
            LOGGER.error("parseConfig(): Exception caught while reading config file " + this.configFileName + ": " + e);
        }

        LOGGER.info("parseConfig():...end");
    }
}
