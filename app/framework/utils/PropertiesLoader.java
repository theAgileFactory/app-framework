/*! LICENSE
 *
 * Copyright (c) 2015, The Agile Factory SA and/or its affiliates. All rights
 * reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package framework.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class PropertiesLoader {

    public static Properties loadProperties(InputStream is, String encoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(is, encoding);
        while (true) {
            int temp = isr.read();
            if (temp < 0)
                break;

            char c = (char) temp;
            sb.append(c);
        }

        String inputString = escapifyStr(sb.toString());
        byte[] bs = inputString.getBytes("ISO-8859-1");
        ByteArrayInputStream bais = new ByteArrayInputStream(bs);

        Properties ps = new Properties();
        ps.load(bais);
        return ps;
    }

    private static char hexDigit(char ch, int offset) {
        int val = (ch >> offset) & 0xF;
        if (val <= 9)
            return (char) ('0' + val);

        return (char) ('A' + val - 10);
    }

    private static String escapifyStr(String str) {
        StringBuilder result = new StringBuilder();

        int len = str.length();
        for (int x = 0; x < len; x++) {
            char ch = str.charAt(x);
            if (ch <= 0x007e) {
                result.append(ch);
                continue;
            }

            result.append('\\');
            result.append('u');
            result.append(hexDigit(ch, 12));
            result.append(hexDigit(ch, 8));
            result.append(hexDigit(ch, 4));
            result.append(hexDigit(ch, 0));
        }
        return result.toString();
    }
}
