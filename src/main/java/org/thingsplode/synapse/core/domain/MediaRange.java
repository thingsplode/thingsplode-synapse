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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.thingsplode.synapse.util.Util;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class MediaRange {

    private final List<MediaType> range = new ArrayList<>();

    /**
     * Parses a Content-Type or Accept header into an ordered List of
     * {@link MediaType} instances, which in turn can be used to determine which
     * media type is most appropriate for serialization.
     *
     * @param mediaRange
     */
    public MediaRange(String mediaRange) {
        if (!Util.isEmpty(mediaRange)) {
            Arrays.asList(mediaRange.split("\\s*,\\s*")).forEach(mtSegment -> {
                range.add(new MediaType(mtSegment));
            });
        }
    }

    public int size() {
        return range.size();
    }

    public List<MediaType> getMediaTypes() {
        return range;
    }
    
    public List<MediaType> copyOfRange() {
        return Collections.unmodifiableList(range);
    }
    
    public MediaType get(int index){
        return range.get(index);
    }

    /**
     * Given a List of supported MediaRanges and requested MediaRanges, returns
     * the single best match in Content-Type header format.
     *
     * @param supportedRanges an ordered List of supported MediaRanges.
     * @param requestedRanges an ordered List of MediaRanges that the client
     * desires.
     * @return the single best MediaRange match in Content-Type header format
     * (String). Or null if no match found.
     */
    public static String getBestMatch(List<MediaType> supportedRanges, List<MediaType> requestedRanges) {
        List<WeightedMatch> matches = new ArrayList<>();

        supportedRanges.stream().map((supportedRange) -> getWeightedMatch(supportedRange, requestedRanges)).filter((m) -> (m != null)).forEach((m) -> {
            matches.add(m);
        });

        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0).mediaType.asMediaType();
        }

        Collections.sort(matches);
        return matches.get(0).mediaType.asMediaType();
    }

    /**
     * Iterates the requested MediaRanges to determine how well the single
     * supported MediaRange matches.
     *
     * @param supportedRange
     * @param requestedRanges
     * @return a WeightedMatch
     */
    private static WeightedMatch getWeightedMatch(MediaType supportedRange, List<MediaType> requestedRanges) {
        int maxRank = -1;
        MediaType bestMatch = null;

        for (MediaType requestedRange : requestedRanges) {
            int rank = supportedRange.rankAgainst(requestedRange);

            if (rank > maxRank) {
                maxRank = rank;
                bestMatch = supportedRange;
            }
        }

        return (maxRank == -1 ? null : new WeightedMatch(bestMatch, maxRank));
    }

    protected static class WeightedMatch
            implements Comparable<WeightedMatch> {

        MediaType mediaType;
        int rank;

        public WeightedMatch(MediaType range, int rank) {
            this.mediaType = range;
            this.rank = rank;
        }

        @Override
        public boolean equals(Object that) {
            if (that == null) {
                return false;
            }

            if (this.getClass().isAssignableFrom(that.getClass())) {
                return (compareTo((WeightedMatch) that) == 0);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode() + rank + mediaType.hashCode();
        }

        /**
         * Reverse-rank natural ordering.
         *
         * @param that
         */
        @Override
        public int compareTo(WeightedMatch that) {
            int rankSign = (that.rank - this.rank);

            if (rankSign == 0) {
                return (int) ((that.mediaType.getQvalue() - this.mediaType.getQvalue()) * 10);
            }

            return rankSign;
        }
    }
}
