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
package org.thingsplode.synapse.endpoint.serializers;

import org.thingsplode.synapse.core.domain.MediaRange;
import org.thingsplode.synapse.core.exceptions.SerializationException;

/**
 *
 * @author tamas.csaba@gmail.com
 * @param <WIREFORMAT>
 */
public interface SynapseSerializer<WIREFORMAT> {

    public MediaRange getSupportedMediaRange();
    
    WIREFORMAT marshall(Object object) throws SerializationException;

    <T> T unMarshall(Class<T> objectType, WIREFORMAT wirecontent) throws SerializationException;
}
