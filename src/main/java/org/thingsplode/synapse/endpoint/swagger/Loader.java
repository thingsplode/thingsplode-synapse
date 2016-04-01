/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thingsplode.synapse.endpoint.swagger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.regex.Pattern;

/**
 *
 * @author tamas.csaba@gmail.com
 */
public class Loader {

    private static final Pattern INSECURE_URI_PATTERN = Pattern.compile(".*[<>&\"].*");

    public static RandomAccessFile extractResource(String path) throws IOException {
        if (path == null || path.isEmpty() || path.endsWith("/")) {
            return null;
        }

        if (path.contains(File.separator + '.')
                || path.contains('.' + File.separator)
                || path.charAt(0) == '.' || path.charAt(path.length() - 1) == '.'
                || INSECURE_URI_PATTERN.matcher(path).matches()) {
            return null;
        }
        if (!path.contains(".")) {
            return null;
        }
        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("WEB-INF" + path);
        if (is == null) {
            Loader.class.getResourceAsStream("/WEB-INF" + path);
        }
        if (is == null) {
            throw new IOException("Could not found /WEB-INF" + path);
        }

        RandomAccessFile raf = new RandomAccessFile(File.createTempFile("syn", "tmp"), "rwd");
        //byte[] buffer = new byte[8192];
        byte[] buffer = new byte[16384];
        int tmp;

        while ((tmp = is.read(buffer)) != -1) {
            raf.write(buffer, 0, tmp);
        }
        raf.seek(0);
        return raf;
    }
}
