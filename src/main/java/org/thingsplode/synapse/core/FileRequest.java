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
package org.thingsplode.synapse.core;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Inbound requests (eg. HTTP requests) first are evaluated as service request (requesting a service execution). If that step is failing, the request is converted
 * to a {@link FileRequest} and passed forward on the pipeline (if the file service is enabled on the Endpoint). If the path describes an actual file, the file will be served.
 * @author Csaba Tamas
 */
@JsonPropertyOrder({ "@msg_type", "header", "body"})
public class FileRequest extends Request<String> {

    public FileRequest() {
    }
    
    public FileRequest(RequestHeader header) {
        super(header);
    }
}
