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

import java.text.ChoiceFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * define the signed number format
 * 
 * @author Johann Kohler
 * 
 */
class SignedNumberFormat extends NumberFormat {

    private static final long serialVersionUID = 1L;
    private NumberFormat nf;
    private ChoiceFormat cf = new ChoiceFormat(new double[] { 0.0, ChoiceFormat.nextDouble(0.0) }, new String[] { "", "+" });

    public SignedNumberFormat(NumberFormat nf) {
        this.nf = nf;
    }

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        return toAppendTo.append(cf.format(number)).append(nf.format(number));
    }

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return toAppendTo.append(cf.format(number)).append(nf.format(number));
    }

    @Override
    public Number parse(String source, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }
}
