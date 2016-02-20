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
import java.util.Collection;
import java.util.UUID;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class Device implements Serializable {
    
    private UUID id;
    private String logicalName;
    private Collection<Device> subDevices;

    public Device() {
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public void setLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

    public Collection<Device> getSubDevices() {
        return subDevices;
    }

    public void setSubDevices(Collection<Device> subDevices) {
        this.subDevices = subDevices;
    }
   
    
    
}
