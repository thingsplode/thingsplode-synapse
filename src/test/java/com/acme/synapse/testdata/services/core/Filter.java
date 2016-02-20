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

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class Filter implements Serializable {
    
    private String query;
    private Integer page;    
    private Integer pageSize;

    public Filter() {
    }
    
    public Filter(String query, Integer page, Integer pageSize) {
        this.query = query;
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * Get the value of pageSize
     *
     * @return the value of pageSize
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Set the value of pageSize
     *
     * @param pageSize new value of pageSize
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }


    /**
     * Get the value of page
     *
     * @return the value of page
     */
    public Integer getPage() {
        return page;
    }

    /**
     * Set the value of page
     *
     * @param page new value of page
     */
    public void setPage(Integer page) {
        this.page = page;
    }


    /**
     * Get the value of query
     *
     * @return the value of query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Set the value of query
     *
     * @param query new value of query
     */
    public void setQuery(String query) {
        this.query = query;
    }

}
