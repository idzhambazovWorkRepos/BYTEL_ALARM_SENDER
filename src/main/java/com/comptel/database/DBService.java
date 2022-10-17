package com.comptel.database;

import com.nokia.calm.model.ELEvent;

import java.sql.*;
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


    //Aggregation

//    private static final String SELECT_ALL_FROM_EL = " SELECT DISTINCT ON (key) EVENTID,KEY,MESSAGE,EVENTTYPE,STREAMID,NODEID " +
//            "FROM el_events WHERE EXTRACT(epoch from eventtimesystem) > ? AND EXTRACT(epoch from eventtimesystem) <= ? " +
//            "ORDER BY key, eventtimesystem DESC";


    // Only one with max EVENTID
//    private static final String SELECT_ALL_FROM_EL = "SELECT MAX(EVENTID) AS EVENTID,KEY,MESSAGE,EVENTTYPE,STREAMID,NODEID,HOST"
//   + " FROM el_events where extract(epoch from eventtimesystem) > ? AND extract(epoch from eventtimesystem) <= ? " +
//   "GROUP BY KEY,MESSAGE,EVENTTYPE,STREAMID,NODEID,HOST ";


     private static final String SELECT_ALL_FROM_EL = "SELECT EVENTID,KEY,MESSAGE,EVENTTYPE,STREAMID,NODEID,HOST"
             + " FROM el_events where extract(epoch from eventtimesystem) > ? AND extract(epoch from eventtimesystem) <= ? ";


    private static final String SELECT_LAST_EVENT = "SELECT CASE WHEN RESULT.time_in_sec IS NULL THEN RESULT.CUR_TIME ELSE " +
            "RESULT.time_in_sec END epoch_time FROM (SELECT extract(epoch FROM now()::timestamp(0)) -1 AS CUR_TIME ,extract(epoch FROM " +
            "(SELECT eventtimesystem FROM (SELECT eventtimesystem,row_number() OVER (ORDER BY eventtimesystem DESC) AS ROW_NUM " +
            "FROM (SELECT DISTINCT eventtimesystem FROM el_events WHERE extract(epoch from eventtimesystem) > ?) AS res) AS foo " +
            "WHERE ROW_NUM='2')) AS time_in_sec) as RESULT";


    public DBService(DBConnectionParams dbConnectionParams) {
        Locale.setDefault(Locale.ENGLISH);
        //As datasourcefactory will generate an id from the params we passed, we are sending
        //simply "null" as the id.
        dataSource = DataSourceFactory.getInstance().createDataSource(null, 
                        dbConnectionParams.getUserName(), dbConnectionParams.getPassword(), 
                        dbConnectionParams.getUrl(), dbConnectionParams.getDriver());
    }

    /**
     * 
     * @param oldEvent
     * @param newEvent
     * @param offset
     * @return
     * @throws SQLException 
     */
    public List<ELEvent> selectAllFromEL(long oldEvent, long newEvent, int offset) throws SQLException {
        LOGGER.info(String.format("%s", SELECT_ALL_FROM_EL));
        long startTime = System.nanoTime();
        List<ELEvent> eventsList = new ArrayList<>();
        ResultSet rs = null;
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(SELECT_ALL_FROM_EL);
            statement.setLong(1, oldEvent);
            statement.setLong(2, newEvent);
            rs = statement.executeQuery();
                while (rs.next() && offset > 0) {
                    ELEvent event = new ELEvent();
                    event.setEventid(rs.getLong("eventid"));
                    LOGGER.log(Level.INFO, "select: processing alarm: {0}", event.getEventid());
                    event.setKey(rs.getString("KEY"));
                    event.setMessage(truncateAndAppend3dots(rs.getString("MESSAGE")));
                    event.setEventtype(rs.getString("EVENTTYPE"));
                    event.setStreamid(rs.getLong("STREAMID"));
                    event.setNodeid(rs.getLong("NODEID"));
                    eventsList.add(event);
                    offset--;
                    LOGGER.info("current size of the events list:" + eventsList.size());
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


    public long selectLastEvent( long oldEvent) throws SQLException {
        LOGGER.fine(String.format("%s", SELECT_LAST_EVENT));
        ResultSet rs = null;
        Connection connection = null;
        PreparedStatement statement = null;
        Long time = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(SELECT_LAST_EVENT);
            statement.setLong(1, oldEvent);

            rs = statement.executeQuery();
            while (rs.next()) {
                time =  rs.getLong("epoch_time");
                LOGGER.info("LastEvent: " + time);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred for '" + SELECT_LAST_EVENT +"' query", e);
            throw e;
        } finally {
            closeConnectionResources(connection, statement, rs);
        }
        return time;
    }

    protected static String truncateAndAppend3dots(String message) {
        //Check EL_EVENTS for eventids 3158826 & 3158825 for a long message
        if (message.length() > 500) {
            return StringUtils.left(message, 500) + "...";
        } else {
            return message;
        }
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
}
