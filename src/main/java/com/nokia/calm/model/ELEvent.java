/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nokia.calm.model;

import java.sql.Timestamp;

/**
 *
 * @author cpt2vot
 */
public class ELEvent {
    
    private long eventid;
    private String eventtype;
    private String key;
    private String message;
    private long streamid;
    private long nodeid;
    private String host;
    private Timestamp eventtimegmt;
    
    private String clientId;
    private String componentName;
    private String moduleName;
    private String nodeName;
    private String streamName;
    private String severity;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
    
    public long getEventid() {
        return eventid;
    }

    public void setEventid(long eventid) {
        this.eventid = eventid;
    }

    public String getEventtype() {
        return eventtype;
    }

    public void setEventtype(String eventtype) {
        this.eventtype = eventtype;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getStreamid() {
        return streamid;
    }

    public void setStreamid(long streamid) {
        this.streamid = streamid;
    }

    public long getNodeid() {
        return nodeid;
    }

    public void setNodeid(long nodeid) {
        this.nodeid = nodeid;
    }

    public Timestamp getEventtimegmt() {
        return eventtimegmt;
    }

    public void setEventtimegmt(Timestamp eventtimegmt) {
        this.eventtimegmt = eventtimegmt;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    

}
