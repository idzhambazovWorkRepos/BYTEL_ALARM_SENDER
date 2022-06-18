/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.comptel.database;

/**
 *
 * @author cpt2vot
 */
public class DBConnectionParams {

    private String url;
    private String userName;
    private String password;
    private String driver;

    public DBConnectionParams(String url, String userName, String password, String driver) {
        this.url = url;
        this.userName = userName;
        this.password = password;
        this.driver = driver;
    }
    
    
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }
}
