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
package models.framework_models.account;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import models.framework_models.parent.IModelConstants;
import play.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.avaje.ebean.Model;

import framework.services.account.LightAuthenticationAccountReaderPlugin;

/**
 * A class to be used for the light version of BizDock.<br/>
 * It is storing the credentials of the user in order to manage a SSO without
 * depending on an external system.<br/>
 * The password is stored hashed using the MD5 hash.
 * 
 * @author Pierre-Yves Cloux
 */
@Entity
public class Credential extends Model {
    private static final long serialVersionUID = 1157762908933366941L;

    private static final int MAX_LOGIN_ATTEMPT = 3;
    
    private static Logger.ALogger log = Logger.of(Credential.class);

    /**
     * Default finder for the entity class
     */
    public static Finder<Long, Credential> find = new Finder<Long, Credential>(Credential.class);

    @Id
    public Long id;

    public boolean isActive = true;

    @Version
    public Timestamp lastUpdate;
    
    public Timestamp lastLoginDate;

    public int failedLogin;
    

    @Column(length = IModelConstants.LARGE_STRING)
    public String uid;

    @Column(length = IModelConstants.LARGE_STRING)
    public String password;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String firstName;

    @Column(length = IModelConstants.MEDIUM_STRING)
    public String lastName;

    @Column(length = IModelConstants.LARGE_STRING)
    public String fullName;

    @Column(length = IModelConstants.LARGE_STRING)
    public String mail;

    public Credential() {
    }

    public void storePassword(String clearPassword) {
        try {
            this.password = cipher(clearPassword);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Credential getCredentialFromUid(String uid) {
        return find.where().eq("uid", uid).findUnique();
    }

    public static Credential getCredentialFromEmail(String mail) {
        return find.where().eq("mail", mail).findUnique();
    }

    public static List<Credential> getCredentialsFromName(String nameCriteria) {
        if (nameCriteria == null) {
            return null;
        }
        String nameLike = nameCriteria.replace('*', '%');
        return find.where().like("fullName", nameLike).findList();
    }

    public static boolean isCredentialMailAlreadyExist(String mail) {
        return find.where().eq("mail", mail).findRowCount() != 0;
    }

    public static boolean isCredentialUidAlreadyExist(String uid) {
        return find.where().eq("uid", uid).findRowCount() != 0;
    }

    /**
     * After 3 failed login attempts, the account is automatically unactivated
     * 
     * @param uid
     * @param password
     * @return
     */
    public static boolean checkCredentialPassword(String uid, String password) {
    	
        boolean success = false;
        if (StringUtils.isBlank(password))
            return false;
        Credential credential = getCredentialFromUid(uid);
        try {
            success = checkPassword(password, credential.password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (!success) {
            if (credential != null) {
                credential.failedLogin = credential.failedLogin + 1;
                if (credential.failedLogin >= MAX_LOGIN_ATTEMPT) {
                    credential.isActive = false;
                }
                credential.save();
            }
        } else {
        	  credential.failedLogin = 0;
        	  credential.lastLoginDate = Timestamp.valueOf(LocalDateTime.now());
              credential.save();
        }
        return success;
    }

    /**
     * Cipher a password using the specified clear password and the 8bytes salt
     * 
     * @param clearPassword
     *            a clear password
     * @param salt
     *            a 8 bytes salt
     * @return a ciphered password as byte
     * @throws NoSuchAlgorithmException
     */
    public static String cipher(String clearPassword) throws NoSuchAlgorithmException {
        byte[] salt = new byte[8];
        (new Random()).nextBytes(salt);
        return cipher(clearPassword, salt);
    }

    /**
     * Cipher a password using the specified clear password and the 8bytes salt
     * 
     * @param clearPassword
     *            a clear password
     * @param salt
     *            a 8 bytes salt
     * @return a ciphered password as byte
     * @throws NoSuchAlgorithmException
     */
    public static String cipher(String clearPassword, byte[] salt) throws NoSuchAlgorithmException {
        byte[] passwordAsByte = clearPassword.getBytes();
        passwordAsByte = ArrayUtils.addAll(passwordAsByte, salt);
        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(passwordAsByte);
        passwordAsByte = ArrayUtils.addAll(crypt.digest(), salt);
        String targetPassword = new String(Base64.encodeBase64(passwordAsByte));
        return new String(Base64.encodeBase64(("{SSHA}" + targetPassword).getBytes()));
    }

    /**
     * Check the password
     * 
     * @param clearPassword
     *            a clear password
     * @param cipheredPassword
     *            the password as it is ciphered into the database
     * @return true if the password is the same
     * @throws NoSuchAlgorithmException
     */
    public static boolean checkPassword(String clearPassword, String cipheredPassword) throws NoSuchAlgorithmException {
        byte[] decodedPassword = Base64.decodeBase64(cipheredPassword);
        decodedPassword = (new String(decodedPassword)).substring("{SSHA}".length()).getBytes();
        decodedPassword = Base64.decodeBase64(decodedPassword);
        byte[] salt = Arrays.copyOfRange(decodedPassword, decodedPassword.length - 8, decodedPassword.length);
        return Arrays.equals(cipher(clearPassword, salt).getBytes(), cipheredPassword.getBytes());
    }
}
