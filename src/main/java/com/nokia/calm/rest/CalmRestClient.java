/*
 * Nokia Software 2020
 * Created at: 27-Sep-2021 00:23:13
 * Created by: Caglar Kilincoglu
 */
package com.nokia.calm.rest;

import com.nokia.calm.model.Alarm;
import com.nokia.calm.model.AlarmWithPage;
import com.comptel.mc.node.logging.NodeLoggerFactory;
import com.comptel.mc.node.logging.TxeLogger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class CalmRestClient {

    private static final TxeLogger LOGGER = NodeLoggerFactory.getNodeLogger(CalmRestClient.class.getCanonicalName());

    private final String CALM_HOST;
    private final int CALM_PORT;
    private final String SERVICE;
    private final String GET_FILTERED_ALARM_WITH_MESSAGE, GET_FILTERED_ALARM_WITHOUT_MESSAGE, GET_ALL_ACTIVE_ALARMS;


    private final static Type CALM_ALARM_LIST_TYPE = new TypeToken<ArrayList<Alarm>>() {
    }.getType();

    public CalmRestClient(String calmHost, int calmPort, String system, String service) {
        CALM_HOST = calmHost;
        CALM_PORT = calmPort;
        SERVICE = service;

        GET_FILTERED_ALARM_WITH_MESSAGE = "http://" + CALM_HOST + ":" + CALM_PORT + "/api/alma/alarms/pageFiltered?text=%s&sourceObject=contains:%s&noiAdditionalText=%s&createdAt=gt:%s";
        GET_FILTERED_ALARM_WITHOUT_MESSAGE = "http://" + CALM_HOST + ":" + CALM_PORT + "/api/alma/alarms/pageFiltered?sourceObject=contains:%s&noiAdditionalText=%s&createdAt=gt:%s";
        GET_ALL_ACTIVE_ALARMS = "http://" + CALM_HOST + ":" + CALM_PORT + "/api/alma/alarms/";
    }

    /**
     * Fetches the Alarm from CALM REST API with all the filters we can provide
     *
     * @param host hostname of the Alarm source
     * @param streamName
     * @param nodeName
     * @param alarmKey key in EL_EVENTS table (i.e. NODEMANAGER3012)
     * @param msg message in EL_EVENTS table
     * @param timeLimit value in seconds that will be subtructed from current time and used as a filter to have alarms newer than the result time
     * @return AlarmWithPage object
     * @throws IOException
     */
    public AlarmWithPage getFilteredAlarmtWithMessage(String host, String streamName, String nodeName, String alarmKey, String msg, int timeLimit) throws IOException {

        String sourceObject = constructCalmSourceObj(host, streamName, nodeName);

        AlarmWithPage alarm = transformFilteredJsonResponse(sendRestRequest(String.format(GET_FILTERED_ALARM_WITH_MESSAGE, msg.replaceAll(" ", "%20"), sourceObject, alarmKey, prepareTimeLimit(timeLimit))));

        return alarm;

    }

    /**
     * Fetches the Alarm from CALM REST API with all the filters we can provide
     * 
     * @param host
     * @param streamName
     * @param nodeName
     * @param alarmKey
     * @param timeLimit
     * @return
     * @throws IOException 
     */
    public AlarmWithPage getFilteredAlarmWithoutMessage(String host, String streamName, String nodeName, String alarmKey, int timeLimit) throws IOException {
        String sourceObject = constructCalmSourceObj(host, streamName, nodeName);
        AlarmWithPage alarm = transformFilteredJsonResponse(sendRestRequest(String.format(GET_FILTERED_ALARM_WITHOUT_MESSAGE, sourceObject, alarmKey, prepareTimeLimit(timeLimit))));
        return alarm;
    }

    /**
     * Fetches all active alarms from CALM
     * 
     * @return list of Alarm objects
     * @throws IOException 
     */
    public List<Alarm> getAllActiveAlarms() throws IOException {
        return transformNonFilteredJsonResponse(sendRestRequest(GET_ALL_ACTIVE_ALARMS));
    }

    /**
     * Constructs a sourceObject for REST query to CALM. There is an order in sourceObject elements separated by '-'
     *
     * First item is fixed String 'process' Second item is StreamName Third item is NodeName Forth item is KEY from EL_EVENTS table Last item is
     * EVENTID from EL_EVENTS table
     *
     * In this method, only host, StreamName and NodeName are set due to the current queries used by VFTR. If any item is empty, return the current
     * constructed sourceObject
     *
     * @param host
     * @param streamName
     * @param nodeName
     * @param alarmKey
     * @param text
     * @param timeLimit
     * @return
     */
    private String constructCalmSourceObj(String host, String streamName, String nodeName) {

        String sb = "host-" + host + "/" + "service-" + SERVICE + "/process-";

        if (streamName.isEmpty()) {
            return sb;
        } else {
            sb = sb.concat("StreamName:" + streamName + "-");
        }

        if (nodeName.isEmpty()) {
            return sb;
        } else {
            sb = sb.concat("NodeName:" + nodeName + "-");
        }
        return sb;
    }

    private String prepareTimeLimit(int seconds) {
        //LOGGER.info("prepareTimeLimit(): Start.");

        LocalDateTime initialTime = LocalDateTime.now();
        LocalDateTime timeLimit = initialTime.minusSeconds(seconds);
        String timeLimitStr = timeLimit.format(DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("timeLimitStr = " + timeLimitStr);
        //LOGGER.info("prepareTimeLimit(): TimeLimit = " + timeLimit.format(DateTimeFormatter.ISO_DATE_TIME));
        //LOGGER.info("prepareTimeLimit(): end");
        return timeLimitStr.substring(0, timeLimitStr.lastIndexOf('.'));
    }

    private String sendRestRequest(String urlinput) throws MalformedURLException, IOException {
        System.out.println("URL = " + urlinput);
        URL url = new URL(urlinput);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : "
                                       + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));

        String output;
        String appended = "";
        while ((output = br.readLine()) != null) {
            appended = appended + output;
        }

        conn.disconnect();

        System.out.println("ServerResponse: " + appended);
        return appended;
    }

    private AlarmWithPage transformFilteredJsonResponse(String response) {
        Gson gson = new Gson();
        return gson.fromJson(response, AlarmWithPage.class);
    }

    private List<Alarm> transformNonFilteredJsonResponse(String response) {
        Gson gson = new Gson();
        return gson.fromJson(response, CALM_ALARM_LIST_TYPE);
    }

}
