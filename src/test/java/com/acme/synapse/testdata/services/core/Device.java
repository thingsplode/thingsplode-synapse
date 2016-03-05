/*
 * Copyright 2016 tamas.csaba@gmail.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.acme.synapse.testdata.services.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *
 * @author tamas.csaba@gmail.com
 */
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "jtype")
public class Device implements Serializable {

    private long id;
    private String logicalName;
    private Integer treshold;
    private List<Device> subDevices;
    private Address address;
    private Tuple<Date, Date> servicePeriod;

    public static Device createTestDevice() {
        Device d = new Device(112l, "test device logical name", Integer.SIZE);
        d.address = new Address("some street", "SOME CNTRY", 5050);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONDAY, -1);
        cal.add(Calendar.DATE, 1);
        Date lastMonthFirstDate = cal.getTime();
        cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
        Date lastMonthsLastday = cal.getTime();
        d.servicePeriod = new Tuple<>(lastMonthFirstDate, lastMonthsLastday);
        d.subDevices = new ArrayList<>();
        d.subDevices.add(new Device(114l, "subdevice 1 logical name", Integer.MAX_VALUE));
        d.subDevices.add(new Device(115l, "subdevice 2 logical name", Integer.MIN_VALUE));
        return d;
    }

    public Device() {
    }

    public Device(long id, String logicalName, Integer treshold) {
        this.id = id;
        this.logicalName = logicalName;
        this.treshold = treshold;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

    public Integer getTreshold() {
        return treshold;
    }

    public void setTreshold(Integer treshold) {
        this.treshold = treshold;
    }

    public Address getAddress() {
        return address;
    }

    public Tuple<Date, Date> getServicePeriod() {
        return servicePeriod;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void setServicePeriod(Tuple<Date, Date> servicePeriod) {
        this.servicePeriod = servicePeriod;
    }
    

    public List<Device> getSubDevices() {
        return subDevices;
    }

    public void setSubDevices(List<Device> subDevices) {
        this.subDevices = subDevices;
    }

}
