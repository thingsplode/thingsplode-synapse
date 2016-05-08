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
package org.thingsplode.synapse.serializers.jackson.adapters;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Iterator;
import org.thingsplode.synapse.core.ParameterWrapper;

/**
 *
 * @author Csaba Tamas
 */
public class ParameterWrapperDeserializer extends StdDeserializer<ParameterWrapper> {

    public ParameterWrapperDeserializer() {
        super(ParameterWrapper.class);
    }

    @Override
    public ParameterWrapper deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jp.readValueAsTree();
        if (node != null && node.size() > 0 && node.isContainerNode()) {
            ParameterWrapper pw = ParameterWrapper.create();
            ArrayNode paramsNode = (ArrayNode) node.get("params");
            Iterator<JsonNode> elemIterator = paramsNode.elements();
            while (elemIterator.hasNext()) {
                JsonNode currentNode = elemIterator.next();
                if (currentNode.getNodeType() == JsonNodeType.OBJECT) {
                    try {
                        String paramid = ((ObjectNode) currentNode).get("paramid").asText();
                        String typeName = ((ObjectNode) currentNode).get("type").asText();
                        Class paramType = null;
                        if (null != typeName) switch (typeName) {
                            case "long":
                                paramType = Long.TYPE;
                                break;
                            case "byte":
                                paramType = Byte.TYPE;
                                break;
                            case "short":
                                paramType = Short.TYPE;
                                break;
                            case "int":
                                paramType = Integer.TYPE;
                                break;
                            case "float":
                                paramType = Float.TYPE;
                                break;
                            case "double":
                                paramType = Double.TYPE;
                                break;
                            case "boolean":
                                paramType = Boolean.TYPE;
                                break;
                            case "char":
                                paramType = Character.TYPE;
                                break;
                            default:
                                paramType = Class.forName(typeName);
                                break;
                        }
                        Object parameterObject = jp.getCodec().treeToValue(currentNode.get("value"), paramType);
                        pw.add(paramid, paramType, parameterObject);
                    } catch (ClassNotFoundException ex) {
                        throw new JsonParseException(jp, ex.getMessage());
                    }
                }
            }
            return pw;
        } else {
            return null;
        }
    }

}
