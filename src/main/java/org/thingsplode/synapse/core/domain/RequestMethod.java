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
package org.thingsplode.synapse.core.domain;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public enum RequestMethod {

    /**
     * The GET method means retrieve whatever information (in the form of an
     * entity) is identified by the Request-URI. If the Request-URI refers to a
     * data-producing process, it is the produced data which shall be returned
     * as the entity in the response and not the source text of the process,
     * unless that text happens to be the output of the process.
     */
    GET,
    /**
     * The POST method is used to request that the origin server accept the
     * entity enclosed in the request as a new subordinate of the resource
     * identified by the Request-URI in the Request-Line.
     */
    POST,
    /**
     * The PUT method requests that the enclosed entity be stored under the
     * supplied Request-URI.
     */
    PUT,
    /**
     * The DELETE method requests that the origin server delete the resource
     * identified by the Request-URI.
     */
    DELETE

}
