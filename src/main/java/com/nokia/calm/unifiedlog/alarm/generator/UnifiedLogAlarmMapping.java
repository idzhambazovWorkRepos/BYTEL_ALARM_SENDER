/*
 * Nokia Software 2020
 * Created at: 19-Dec-2020 12:12:54
 * Created by: Caglar Kilincoglu
 */
package com.nokia.calm.unifiedlog.alarm.generator;

import com.nokia.logging.alarm.AlarmTask;
import com.nokia.logging.alarm.PerceivedSeverity;

public class UnifiedLogAlarmMapping {

    private String errorCode;
    private int alarmId;
    private AlarmTask alarmTask;
    private PerceivedSeverity severity;
    private String key;
    private String text;
    private String eventType;
    private String probableCause;

    public String getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public int getAlarmId() {
        return this.alarmId;
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public AlarmTask getAlarmTask() {
        return this.alarmTask;
    }

    public void setAlarmTask(AlarmTask alarmTask) {
        this.alarmTask = alarmTask;
    }

    public PerceivedSeverity getSeverity() {
        return this.severity;
    }

    public void setSeverity(PerceivedSeverity severity) {
        this.severity = severity;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getEventType() {
        return this.eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getProbableCause() {
        return this.probableCause;
    }

    public void setProbableCause(String probableCause) {
        this.probableCause = probableCause;
    }

    public String toString() {
        return "errorCode=" + this.errorCode + ", alarmId=" + this.alarmId + ", alarmTask="
               + this.alarmTask + ", severity=" + this.severity + ", key=" + this.key
               + ", text=" + this.text + ", eventType=" + this.eventType + ", probableCause="
               + this.probableCause;
    }
}
