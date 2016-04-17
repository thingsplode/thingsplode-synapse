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

import java.io.UnsupportedEncodingException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Csaba Tamas
 */
public class UriTest {

    @Test
    public void testUriProcessor() throws UnsupportedEncodingException {
        String us = "this_service/call?parameter=1&param=2";
        Uri uri = new Uri(us);
        Assert.assertEquals("/this_service/call", uri.getPath());
        Assert.assertEquals("parameter=1&param=2", uri.getQuery());
        Assert.assertTrue(uri.getQueryParameters().size() == 2);
    }
}
