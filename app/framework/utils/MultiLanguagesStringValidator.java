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

import play.libs.F.Tuple;

/**
 * The MultiLanguagesString validator: if the field is required then at least
 * one value for a language must be given.
 * 
 * @author Johann Kohler
 * 
 */
public class MultiLanguagesStringValidator extends play.data.validation.Constraints.Validator<Object> {

    @Override
    public boolean isValid(Object object) {

        if (!(object instanceof MultiLanguagesString)) {
            return false;
        }

        MultiLanguagesString s = (MultiLanguagesString) object;

        for (String value : s.getValues()) {
            if (value != null && !value.equals("")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Tuple<String, Object[]> getErrorMessageKey() {
        return null;
    }

}