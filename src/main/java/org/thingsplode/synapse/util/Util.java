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
package org.thingsplode.synapse.util;

import io.netty.channel.ChannelHandlerContext;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import org.thingsplode.synapse.core.ConnectionContext;
import static org.thingsplode.synapse.endpoint.handlers.RequestHandler.CONNECTION_CTX_ATTR;

/**
 *
 * @author Csaba Tamas
 */
public class Util {

    private static final Pattern p = Pattern.compile(".(?:jpg|gif|png|css|html|zip|txt)$");

    public static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    public static boolean notEmpty(String s) {
        return !isEmpty(s);
    }

    public static boolean addContextProperty(ChannelHandlerContext ctx, String key, String value) {
        ConnectionContext cctx = ctx.channel().attr(CONNECTION_CTX_ATTR).get();
        if (cctx != null) {
            cctx.addProperty(key, value);
            return true;
        } else {
            return false;
        }
    }

    public static Optional<String> getContextProperty(ChannelHandlerContext ctx, String key) {
        ConnectionContext cctx = ctx.channel().attr(CONNECTION_CTX_ATTR).get();
        if (cctx != null) {
            return cctx.getProperty(key);
        } else {
            return Optional.empty();
        }
    }

    /**
     *
     * @param regexp
     * @param stringArray
     * @return
     */
    public static boolean containsAnyOfStrings(String regexp, String[] stringArray) {
        Optional<String> result = Arrays.asList(stringArray).stream().filter(s -> {
            Pattern p = Pattern.compile(regexp);
            return p.matcher(s).find();
        }).findFirst();
        return result.isPresent();
    }

    /**
     *
     * @param regexp
     * @param stringArray
     * @return
     */
    public static boolean containsAll(String regexp, String[] stringArray) {
        long matched = Arrays.asList(stringArray).stream().filter(s -> {
            Pattern p = Pattern.compile(regexp);
            return p.matcher(s).find();
        }).count();
        return stringArray.length == matched;
    }

    /**
     *
     * @param regexPattern
     * @param stringArray
     * @return
     */
    public static boolean containsAll(Pattern regexPattern, String[] stringArray) {
        long matched = Arrays.asList(stringArray).stream().filter(s -> {
            return regexPattern.matcher(s).find();
        }).count();
        return stringArray.length == matched;
    }

    /**
     *
     * @param checkableUrl
     * @return
     */
    public boolean isAcceptedFileExtension(String checkableUrl) {
        return false;
    }
}
