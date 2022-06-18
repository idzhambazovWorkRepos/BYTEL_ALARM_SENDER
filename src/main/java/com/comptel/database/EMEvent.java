package com.comptel.database;

/**
 *
 * @author cpt2vot
 */
public class EMEvent {
    private long object_id;
    private String additional_id;
    private String origin_component;
    private String origin_module;

    public long getObject_id() {
        return object_id;
    }

    public void setObject_id(long object_id) {
        this.object_id = object_id;
    }

    public String getAdditional_id() {
        return additional_id;
    }

    public void setAdditional_id(String additional_id) {
        this.additional_id = additional_id;
    }

    public String getOrigin_component() {
        return origin_component;
    }

    public void setOrigin_component(String origin_component) {
        this.origin_component = origin_component;
    }

    public String getOrigin_module() {
        return origin_module;
    }

    public void setOrigin_module(String origin_module) {
        this.origin_module = origin_module;
    }
    
    
}
