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

    public static final String DATE_FORMAT = "dd/MM/yyyy";

    /**
     * Constant to be used for scale of percentage.
     */
    public static final int PERCENTAGE_SCALE = 3;

    /**
     * Constant to be used for scale of years.
     */
    public static final int YEAR_SCALE = 5;

    /**
     * Constant to be used for scale of verions.
     */
    public static final int VERSION_SCALE = 4;

    /**
     * Constant to be used for the precision of decimal numbers.<br/>
     * Such numbers are typically to be used for amounts.
     */
    public static final int BIGNUMBER_PRECISION = 12;

    /**
     * Constant to be used for the scale of decimal numbers.<br/>
     * Such numbers are typically to be used for amounts
     */
    public static final int BIGNUMBER_SCALE = 2;

    /**
     * Small string typically used for a short name (login, project name, etc.)
     */
    public static final int SMALL_STRING = 32;

    /**
     * Small string typically used for a human readable name (name of a person)
     */
    public static final int MEDIUM_STRING = 64;

    /**
     * Large string typically used for specific text identifier (e-mail address,
     * token, etc.)
     */
    public static final int LARGE_STRING = 256;

    /**
     * Very large string usually containing a description.
     */
    public static final int VLARGE_STRING = 1500;

    /**
     * Extra large string usually containing a description.
     */
    public static final int XLARGE_STRING = 2500;

    /**
     * Length for a variable that should store a language code (ISO639-1)
     */
    public static final int LANGUAGE_CODE = 2;

    /**
     * Length for a variable that should store a phone number (pattern is :
     * +41217879070)
     */
    public static final int PHONE_NUMBER = 30;

    /**
     * Length for a variable that should store a currency code (ISO 4217)
     */
    public static final int CURRENCY_CODE = 3;

    /**
     * IPv4 address as a String
     */
    public static final int IPv4_ADDRESS = 15;

    /**
     * IPv6 address as a String
     */
    public static final int IPv6_ADDRESS = 39;

    /**
     * An e-mail validation pattern
     */
    public static final String EMAIL_VALIDATION_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    /**
     * Login/uid validation pattern
     */
    public static final String UID_VALIDATION_PATTERN = "^[a-zA-Z0-9_\\.@-]*$";

    /**
     * Minimal length for an UID
     */
    public static final int MIN_UID_LENGTH = 2;

    /**
     * Minimal length for an NAME
     */
    public static final int MIN_NAME_LENGTH = 2;
}
