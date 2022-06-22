package com.comptel.database;

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


    private static final String SELECT_ALL_FROM_EL = "select EVENTID,KEY,MESSAGE,EVENTTYPE,STREAMID,NODEID,HOST"
            + " from el_events where EVENTID > ?  ORDER BY EVENTID ASC";

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
     * 
     * @param startId
     * @param offset
     * @param componentName "EM10GeneratorComponent"
     * @param moduleName EM10Generator
     * @param hostServer
     * @return
     * @throws SQLException 
     */
    public List<ELEvent> selectAllFromEL(long startId, int offset) throws SQLException {
        LOGGER.fine(String.format("%s", SELECT_ALL_FROM_EL));
        LOGGER.info("eventid: " + startId);
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
