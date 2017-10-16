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
package models.framework_models.parent;

/**
 * Interface which references the constants used by the entity objects
 * 
 * @author Pierre-Yves Cloux
 */
public interface IModelConstants {

    String DATE_FORMAT = "dd/MM/yyyy";

    /**
     * Constant to be used for scale of percentage.
     */
    int PERCENTAGE_SCALE = 3;

    /**
     * Constant to be used for scale of years.
     */
    int YEAR_SCALE = 5;

    /**
     * Constant to be used for scale of verions.
     */
    int VERSION_SCALE = 4;

    /**
     * Constant to be used for the precision of decimal numbers.<br/>
     * Such numbers are typically to be used for amounts.
     */
    int BIGNUMBER_PRECISION = 12;

    /**
     * Constant to be used for the scale of decimal numbers.<br/>
     * Such numbers are typically to be used for amounts
     */
    int BIGNUMBER_SCALE = 2;

    /**
     * Small string typically used for a short name (login, project name, etc.)
     */
    int SMALL_STRING = 32;

    /**
     * Small string typically used for a human readable name (name of a person)
     */
    int MEDIUM_STRING = 64;

    /**
     * Large string typically used for specific text identifier (e-mail address,
     * token, etc.)
     */
    int LARGE_STRING = 256;

    /**
     * Very large string usually containing a description.
     */
    int VLARGE_STRING = 1500;

    /**
     * Extra large string usually containing a description.
     */
    int XLARGE_STRING = 2500;

    /**
     * Very extra large string usually containing a long description (consider using text instead).
     */
    int XXLARGE_STRING = 5000;

    /**
     * Length for a variable that should store a language code (ISO639-1)
     */
    int LANGUAGE_CODE = 2;

    /**
     * Length for a variable that should store a phone number (pattern is :
     * +41217879070)
     */
    int PHONE_NUMBER = 30;

    /**
     * Length for a variable that should store a currency code (ISO 4217)
     */
    int CURRENCY_CODE = 3;

    /**
     * IPv4 address as a String
     */
    int IPv4_ADDRESS = 15;

    /**
     * IPv6 address as a String
     */
    int IPv6_ADDRESS = 39;

    /**
     * An e-mail validation pattern
     */
    String EMAIL_VALIDATION_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    /**
     * Login/uid validation pattern
     */
    String UID_VALIDATION_PATTERN = "^[a-zA-Z0-9_\\.@-]*$";

    /**
     * Minimal length for an UID
     */
    int MIN_UID_LENGTH = 2;

    /**
     * Minimal length for an NAME
     */
    int MIN_NAME_LENGTH = 2;
}
