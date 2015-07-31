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

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.UUID;

import framework.commons.IFrameworkConstants;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

/**
 * Provider for assets with version management.
 * 
 * @author Johann Kohler
 * 
 */
public class AssetsProvider {

    private static final String CACHE_PREFIX = IFrameworkConstants.MAF_CACHE_PREFIX + "assets.version.";

    /**
     * Get a local asset by path.
     * 
     * @param assetsRootLocation
     *            the assets root location
     * @param path
     *            the file path of the asset
     */
    public static String local(String assetsRootLocation, String path) {

        String cacheKey = CACHE_PREFIX + path;

        String fileChecksum = (String) Cache.get(cacheKey);

        if (fileChecksum == null) {
            try {
                fileChecksum = getFileChecksumByPath(path);
            } catch (Exception e) {
                Logger.error("AssetsVersion/local: error when getting the checksum for the path " + path, e);
                fileChecksum = UUID.randomUUID().toString();
            }
            Cache.set(cacheKey, fileChecksum);
        }

        return assetsRootLocation + path + "?" + fileChecksum;
    }

    /**
     * Get a local asset by url.
     * 
     * @param url
     *            the relative url of the asset
     */
    public static String local(String url) {

        String cacheKey = CACHE_PREFIX + url;

        String fileChecksum = (String) Cache.get(cacheKey);

        if (fileChecksum == null) {

            try {
                fileChecksum = getFileChecksumByUrl(Play.application().configuration().getString("maf.private.url") + url);
            } catch (Exception e) {
                Logger.error("AssetsVersion/local: error when getting the checksum for the url " + url, e);
                fileChecksum = UUID.randomUUID().toString();
            }
            Cache.set(cacheKey, fileChecksum);
        }

        return url + "?" + fileChecksum;
    }

    private static String getFileChecksumByUrl(String url) throws Exception {
        WSResponse response = WS.url(url).get().get(1000);
        if (response.getStatus() == 200) {
            String fileContent = response.getBody();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            return getFileChecksum(md.digest(fileContent.getBytes()));
        } else {
            throw new Exception("not a 200");
        }
    }

    private static String getFileChecksumByPath(String path) throws Exception {

        MessageDigest md = MessageDigest.getInstance("SHA1");

        InputStream fis = Play.application().resourceAsStream("public/" + path);
        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }

        try {
            fis.close();
        } catch (Exception e) {
        }

        return getFileChecksum(md.digest());

    }

    private static String getFileChecksum(byte[] digest) throws Exception {
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < digest.length; i++) {
            sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

}
