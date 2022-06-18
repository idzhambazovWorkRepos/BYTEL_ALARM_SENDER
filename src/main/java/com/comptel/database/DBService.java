package com.comptel.database;

//import com.comptel.cc.event.client.vo.AlarmEvent;
//import com.comptel.cc.event.client.vo.AlarmSeverity;
import com.nokia.calm.model.ELEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author cpt2vot
 */
public class DBService {

    private static final Logger LOGGER = Logger.getLogger(DBService.class.getCanonicalName());

    private DataSource dataSource;
    private PreparedStatement statement = null;
    private Connection connection = null;



    private static final String SELECT_ALL_FROM_EL = "select EVENTID,KEY,MESSAGE,EVENTTYPE,STREAMID,NODEID,HOST"
            + " from el_events where EVENTID > ?  ORDER BY EVENTID ASC";
    private static final String SELECT_FROM_EL = "select EVENTID,KEY,MESSAGE,EVENTTYPE,STREAMID,NODEID,HOST"
            + " from el_events where EVENTID > ? and HOST = ? and EVENTTYPE != 'I' and KEY != 'MULTINE' ORDER BY EVENTID ASC";
    
    private static final String SELECT_BY_EVENT_ID = 
            "select NODEID, STREAMID from el_events where EVENTID = ?";
    
    private static final String UPDATE_STREAM_AND_NODE_NAME_EM = 
            "update NOTIFICATION set origin_component=?, origin_module=? where object_id=?";    
    
    private static final String SELECT_NULL_ORIGINMODULE_ORIGINCOMPONENT_FROM_EM = 
            "select object_id,additional_id from notification "
            + "where ? < object_id AND origin_module = ? AND origin_component = ?"
            + " AND additional_id is not null order by object_id asc";
    
    private static final String SELECT_LASTPROCESSED_ADDITIONAL_ID_FROM_EM = 
            "select MAX(TO_NUMBER(ADDITIONAL_ID)) from notification where ORIGIN_HOST = ?";    
    
    private static final String SELECT_STREAM_NAME_FROM_EL_STREAMS = 
                                    "select NAME from EL_STREAMS where STREAMID = ?";   
    
    private static final String SELECT_NODE_NAME_FROM_EL_NODES = 
                                    "select distinct NAME from EL_NODES where NODEID = ?";
    
    private static final String SELECT_GROUP_FROM_EM = 
            "select count(1) from notification where acknowledged = 0 "
            + "and NOTIFICATION_CODE = ? and ORIGIN_MODULE = ? and ORIGIN_COMPONENT = ? "
//            + "and RECEIVE_TIME > ? - ?/(24*60*60)";
            + "and ORIGIN_HOST = ? and RECEIVE_TIME > sysdate - ?/(24*60*60)";
    
    private static final String SELECT_GROUP_FROM_EM1 = 
            "select count(1) from notification where acknowledged = 0 "
            + "and NOTIFICATION_CODE = ? and ORIGIN_HOST = ? "
            + "and RECEIVE_TIME > sysdate - ?/(24*60*60)";
    
    private static final String SELECT_GROUP_FROM_EM2 = 
            "select count(1) from notification where acknowledged = 0 "
            + "and NOTIFICATION_CODE = ? and MESSAGE like ? and ORIGIN_HOST = ? "
            + "and RECEIVE_TIME > sysdate - ?/(24*60*60)";
    
    private static final String SELECT_GROUP_FROM_EM3 = 
            "select count(1) from notification where acknowledged = 0 "
            + "and NOTIFICATION_CODE = ? and ORIGIN_HOST = ? "
            + "and RECEIVE_TIME > sysdate - ?/(24*60*60) and ORIGIN_MODULE = ? ";
    
    private static final String SELECT_GROUP_FROM_EM4 = 
            "select count(1) from notification where acknowledged = 0 "
            + "and NOTIFICATION_CODE = ? and ORIGIN_HOST = ? "
            + "and RECEIVE_TIME > sysdate - ?/(24*60*60) and ORIGIN_MODULE = ? "
            + "and ORIGIN_COMPONENT = ? ";
    
    private static final String SELECT_GROUP_FROM_EM5 = 
            "select count(1) from notification where acknowledged = 0 "
            + "and NOTIFICATION_CODE = ? and MESSAGE like ? and ORIGIN_HOST = ? "
            + "and RECEIVE_TIME > sysdate - ?/(24*60*60) and ORIGIN_MODULE = ? ";
    
    private static final String SELECT_GROUP_FROM_EM6 = 
            "select count(1) from notification where acknowledged = 0 "
            + "and NOTIFICATION_CODE = ? and MESSAGE like ? and ORIGIN_HOST = ? "
            + "and RECEIVE_TIME > sysdate - ?/(24*60*60) and ORIGIN_MODULE = ? "
            + "and ORIGIN_COMPONENT = ? ";
    
    private static final String UPDATE_ACKUSER_ON_EL = 
            "UPDATE el_events SET ACKUSER=? WHERE eventid=?";

    public DBService(DBConnectionParams dbConnectionParams) {
        Locale.setDefault(Locale.ENGLISH);
        //As datasourcefactory will generate an id from the params we passed, we are sending
        //simply "null" as the id.
        dataSource = DataSourceFactory.getInstance().createDataSource(null, 
                        dbConnectionParams.getUserName(), dbConnectionParams.getPassword(), 
                        dbConnectionParams.getUrl(), dbConnectionParams.getDriver());
    }
    
    /**
     * Selects MAX(ADDITIONAL_ID) and uses that value as the last processed alarm's eventid
     */
    public long selectMaxAdditionalId(String notifHost) throws SQLException {
        
        long maxAdditionalId = 0l;
        long startTime = System.nanoTime();
        ResultSet rs = null;
        PreparedStatement statement = null;
        Connection connection = null;

        try {
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement(SELECT_LASTPROCESSED_ADDITIONAL_ID_FROM_EM);
            statement.setString(1, notifHost);
            rs = statement.executeQuery();
            if(rs.next()) {
                if (rs.getString(1) != null) {
                    maxAdditionalId = Long.parseLong(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for '"
                    +SELECT_LASTPROCESSED_ADDITIONAL_ID_FROM_EM+"' query", e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime)/1e6;
            LOGGER.fine("QueryDuration(SELECT_LASTPROCESSED_ADDITIONAL_ID_FROM_EM): " + queryDuration);
        }
        
        return maxAdditionalId;
    }    
    
    /**
     * 
     * @param startId
     * @param offset
     * @param componentName "EM10GeneratorComponent"
     * @param moduleName EM10Generator
     * @param hostServer
     * @return
     * @throws SQLException 
     */
/*    public List<ELEvent> select(long startId, int offset, String moduleName, String componentName,
            String hostServer) throws SQLException {
        
        assert offset > 0;
        List<ELEvent> eventList = new ArrayList<ELEvent>();
        long startTime = System.nanoTime();
        PreparedStatement statement = null;
        Connection connection = null;
        ResultSet rs = null;

        LOGGER.log(Level.INFO, "select: select events from EventLink starting from: {0}", offset);

        try {
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement(SELECT_FROM_EL);
            statement.setLong(1, startId);
            statement.setString(2, hostServer);
            rs = statement.executeQuery();
            while(rs.next() && offset > 0) {
                ELEvent event = new ELEvent();
                event.setEventid(rs.getLong("EVENTID"));
                LOGGER.log(Level.INFO, "select: processing alarm: {0}", event.getEventid());
                event.setKey(rs.getString("KEY"));
                event.setMessage(truncateAndAppend3dots(rs.getString("MESSAGE")));
                event.setEventtype(rs.getString("EVENTTYPE"));
                event.setStreamid(rs.getLong("STREAMID"));
                event.setStreamName(selectStreamNameFromEL_STREAMS(event.getStreamid()));
                event.setNodeid(rs.getLong("NODEID"));
                event.setNodeName(selectNodeNameFromEL_NODES(event.getNodeid()));
                
                eventList.add(event);
                offset--;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for SELECT query", e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime)/1e6;
            LOGGER.log(Level.FINE, "QueryDuration: {0}", queryDuration);
        }
        
        return eventList;
    }*/

    public List<ELEvent> selectAllFromEL(long startId, int offset) throws SQLException {
        LOGGER.fine(String.format("%s", SELECT_ALL_FROM_EL));
        LOGGER.info("EVENTID: " + startId);
        long startTime = System.nanoTime();
        List<ELEvent> eventsList = new ArrayList<>();

        ResultSet rs = null;
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(SELECT_ALL_FROM_EL);
            statement.setLong(1, startId);
            rs = statement.executeQuery();
                while (rs.next() && offset > 0) {
                    ELEvent event = new ELEvent();
                    event.setEventid(rs.getLong("eventid"));
                    LOGGER.log(Level.INFO, "select: processing alarm: {0}", event.getEventid());
                    event.setKey(rs.getString("KEY"));
                    event.setMessage(truncateAndAppend3dots(rs.getString("MESSAGE")));
                    event.setEventtype(rs.getString("EVENTTYPE"));
                    event.setStreamid(rs.getLong("STREAMID"));
                    //event.setStreamName(selectStreamNameFromEL_STREAMS(event.getStreamid()));
                    event.setNodeid(rs.getLong("NODEID"));
                    //event.setNodeName(selectNodeNameFromEL_NODES(event.getNodeid()));
                    eventsList.add(event);
                    offset--;
                    LOGGER.info("CURRENT LIST SIZE:" + eventsList.size());
                }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for '" + SELECT_ALL_FROM_EL +"' query", e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime)/1e6;
            LOGGER.fine("QueryDuration(SELECT_ALL_FROM_EL): " + queryDuration+"ms");
        }

        return eventsList;
    }

    
    public ELEvent selectByEventId(long event_id) throws SQLException {
        long startTime = System.nanoTime();
        PreparedStatement statement = null;
        Connection connection = null;
        ResultSet rs = null;

        ELEvent elevent = null;
        try {
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement(SELECT_BY_EVENT_ID);
            statement.setLong(1, event_id);
            rs = statement.executeQuery();
            if (rs.next()) {
                elevent = new ELEvent();
                elevent.setEventid(event_id);
                elevent.setNodeid(rs.getLong("NODEID"));
                elevent.setStreamid(rs.getLong("STREAMID"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for SELECT_BY_EVENT_ID", e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime)/1e6;
            LOGGER.fine("QueryDuration(SELECT_BY_EVENT_ID): " + queryDuration);
        }
        
        return elevent;
    }

    protected static String truncateAndAppend3dots(String message) {
        //Check EL_EVENTS for eventids 3158826 & 3158825 for a long message
        if (message.length() > 500) {
            return StringUtils.left(message, 500) + "...";
        } else {
            return message;
        }
    }
    
    public List<EMEvent> selectNullOriginModuleAndOriginComponent(long start_object_id, int offset
            , String origin_module, String origin_component) 
            throws SQLException {
        
        assert offset > 0;
        LOGGER.fine(String.format("%s [%d, %s, %s]", SELECT_NULL_ORIGINMODULE_ORIGINCOMPONENT_FROM_EM,
                                                start_object_id, origin_module, origin_component) );        
        List<EMEvent> emeventList = new ArrayList<EMEvent>();
        long startTime = System.nanoTime();
        PreparedStatement statement = null;
        Connection connection = null;
        ResultSet rs = null;

        try {
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement(SELECT_NULL_ORIGINMODULE_ORIGINCOMPONENT_FROM_EM);
            statement.setLong  (1, start_object_id);
            statement.setString(2, origin_module);
            statement.setString(3, origin_component);
            rs = statement.executeQuery();
            while(rs.next() && offset > 0) {
                EMEvent emevent = new EMEvent();
                emevent.setAdditional_id(rs.getString("ADDITIONAL_ID"));
                emevent.setObject_id(rs.getLong("OBJECT_ID"));
                
                emeventList.add(emevent);
                offset--;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, String.format("Error occurred for '%s'", 
                                        SELECT_NULL_ORIGINMODULE_ORIGINCOMPONENT_FROM_EM), e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime)/1e6;
            LOGGER.fine("QueryDuration(SELECT_NULL_ORIGINMODULE_ORIGINCOMPONENT_FROM_EM): " 
                            + queryDuration);
        }
        
        return emeventList;
    }
    
    public int updateStreamAndNodeName(long object_id, String origin_component, String origin_module) 
            throws SQLException {

        LOGGER.fine(String.format("%s [%d, %s, %s]", UPDATE_STREAM_AND_NODE_NAME_EM,
                object_id, origin_component, origin_module) );
        long startTime = System.nanoTime();
        int updatedCount = 0;
        PreparedStatement statement = null;
        Connection connection = null;
        ResultSet rs = null;

        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(UPDATE_STREAM_AND_NODE_NAME_EM);

            statement.setString(1, origin_component);
            statement.setString(2, origin_module);
            statement.setLong  (3, object_id);
            updatedCount = statement.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for '" + UPDATE_STREAM_AND_NODE_NAME_EM + "' query", e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime) / 1e6;
            LOGGER.fine("QueryDuration(UPDATE_STREAM_AND_NODE_NAME_EM): " + queryDuration + "ms");
        }

        return updatedCount;
    }   

    public String selectStreamNameFromEL_STREAMS(long streamid) 
            throws SQLException {
        String name = "";
        LOGGER.info(String.format("%s with STREAMID=%d", SELECT_STREAM_NAME_FROM_EL_STREAMS, streamid));
        long startTime = System.nanoTime();
        PreparedStatement statement = null;
        Connection connection = null;
        ResultSet rs = null;

        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(SELECT_STREAM_NAME_FROM_EL_STREAMS);
            statement.setLong(1, streamid);
            rs = statement.executeQuery();
            if(rs.next()) {
                name = rs.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for '"+SELECT_STREAM_NAME_FROM_EL_STREAMS+"' query", e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime)/1e6;
            LOGGER.fine("QueryDuration(SELECT_STREAM_NAME_FROM_EL_STREAMS): " + queryDuration);
        }
        
        return name;
    }

    public String selectNodeNameFromEL_NODES(long nodeid) 
            throws SQLException {
        String name = "";
        LOGGER.info(String.format("%s with NODEID=%d", SELECT_NODE_NAME_FROM_EL_NODES, nodeid));
        long startTime = System.nanoTime();
        PreparedStatement statement = null;
        Connection connection = null;
        ResultSet rs = null;

        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(SELECT_NODE_NAME_FROM_EL_NODES);
            statement.setLong(1, nodeid);
            rs = statement.executeQuery();
            if(rs.next()) {
                name = rs.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for '"+SELECT_NODE_NAME_FROM_EL_NODES+"' query", e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime)/1e6;
            LOGGER.fine("QueryDuration(SELECT_NODE_NAME_FROM_EL_NODES): " + queryDuration);
        }
        
        return name;
    }    
    
//    public Boolean selectGroupFromEM(String alarmKey, String streamName, String nodeName, int timeLimit, java.sql.Date sqlDate) 
    public Boolean selectGroupFromEM(String alarmKey, String streamName, String nodeName, int timeLimit, String notifHost) 
            throws SQLException {

        long startTime = System.nanoTime();

        Boolean state = false;
        String result = "";
        PreparedStatement statement = null;
        Connection connection = null;
        ResultSet rs = null;

        try {
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement(SELECT_GROUP_FROM_EM);
            statement.setString(1, alarmKey);
            statement.setString(2, streamName);
            statement.setString(3, nodeName);
            statement.setString(4, notifHost);
//            statement.setDate(4, sqlDate);
//            statement.setInt(5, timeLimit);
            statement.setInt(5, timeLimit);
            rs = statement.executeQuery();
            LOGGER.finest("Alarm key " + alarmKey + " for stream " + streamName + " for node " 
//                    + nodeName + " alarm date " + sqlDate + " timeLimit " + timeLimit);
                    + nodeName + " for host " + notifHost + " timeLimit " + timeLimit);
            LOGGER.finest("Query is " + statement.toString());
            if (rs.next()) {
                result = rs.getString(1);
            
                if (result != null && ! result.equals("0")) {
                    // There is a group made for this alarm. So better not send the alarm...
                    LOGGER.info("Group for the alarm is found so don't send the alarm.");
                    state = true;
                }
            }
            LOGGER.info("Result of the query is " + result);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for SELECT query", e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime)/1e6;
            LOGGER.fine("QueryDuration: " + queryDuration);
        }

        return state;
    }

    public Boolean selectGrpFromEM(PreparedStatement stmnt) 
            throws SQLException {

        long startTime = System.nanoTime();
        ResultSet rs = null;

        Boolean state = false;
        String result = "";
        try {
            rs = stmnt.executeQuery();
            if (rs.next()) {
                result = rs.getString(1);
            
                if (result != null && ! result.equals("0")) {
                    // There is a group made for this alarm. So better not send the alarm...
                    LOGGER.info("Group for the alarm is found so don't send the alarm.");
                    state = true;
                }
            }
            LOGGER.log(Level.INFO, "Result of the query is {0}", result);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for SELECT query", e);
            throw e;
        } finally {
            closeConnectionResources(connection, stmnt, rs);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime)/1e6;
            LOGGER.log(Level.FINE, "QueryDuration: {0}", queryDuration);
        }

        return state;
    }
    
    
    private void closeConnectionResources(Connection connection, Statement statement, ResultSet resultSet) {

        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "", e);
            }
        }

        if (statement != null) {
            try {
                statement.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "", e);
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "", e);
            }
        }
    }
    public void setConnection () throws SQLException {
        connection = this.dataSource.getConnection();
    }
    
    public PreparedStatement prepareStatement(String alarmKey, String notifHost, 
            int timeLimit) throws SQLException {
        statement = connection.prepareStatement(SELECT_GROUP_FROM_EM1);
        statement.setString(1, alarmKey);
        statement.setString(2, notifHost);
        statement.setInt(3, timeLimit);

        LOGGER.log(Level.FINEST,"Alarm key {0} for host {1} and timeLimit {2}", 
                new Object[]{alarmKey, notifHost, timeLimit});
        LOGGER.log(Level.FINEST, "Query is {0}", statement.toString());

        return statement;
    }
    
    public PreparedStatement prepareStatement(String alarmKey, String message, 
            String notifHost, int timeLimit) throws SQLException {
        statement = connection.prepareStatement(SELECT_GROUP_FROM_EM2);
        statement.setString(1, alarmKey);
        statement.setString(2, message);
        statement.setString(3, notifHost);
        statement.setInt(4, timeLimit);

        LOGGER.log(Level.FINEST,"Alarm key {0} containing message {1} for host {2} "
                + "and timeLimit {3}", new Object[]{alarmKey, message, notifHost, timeLimit});

        return statement;
    }
    
    public PreparedStatement prepareStatement(String alarmKey, String notifHost, 
            int timeLimit, String streamName) throws SQLException {
        statement = connection.prepareStatement(SELECT_GROUP_FROM_EM3);
        statement.setString(1, alarmKey);
        statement.setString(2, notifHost);
        statement.setInt(3, timeLimit);
        statement.setString(4, streamName);

        LOGGER.log(Level.FINEST,"Alarm key {0} for host {1} and timeLimit {2}"
                + " for stream {3}", new Object[]{alarmKey, notifHost, timeLimit, streamName});
        LOGGER.log(Level.FINEST, "Query is {0}", statement.toString());

        return statement;
    }
    
    public PreparedStatement prepareStatement(String alarmKey, String notifHost, 
            int timeLimit, String streamName, String nodeName) throws SQLException {
        statement = connection.prepareStatement(SELECT_GROUP_FROM_EM4);
        statement.setString(1, alarmKey);
        statement.setString(2, notifHost);
        statement.setInt(3, timeLimit);
        statement.setString(4, streamName);
        statement.setString(5, nodeName);

        LOGGER.log(Level.FINEST,"Alarm key {0} for host {1} and timeLimit {2} for stream {3} "
                + "for node {4} ", new Object[]{alarmKey, notifHost, timeLimit, streamName, nodeName});
        LOGGER.log(Level.FINEST, "Query is {0}", statement.toString());

        return statement;
    }

    public PreparedStatement prepareStatement(String alarmKey, String message, 
            String notifHost, int timeLimit, String streamName) throws SQLException {
        statement = connection.prepareStatement(SELECT_GROUP_FROM_EM5);
        statement.setString(1, alarmKey);
        statement.setString(2, message);
        statement.setString(3, notifHost);
        statement.setInt(4, timeLimit);
        statement.setString(5, streamName);

        LOGGER.log(Level.FINEST,"Alarm key {0} containing message {1} for host {2} and timeLimit {3} "
                + "for stream {4} ", new Object[]{alarmKey, message, notifHost, timeLimit, streamName});
        LOGGER.log(Level.FINEST, "Query is {0}", statement.toString());

        return statement;
    }

    public PreparedStatement prepareStatement(String alarmKey, String message, 
            String notifHost, int timeLimit, String streamName, String nodeName) throws SQLException {
        statement = connection.prepareStatement(SELECT_GROUP_FROM_EM6);
        statement.setString(1, alarmKey);
        statement.setString(2, message);
        statement.setString(3, notifHost);
        statement.setInt(4, timeLimit);
        statement.setString(5, streamName);
        statement.setString(6, nodeName);

        LOGGER.log(Level.FINEST,"Alarm key {0} containing message {1} for host {2} and timeLimit {3} "
                + "for stream {4} for node {5}", new Object[]{alarmKey, message, notifHost, timeLimit, 
                    streamName, nodeName});
        LOGGER.log(Level.FINEST, "Query is {0}", statement.toString());

        return statement;
    }
    
    public void updateAckuser(String ackuser, long eventid) throws SQLException {
        LOGGER.fine(String.format("%s with ACKUSER=%s, EVENTID=%d",
                UPDATE_ACKUSER_ON_EL, ackuser, eventid));
        long startTime = System.nanoTime();
        
        ResultSet rs = null;
        Connection connection = null;
        PreparedStatement statement = null;
        int updatedCount = 0;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(UPDATE_ACKUSER_ON_EL);
            statement.setString(1, ackuser);
            statement.setLong(2, eventid);
            
            updatedCount = statement.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for '"+UPDATE_ACKUSER_ON_EL+"' query", e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }
        
        if (LOGGER.isLoggable(Level.FINE)) {
            long endTime = System.nanoTime();
            double queryDuration = (endTime - startTime)/1e6;
            LOGGER.fine("QueryDuration(UPDATE_ACKUSER_ON_EL): " + queryDuration);
        }
    }
}
