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
package org.thingsplode.synapse.serializers.gson;

import org.thingsplode.synapse.serializers.SynapseSerializer;

/**
 *
 * @author Csaba Tamas
 */
//public class GsonSerializerTest extends AbstractParserTest {
public class GsonSerializerTest {

    private final GsonSerializer serializer = new GsonSerializer(true, null, null);

    //@Override
    public SynapseSerializer<String> getSerializer() {
        return serializer;
    }

}
