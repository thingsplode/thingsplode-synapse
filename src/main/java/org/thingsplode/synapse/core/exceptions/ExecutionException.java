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
package org.thingsplode.synapse.core.exceptions;

import org.thingsplode.synapse.core.domain.HttpStatus;
import org.thingsplode.synapse.core.domain.Request;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class ExecutionException extends SynapseException {

    public ExecutionException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ExecutionException(Request.RequestHeader header, Exception e) {
        super("Service execution error on path: " + header.getUri().getPath() + " and method: " + header.getMethod() + " with error -> " + e.getMessage() + ". Exception Type: " + e.getClass(), HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
}
