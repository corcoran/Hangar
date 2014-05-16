/*
 * Copyright © 2014 Jeff Corcoran
 *
 * This file is part of Hangar.
 *
 * Hangar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hangar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hangar.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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
    private int order;
    private int widget_order;
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

    public void setOrder(int order) { this.order = order; }

    public int getOrder() { return order; }

    public void setWidgetOrder(int widget_order) { this.widget_order = widget_order; }

    public int getWidgetOrder() { return widget_order; }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return name;
    }
}