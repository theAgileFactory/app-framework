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
package framework.services.api.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.tuple.Pair;

import framework.services.api.commons.ApiMethod;
import framework.services.api.commons.ApiSignatureException;

/**
 * Protocol details.
 * 
 * The authentication keys:
 * <ul>
 * <li>Shared Secret</li>
 * <li>Application Key</li>
 * </ul>
 * 
 * Each request (only HTTPS) must be associated with two headers:
 * <ul>
 * <li>X-bizdock-timestamp : a timestamp (Epoch time format)</li>
 * <li>X-bizdock-application : the "Application Key"</li>
 * <li>X-bizdock-signature : a signature</li>
 * </ul>
 * 
 * Here is how to create a signature:
 * <p>
 * #PROTOCOL_VERSION#"+BASE64(SHA256(SHARED_SECRET+"+"+"+"+REQUEST_METHOD+"+"+
 * REQUEST_URL+"+"+REQUEST_BODY+"+"+ EPOCH_TIMESTAMP))
 * </p>
 * 
 * Here are the parameters:
 * <ul>
 * <li>PROTOCOL_VERSION : the version of the signature protocol</li>
 * <li>SHARED_SECRET : your shared secret</li>
 * <li>REQUEST_METHOD : the method of the API call (see {@link ApiMethod}</li>
 * <li>REQUEST_URL : the full URL of the request</li>
 * <li>REQUEST_BODY : the body of the request (only for POST requests)</li>
 * <li>EPOCH_TIMESTAMP : the Epoch time of your system</li>
 * </ul>
 * 
 * In order to prevent replay, the signature contains a TimeStamp
 * (EPOCH_TIMESTAMP). To get the current BizDock time, you can connect using the
 * BizDock time API. This is the only API for which a timestamp not aligned with
 * the BizDock time will be accepted.
 * 
 * @author Pierre-Yves Cloux
 *
 */
public class SignatureGeneratorImpl implements ISignatureGenerator {
    private long timeCorrection = 0;
    private String sharedSecret;
    private String applicationKey;
    private String hashAlgorithm = "SHA-512";
    private int protocolVersion = 1;

    /**
     * Creates a SignatureGenerator.
     * 
     * @param sharedSecret
     *            your shared secret
     * @param applicationKey
     *            your application key
     */
    public SignatureGeneratorImpl(String sharedSecret, String applicationKey) {
        this.sharedSecret = sharedSecret;
        this.applicationKey = applicationKey;
    }

    /**
     * Creates a SignatureGenerator with the specified "referenceTime". This
     * allows to take into account a possible time difference between the
     * calling client and the BizDock server.
     * 
     * @param sharedSecret
     *            your shared secret
     * @param applicationKey
     *            your application key
     * @param referenceTime
     *            the bizDock time (retrieved by a call to the BizDock time API)
     */
    public SignatureGeneratorImpl(String sharedSecret, String applicationKey, long referenceTime) {
        this(sharedSecret, applicationKey);
        timeCorrection = System.currentTimeMillis() - referenceTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * signature.client.ISignatureGeneratorImpl#getRequestSignature(signature
     * .commons.ApiMethod, java.lang.String, byte[], long)
     */
    public byte[] getRequestSignature(ApiMethod method, String url, byte[] body, long timeStamp) throws ApiSignatureException {
        StringBuffer cipheredPart = new StringBuffer();
        cipheredPart.append(getSharedSecret()).append('+');
        cipheredPart.append(method.name()).append('+');
        cipheredPart.append(url).append('+');
        if (method.equals(ApiMethod.POST) || method.equals(ApiMethod.PUT)) {
            cipheredPart.append(new String(body)).append('+');
        }
        cipheredPart.append(timeStamp);
        try {
            MessageDigest md = MessageDigest.getInstance(getHashAlgorithm());
            byte[] digest = md.digest(cipheredPart.toString().getBytes());
            ByteArrayOutputStream signature = new ByteArrayOutputStream();
            signature.write(("#" + getProtocolVersion() + "#").getBytes());
            signature.write(Base64.encodeBase64(digest, false, true));
            return signature.toByteArray();
        } catch (NoSuchAlgorithmException e) {
            throw new ApiSignatureException(e);
        } catch (IOException e) {
            throw new ApiSignatureException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * signature.client.ISignatureGeneratorImpl#getRequestSignature(signature
     * .commons.ApiMethod, java.lang.String, byte[])
     */
    public Pair<byte[], Long> getRequestSignature(ApiMethod method, String url, byte[] body) throws ApiSignatureException {
        long timeStamp = System.currentTimeMillis() + getTimeCorrection();
        return Pair.of(getRequestSignature(method, url, body, timeStamp), timeStamp);
    }

    /*
     * (non-Javadoc)
     * 
     * @see signature.client.ISignatureGeneratorImpl#getTimeCorrection()
     */
    public long getTimeCorrection() {
        return timeCorrection;
    }

    /**
     * Set the time correction (in ms) to be applied between your server time
     * and the BizDock time. Basically it is : (BizDock Epoch Time) - (Your
     * server Epoch time)
     * 
     * @param timeCorrection
     */
    public void setTimeCorrection(long timeCorrection) {
        this.timeCorrection = timeCorrection;
    }

    /*
     * (non-Javadoc)
     * 
     * @see signature.client.ISignatureGeneratorImpl#getSharedSecret()
     */
    public String getSharedSecret() {
        return sharedSecret;
    }

    /*
     * (non-Javadoc)
     * 
     * @see signature.client.ISignatureGeneratorImpl#getApplicationKey()
     */
    public String getApplicationKey() {
        return applicationKey;
    }

    /*
     * (non-Javadoc)
     * 
     * @see signature.client.ISignatureGeneratorImpl#getProtocolVersion()
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    /*
     * (non-Javadoc)
     * 
     * @see signature.client.ISignatureGeneratorImpl#getHashAlgorithm()
     */
    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
}
