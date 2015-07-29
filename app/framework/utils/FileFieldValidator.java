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

import java.net.MalformedURLException;
import java.net.URL;

import play.libs.F.Tuple;

/**
 * The file field validator if it is required.
 * 
 * @author Johann Kohler
 */
public class FileFieldValidator extends play.data.validation.Constraints.Validator<FileField> {

    @Override
    public boolean isValid(FileField fileField) {

        switch (fileField.getFileType()) {
        case UPLOAD:
            return !fileField.getValue().equals("");
        case URL:
            return isValidUrl(fileField.getValue());
        }

        return false;
    }

    @Override
    public Tuple<String, Object[]> getErrorMessageKey() {
        return null;
    }

    /**
     * Check the validity of an URL.
     * 
     * @param urlStr
     *            the URL to check
     */
    private boolean isValidUrl(String urlStr) {
        try {
            new URL(urlStr);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

}