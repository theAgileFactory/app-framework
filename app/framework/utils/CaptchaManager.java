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

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import framework.commons.IFrameworkConstants;
import play.Logger;
import play.cache.Cache;

/**
 * A class which manages a captcha.
 * 
 * @author Pierre-Yves Cloux
 */
public class CaptchaManager {
    private static Logger.ALogger log = Logger.of(CaptchaManager.class);
    private static final int CAPTCHA_DURATION_SECONDS = 60;

    public CaptchaManager() {
    }

    /**
     * Check that the specified text is the one produced by the captcha and
     * stored into the cache
     * 
     * @param uuid
     *            a unique captcha id
     * @param text
     *            the captcha text
     * @return true if the captcha is valid
     */
    public static boolean validateCaptcha(String uuid, String text) {
        String retreivedText = (String) Cache.get(IFrameworkConstants.CAPTCHA_CACHE_PREFIX + uuid);
        if (log.isDebugEnabled()) {
            log.debug("CHECK " + uuid + "=" + text);
        }
        return retreivedText != null && retreivedText.toLowerCase().equals(StringUtils.deleteWhitespace(text.toLowerCase()));
    }

    private static CaptchaParameters getCaptchaParameters(String uuid) {
        CaptchaParameters captchaParameters = new CaptchaParameters();
        Cache.set(IFrameworkConstants.CAPTCHA_CACHE_PREFIX + uuid, captchaParameters.text, CAPTCHA_DURATION_SECONDS);
        if (log.isDebugEnabled()) {
            log.debug("SET " + uuid + "=" + captchaParameters.text);
        }
        return captchaParameters;
    }

    /**
     * Creates a captcha.<br/>
     * The method returns an image and associates the captcha text with the uuid
     * specified as a parameter.
     * 
     * @param uuid
     *            a unique id
     * @return a byte array
     * @throws IOException
     */
    public static byte[] createCaptcha(String uuid) throws IOException {
        CaptchaParameters captcha = getCaptchaParameters(uuid);
        BufferedImage image = new BufferedImage(captcha.width, captcha.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = image.createGraphics();
        graphics2D.setColor(Color.WHITE);
        Paint oldPaint = graphics2D.getPaint();
        graphics2D.setPaint(captcha.gradient);
        graphics2D.fillRect(0, 0, captcha.width, captcha.height);
        graphics2D.setPaint(oldPaint);

        TextLayout tl = new TextLayout(captcha.displayedText, new Font(captcha.fontType, captcha.fontStyle, captcha.fontSize),
                new FontRenderContext(null, false, false));
        AffineTransform textAt = new AffineTransform();
        textAt.translate(0, (float) tl.getBounds().getHeight());
        Shape textShape = tl.getOutline(textAt);

        AffineTransform transformer = new AffineTransform();
        transformer.setToIdentity();
        transformer.translate(captcha.width / 2, captcha.height / 2);
        transformer.rotate(Math.toRadians(captcha.rotate));
        transformer.shear(captcha.shearX, captcha.shearY);

        AffineTransform toCenterAt = new AffineTransform();
        toCenterAt.concatenate(transformer);
        toCenterAt.translate(-(textShape.getBounds().width / 2) + captcha.positionX, -(textShape.getBounds().height / 2) + captcha.positionY);
        graphics2D.transform(toCenterAt);

        graphics2D.setColor(Color.DARK_GRAY);
        graphics2D.fill(textShape);
        graphics2D.draw(textShape);

        graphics2D.dispose();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        outputStream.close();
        return outputStream.toByteArray();
    }

    /**
     * A class which contains the parameters for the captcha. These parameters
     * are generated randomly.
     * 
     * @author Pierre-Yves Cloux
     */
    private static class CaptchaParameters {
        private static String[] fontFamilies = { Font.DIALOG, Font.DIALOG_INPUT, Font.MONOSPACED, Font.SANS_SERIF, Font.SERIF };
        private static int[] fontStyles = { Font.BOLD, Font.PLAIN, Font.ITALIC };
        public double rotate;
        public double shearX;
        public double shearY;
        public int fontSize;
        public String fontType;
        public int fontStyle;
        public int width;
        public int height;
        public float positionX;
        public float positionY;
        public GradientPaint gradient;
        public String text;
        private String displayedText;

        public CaptchaParameters() {
            Random random = new Random(System.currentTimeMillis());

            // Create the text (letters or digits except 1,0,O,o,l to avoid
            // ambiguities) and UUID and set the text in cache for one minute
            this.text = RandomStringUtils.random(random.nextInt(2) + 5, "qwertzuipkjhgfdsayxcvbnmQWERTZUIPLKJHGFDSAYXCVBNM23456789");

            // Insert a white space
            int whiteSpaceIndex = random.nextInt(this.text.length() - 1);
            this.displayedText = this.text.substring(0, whiteSpaceIndex) + " " + this.text.substring(whiteSpaceIndex);
            // this.rotate = random.nextFloat() * 20 - 10;
            this.rotate = 0;
            // this.shearX = random.nextFloat() - 0.5;
            this.shearX = 0;
            // this.shearY = random.nextFloat() - 0.5;
            this.shearY = 0;
            this.fontSize = random.nextInt(3) + 16;
            this.height = 60;
            this.width = 120;
            this.positionX = random.nextFloat() * 7 - 5;
            this.positionY = random.nextFloat() * 7 - 5;
            switch (random.nextInt(4)) {
            case 0:
                this.gradient = new GradientPaint(0, 0, Color.GRAY, this.width, this.height, Color.WHITE);
                break;
            case 1:
                this.gradient = new GradientPaint(0, 0, Color.WHITE, this.width, this.height, Color.GRAY);
                break;
            case 2:
                this.gradient = new GradientPaint(this.width, 0, Color.GRAY, 0, this.height, Color.WHITE);
                break;
            case 3:
                this.gradient = new GradientPaint(0, this.height, Color.GRAY, this.width, 0, Color.WHITE);
                break;
            }
            this.fontType = fontFamilies[random.nextInt(fontFamilies.length)];
            this.fontStyle = fontStyles[random.nextInt(fontStyles.length)];
        }
    }
}
