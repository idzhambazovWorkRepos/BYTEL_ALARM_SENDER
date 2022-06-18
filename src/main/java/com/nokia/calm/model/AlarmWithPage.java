/*
 * Nokia Software 2020
 * Created at: 27-Sep-2021 01:34:10
 * Created by: Caglar Kilincoglu
 */

package com.nokia.calm.model;

import java.util.List;

/**
 */
public class AlarmWithPage {
    public List<Alarm> items;
    public int totalRecords;
    public int pageNum;
    public int pageSize;
    public List<Link> links;

    public List<Alarm> getItems() {
        return items;
    }

    public void setItems(List<Alarm> items) {
        this.items = items;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
    
    

}
