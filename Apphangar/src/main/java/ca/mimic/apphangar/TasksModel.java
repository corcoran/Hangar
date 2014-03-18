package ca.mimic.apphangar;

import java.sql.Timestamp;

public class TasksModel {
    private long id;
    private String name;
    private String icon;
    private String packagename;
    private String classname;
    private int seconds;
    private int launches;
    private Timestamp timestamp;
    private Boolean blacklisted;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPackageName() {
        return packagename;
    }

    public void setPackageName(String packagename) {
        this.packagename = packagename;
    }

    public String getClassName() {
        return classname;
    }

    public void setClassName(String classname) {
        this.classname = classname;
    }

    public int getSeconds() { return seconds; }

    public void setSeconds(int seconds) { this.seconds = seconds; }

    public int getLaunches() { return launches; }

    public void setLaunches(int launches) { this.launches = launches; }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getBlacklisted() { return blacklisted; }

    public void setBlacklisted(Boolean blacklisted) { this.blacklisted = blacklisted; }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return name;
    }
}