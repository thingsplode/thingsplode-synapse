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

import org.thingsplode.synapse.core.MediaType;
import org.thingsplode.synapse.core.MediaRange;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Csaba Tamas
 */
public class MediaRangeTest {

    @Test
    public void shouldParseQFactor() {
        MediaRange range = new MediaRange("text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c");
        assertNotNull(range);
        assertEquals(4, range.size());

        MediaType m1 = range.get(0);
        assertEquals("text/plain", m1.asMediaType());
        assertEquals(0.5, m1.getQvalue(), 0.01);
        assertTrue(m1.getParameters().isEmpty());

        MediaType m2 = range.get(1);
        assertEquals("text/html", m2.asMediaType());
        assertEquals(1.0, m2.getQvalue(), 0.01);
        assertTrue(m2.getParameters().isEmpty());

        MediaType m3 = range.get(2);
        assertEquals("text/x-dvi", m3.asMediaType());
        assertEquals(0.8, m3.getQvalue(), 0.01);
        assertTrue(m3.getParameters().isEmpty());

        MediaType m4 = range.get(3);
        assertEquals("text/x-c", m4.asMediaType());
        assertEquals(1.0, m4.getQvalue(), 0.01);
        assertTrue(m4.getParameters().isEmpty());
    }

    @Test
    public void shouldParameters() {
        MediaRange range = new MediaRange("text/*, text/html, text/html;level=1, */*");
        assertNotNull(range);
        assertEquals(4, range.size());

        MediaType m1 = range.get(0);
        assertEquals("text/*", m1.asMediaType());
        assertEquals(1.0, m1.getQvalue(), 0.01);
        assertTrue(m1.getParameters().isEmpty());

        MediaType m2 = range.get(1);
        assertEquals("text/html", m2.asMediaType());
        assertEquals(1.0, m2.getQvalue(), 0.01);
        assertTrue(m2.getParameters().isEmpty());

        MediaType m3 = range.get(2);
        assertEquals("text/html; level=1", m3.asMediaType());
        assertEquals(1.0, m3.getQvalue(), 0.01);
        assertEquals(1, m3.getParameters().size());
        assertEquals("1", m3.getParameters().get("level"));

        MediaType m4 = range.get(3);
        assertEquals("*/*", m4.asMediaType());
        assertEquals(1.0, m4.getQvalue(), 0.01);
        assertTrue(m4.getParameters().isEmpty());
    }

    @Test
    public void shouldParseParametersAndQFactor() {
        MediaRange range = new MediaRange("text/*;q=0.3 , text/html;q=0.7, text/html;q=0.9;level=1,text/html;level=2;q=0.4, */*;q=0.5");
        assertNotNull(range);
        assertEquals(5, range.size());

        MediaType m1 = range.get(0);
        assertEquals("text/*", m1.asMediaType());
        assertEquals(0.3, m1.getQvalue(), 0.01);
        assertTrue(m1.getParameters().isEmpty());

        MediaType m2 = range.get(1);
        assertEquals("text/html", m2.asMediaType());
        assertEquals(0.7, m2.getQvalue(), 0.01);
        assertTrue(m2.getParameters().isEmpty());

        MediaType m3 = range.get(2);
        assertEquals("text/html; level=1", m3.asMediaType());
        assertEquals(0.9, m3.getQvalue(), 0.01);
        assertEquals(1, m3.getParameters().size());
        assertEquals("1", m3.getParameters().get("level"));

        MediaType m4 = range.get(3);
        assertEquals("text/html; level=2", m4.asMediaType());
        assertEquals(0.4, m4.getQvalue(), 0.01);
        assertEquals("2", m4.getParameters().get("level"));

        MediaType m5 = range.get(4);
        assertEquals("*/*", m5.asMediaType());
        assertEquals(0.5, m5.getQvalue(), 0.01);
        assertTrue(m5.getParameters().isEmpty());
    }

    @Test
    public void shouldChooseTextXml() {
        MediaRange supported = new MediaRange("application/xbel+xml, text/xml");
        MediaRange requested = new MediaRange("text/*;q=0.5,*; q=0.1");
        assertEquals("text/xml", MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }

    @Test
    public void shouldChooseTextJson() {
        MediaRange supported = new MediaRange("application/json, text/json");
        MediaRange requested = new MediaRange("image/*;q=0.5,text/*; q=0.1");
        assertEquals("text/json", MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }

    @Test
    public void shouldChooseTextHtml() {
        MediaRange supported = new MediaRange("text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c");
        MediaRange requested = new MediaRange("text/html;q=0.5,text/plain; q=0.1,*/*");
        assertEquals("text/html", MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }

    @Test
    public void shouldChooseTextXC() {
        MediaRange supported = new MediaRange("text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c");
        MediaRange requested = new MediaRange("*/*;q=0.9, text/x-c");
        assertEquals("text/x-c", MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }

    @Test
    public void shouldChooseLevel1() {
        MediaRange supported = new MediaRange("text/*;q=0.3 , text/html;q=0.7, text/html;q=0.9;level=1,text/html;level=2;q=0.4, */*;q=0.5");
        MediaRange requested = new MediaRange("text/*");
        assertEquals("text/html; level=1", MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }

    @Test
    public void shouldChooseApplicationJson() {
        MediaRange supported = new MediaRange("application/json, application/javascript, text/javascript, application/xml, text/xml");
        MediaRange requested = new MediaRange("application/json; application/xml");
        assertEquals("application/json", MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }

    @Test
    public void shouldChooseApplicationJsonWithApplicationStar() {
        MediaRange supported = new MediaRange("application/json, application/javascript, text/javascript, application/xml, text/xml");
        MediaRange requested = new MediaRange("application/*; text/xml");
        assertEquals("application/json", MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }

    @Test
    public void shouldChooseApplicationXml() {
        MediaRange supported = new MediaRange("application/json, application/javascript, text/javascript, application/xml, text/xml");
        MediaRange requested = new MediaRange("application/xml;application/json");
        assertEquals("application/xml", MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }

    @Test
    public void shouldChooseTextJavascriptWithTextStar() {
        MediaRange supported = new MediaRange("application/json, application/javascript, text/javascript, application/xml, text/xml");
        MediaRange requested = new MediaRange("text/*; text/xml");
        assertEquals("text/javascript", MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }

    @Test
    public void shouldChooseApplicationJsonWithStarStar() {
        MediaRange supported = new MediaRange("text/javascript;q=0.8, application/json, application/xml, text/xml");
        MediaRange requested = new MediaRange("*/*");
        assertEquals("application/json", MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }

    @Test
    public void shouldReturnNull() {
        MediaRange supported = new MediaRange("text/javascript, application/json, application/xml, text/xml");
        MediaRange requested = new MediaRange("application/blah, text/plain");
        assertNull(MediaRange.getBestMatch(supported.getMediaTypes(), requested.getMediaTypes()));
    }
}
