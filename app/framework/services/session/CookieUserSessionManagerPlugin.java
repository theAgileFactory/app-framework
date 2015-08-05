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
package framework.services.session;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.play.StorageHelper;

import play.Logger;
import play.mvc.Http.Context;
import play.mvc.Http.Session;

/**
 * The implementation of the {@link IUserSessionManagerPlugin} interface.<br/>
 * This one makes use of the Play framework {@link Session}. It also implements
 * a session expiration mechanism. Such implementation is based on PAC4J model.
 * 
 * @author Pierre-Yves Cloux
 */
public class CookieUserSessionManagerPlugin implements IUserSessionManagerPlugin {
    private static Logger.ALogger log = Logger.of(CookieUserSessionManagerPlugin.class);
    private String profileAttributeName;

    /**
     * @param profileAttribute
     */
    private CookieUserSessionManagerPlugin() {
        super();
    }

    @Override
    public String getUserSessionId(Context ctx) {
        // get the session id
        final String sessionId = ctx.session().get(Pac4jConstants.SESSION_ID);
        if (log.isDebugEnabled()) {
            log.debug("Session id found : " + sessionId);
        }
        if (StringUtils.isNotBlank(sessionId)) {
            // get the user profile
            final CommonProfile profile = StorageHelper.getProfile(sessionId);
            if (log.isDebugEnabled()) {
                log.debug("Found profile : " + profile);
            }

            if (profile != null) {
                if (!isCustomProfileAttributeName()) {
                    return profile.getId();
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.valueOf(profile.getAttributes() != null ? profile.getAttributes().getClass() + " : " + profile.getAttributes()
                            : "NO ATTRIBUTES"));
                    log.debug(String.valueOf(profile.getAttribute(getProfileAttributeName()) != null ? profile.getAttribute(getProfileAttributeName())
                            .getClass() + " : " + profile.getAttribute(getProfileAttributeName()) : "NO VALUE"));
                }

                if (profile.getAttribute(getProfileAttributeName()) != null) {
                    Object attribute = profile.getAttribute(getProfileAttributeName());
                    if (attribute != null && attribute instanceof List) {
                        attribute = ((List<?>) attribute).get(0);
                    }
                    return attribute != null ? String.valueOf(attribute) : null;
                }
            } else {
                // User session is not null but profile is null
                // Clear the session
                ctx.session().remove(Pac4jConstants.SESSION_ID);
            }
        }
        return null;
    }

    private boolean isCustomProfileAttributeName() {
        return profileAttributeName != null;
    }

    private String getProfileAttributeName() {
        return profileAttributeName;
    }

    @Override
    public void setUserProfileAttributeName(String profileAttributeName) {
        this.profileAttributeName = profileAttributeName;
    }
}
