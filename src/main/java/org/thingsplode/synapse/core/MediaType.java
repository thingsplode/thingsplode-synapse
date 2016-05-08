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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author Csaba Tamas
 */
public class MediaType {

    /**
     * "application/json; charset=UTF-8"
     */
    public static final String APPLICATION_JSON = "application/json; charset=UTF-8";
    private static final String MEDIA_TYPE_REGEX = "(\\S+?|\\*)/(\\S+?|\\*)";
    private static final Pattern MEDIA_TYPE_PATTERN = Pattern.compile(MEDIA_TYPE_REGEX);
    private static final String PARAMETER_REGEX = "(\\w+?)(?:\\s*?=\\s*?(\\S+?))";
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(PARAMETER_REGEX);

    private final String name;
    private String type;
    private String subtype;
    private float qvalue = 1.0f;
    private final Map<String, String> parameters = new HashMap<>();

    public MediaType(String segment) {
        if (Util.isEmpty(segment)){
            segment = "text/plain";
        }
        this.name = segment;
        String[] pieces = segment.split("\\s*;\\s*");
        Matcher matcher = MEDIA_TYPE_PATTERN.matcher(pieces[0]);

        if (matcher.matches()) {
            this.type = matcher.group(1);
            this.subtype = matcher.group(2);
        }
        
        for (int i = 1; i < pieces.length; ++i) {
            Matcher p = PARAMETER_PATTERN.matcher(pieces[i]);

            if (p.matches()) {
                String token = p.group(1);
                String value = p.group(2);

                if ("q".equalsIgnoreCase(token)) {
                    this.qvalue = Float.parseFloat(value);
                } else if (value != null) {
                    this.parameters.put(token, value);
                } else {
                    this.parameters.put(token, null);
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public float getQvalue() {
        return qvalue;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return name;
    }

    public String asMediaType() {
        StringBuilder b = new StringBuilder(type);
        b.append("/");
        b.append(subtype);

        parameters.entrySet().stream().map((entry) -> {
            b.append("; ");
            b.append(entry.getKey());
            return entry;
        }).filter((entry) -> (entry.getValue() != null)).forEach((entry) -> {
            b.append("=");
            b.append(entry.getValue());
        });

        return b.toString();
    }

    /**
     *
     * @param that
     * @return -1 if not applicable. Otherwise a rank value >= 0
     */
    public int rankAgainst(final MediaType that) {
        int rank = -1;  // Default is not-applicable.

        if ((this.type.equals(that.type) || "*".equals(this.type) || "*".equals(that.subtype))
                && (this.subtype.equals(that.subtype) || "*".equals(that.subtype) || "*".equals(this.subtype))) {
            rank = 0;  // This media type IS applicable

            if (this.type.equals(that.type)) {
                rank += 100;
            }

            if (this.subtype.equals(that.subtype) && !"*".equals(this.subtype)) {
                rank += 50;
            }

            for (Entry<String, String> entry : parameters.entrySet()) {
                String value = that.parameters.get(entry.getKey());

                if (value != null && value.equals(entry.getValue())) {
                    rank += 2;
                }
            }
        }

        return rank;
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) {
            return false;
        }

        if (this.getClass().isAssignableFrom(that.getClass())) {
            return equals((MediaType) that);
        }

        return false;
    }

    public boolean equals(MediaType that) {
        if (that == null) {
            return false;
        }

        boolean result = (name.equals(that.name) && type.equals(that.type) && subtype.equals(that.subtype));

        if (!result) {
            return false;
        }

        if (qvalue != that.qvalue) {
            return false;
        }

        return parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode() + name.hashCode() + parameters.hashCode() + (int) (qvalue * 10.0);
    }
}
