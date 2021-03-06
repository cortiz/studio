/*
 * Crafter Studio Web-content authoring solution
 * Copyright (C) 2007-2017 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.studio.impl.v1.service.security;

import org.craftercms.studio.api.v1.constant.DmConstants;
import org.craftercms.studio.api.v1.constant.StudioConstants;
import org.craftercms.studio.api.v1.dal.Group;
import org.craftercms.studio.api.v1.dal.SiteFeed;
import org.craftercms.studio.api.v1.dal.User;
import org.craftercms.studio.api.v1.exception.SiteNotFoundException;
import org.craftercms.studio.api.v1.exception.security.*;
import org.craftercms.studio.api.v1.log.Logger;
import org.craftercms.studio.api.v1.log.LoggerFactory;
import org.craftercms.studio.api.v1.service.activity.ActivityService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapEntryIdentification;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.craftercms.studio.api.v1.util.StudioConfiguration.*;
import static org.springframework.ldap.query.LdapQueryBuilder.query;

public class DbWithLdapExtensionSecurityProvider extends DbSecurityProvider {

    private final static Logger logger = LoggerFactory.getLogger(DbWithLdapExtensionSecurityProvider.class);

    @Override
    public String authenticate(String username, String password) throws BadCredentialsException, AuthenticationSystemException {

        // Mapper for user data if user is successfully authenticated
        AuthenticatedLdapEntryContextMapper<User> mapper = new AuthenticatedLdapEntryContextMapper<User>() {
            @Override
            public User mapWithContext(DirContext dirContext, LdapEntryIdentification ldapEntryIdentification) {
                try {
                    // User entry - extract attributes
                    DirContextOperations dirContextOperations = (DirContextOperations)dirContext.lookup(ldapEntryIdentification.getRelativeName());
                    Attributes attributes = dirContextOperations.getAttributes();
                    String emailAttribName = studioConfiguration.getProperty(SECURITY_LDAP_USER_ATTRIBUTE_EMAIL);
                    String firstNameAttribName = studioConfiguration.getProperty(SECURITY_LDAP_USER_ATTRIBUTE_FIRST_NAME);
                    String lastNameAttribName = studioConfiguration.getProperty(SECURITY_LDAP_USER_ATTRIBUTE_LAST_NAME);
                    String siteIdAttribName = studioConfiguration.getProperty(SECURITY_LDAP_USER_ATTRIBUTE_SITE_ID);
                    String groupNameAttribName = studioConfiguration.getProperty(SECURITY_LDAP_USER_ATTRIBUTE_GROUP_NAME);
                    Attribute emailAttrib = attributes.get(emailAttribName);
                    Attribute firstNameAttrib = attributes.get(firstNameAttribName);
                    Attribute lastNameAttrib = attributes.get(lastNameAttribName);
                    Attribute siteIdAttrib = attributes.get(siteIdAttribName);
                    Attribute groupNameAttrib = attributes.get(groupNameAttribName);


                    User user = new User();
                    user.setGroups(new ArrayList<>());
                    user.setActive(1);
                    user.setUsername(username);

                    if (emailAttrib != null && emailAttrib.get() != null) {
                        user.setEmail(emailAttrib.get().toString());
                    } else {
                        logger.error("No LDAP attribute " + emailAttribName + " found for username " + username + ". User will " +
                                     "not be imported into DB.");
                        return null;
                    }
                    if (firstNameAttrib != null && firstNameAttrib.get() != null) {
                        user.setFirstname(firstNameAttrib.get().toString());
                    } else {
                        logger.warn("No LDAP attribute " + firstNameAttribName + " found for username " + username);
                    }
                    if (lastNameAttrib != null && lastNameAttrib.get() != null) {
                        user.setLastname(lastNameAttrib.get().toString());
                    } else {
                        logger.warn("No LDAP attribute " + lastNameAttribName + " found for username " + username);
                    }

                    String siteId;
                    SiteFeed siteFeed;
                    if (siteIdAttrib != null && siteIdAttrib.get() != null) {
                        Map<String, Object> params = new HashMap<>();
                        NamingEnumeration siteIdValues = siteIdAttrib.getAll();
                        while (siteIdValues.hasMore()) {
                            Object siteIdObj = siteIdValues.next();
                            if (siteIdObj != null) {
                                siteId = siteIdObj.toString();
                                params.put("siteId", siteId);
                                siteFeed = siteFeedMapper.getSite(params);
                                if (siteFeed != null) {
                                    if (groupNameAttrib != null && groupNameAttrib.size() > 0) {
                                        NamingEnumeration groupAttribValues = groupNameAttrib.getAll();
                                        while (groupAttribValues.hasMore()) {
                                            Object groupNameObj = groupAttribValues.next();
                                            if (groupNameObj != null) {
                                                String groupName = groupNameObj.toString();
                                                Group g = new Group();
                                                g.setName(groupName);
                                                g.setExternallyManaged(1);
                                                g.setDescription("Externally managed group");
                                                g.setSiteId(siteFeed.getId());
                                                g.setSite(siteFeed.getSiteId());
                                                user.getGroups().add(g);
                                            }
                                        }
                                    } else {
                                        logger.warn("No LDAP attribute " + groupNameAttribName + " found for username " + username);
                                    }
                                }
                            }
                        }
                    } else {
                        logger.warn("No LDAP attribute " + siteIdAttribName + " found for username " + username);
                    }

                    return user;
                } catch (NamingException e) {
                    logger.error("Error getting details from LDAP for username " + username, e);

                    return null;
                }
            }
        };

        // Create ldap query to authenticate user
        LdapQuery ldapQuery = query().where(studioConfiguration.getProperty(SECURITY_LDAP_USER_ATTRIBUTE_USERNAME)).is(username);
        User user;
        try {
            user = ldapTemplate.authenticate(ldapQuery, password, mapper);
        } catch (EmptyResultDataAccessException e) {
            logger.info("User " + username + " not found with external security provider. Trying to authenticate against studio database");
            // When user not found try to authenticate against studio database
            return super.authenticate(username, password);
        } catch (CommunicationException e) {
            logger.info("Failed to connect with external security provider. Trying to authenticate against studio database");
            // When user not found try to authenticate against studio database
            return super.authenticate(username, password);
        } catch (AuthenticationException e) {
            logger.error("Authentication failed with the LDAP system", e);

            throw new BadCredentialsException();
        } catch (Exception e) {
            logger.error("Authentication failed with the LDAP system", e);

            throw new AuthenticationSystemException("Authentication failed with the LDAP system", e);
        }

        if (user != null) {
            // When user authenticated against LDAP, upsert user data into studio database
            boolean toRet = true;
            if (super.userExists(username)) {
                try {
                    boolean success = updateUserInternal(user.getUsername(), user.getFirstname(), user.getLastname(), user.getEmail());
                    if (success) {
                        ActivityService.ActivityType activityType = ActivityService.ActivityType.UPDATED;
                        Map<String, String> extraInfo = new HashMap<String, String>();
                        extraInfo.put(DmConstants.KEY_CONTENT_TYPE, StudioConstants.CONTENT_TYPE_USER);
                        activityService.postActivity("N/A", user.getUsername(), "N/A", activityType, ActivityService.ActivitySource.UI, extraInfo);
                    }
                } catch (UserNotFoundException e) {
                    logger.error("Error updating user " + username + " with data from external authentication provider", e);

                    throw new AuthenticationSystemException("Error updating user " + username +
                                                            " with data from external authentication provider", e);
                }
            } else {
                try {
                    boolean success = createUser(user.getUsername(), password, user.getFirstname(), user.getLastname(), user.getEmail(), true);
                    if (success) {
                        ActivityService.ActivityType activityType = ActivityService.ActivityType.CREATED;
                        Map<String, String> extraInfo = new HashMap<String, String>();
                        extraInfo.put(DmConstants.KEY_CONTENT_TYPE, StudioConstants.CONTENT_TYPE_USER);
                        activityService.postActivity("N/A", user.getUsername(), "N/A", activityType, ActivityService.ActivitySource.UI, extraInfo);
                    }
                } catch (UserAlreadyExistsException e) {
                    logger.error("Error adding user " + username + " from external authentication provider", e);

                    throw new AuthenticationSystemException("Error adding user " + username + " from external authentication provider", e);
                }
            }
            for (Group group : user.getGroups()) {
                try {
                    upsertUserGroup(group.getSite(), group.getName(), user.getUsername());
                } catch (GroupAlreadyExistsException | SiteNotFoundException | UserNotFoundException |
                    UserAlreadyExistsException | GroupNotFoundException e) {
                    logger.error("Failed to upsert user groups data from LDAP", e);
                }
            }

            String token = createToken(user);
            storeSessionTicket(token);
            storeSessionUsername(username);

            return token;
        } else {
            logger.error("Failed to retrieve LDAP user details");

            throw new AuthenticationSystemException("Failed to retrieve LDAP user details");
        }
    }

    private boolean updateUserInternal(String username, String firstName, String lastName, String email) throws UserNotFoundException {
        if (!userExists(username)) {
            throw new UserNotFoundException();
        } else {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("username", username);
            params.put("firstname", firstName);
            params.put("lastname", lastName);
            params.put("email", email);
            params.put("externallyManaged", 1);
            securityMapper.updateUser(params);
            return true;
        }
    }

    private boolean upsertUserGroup(String siteId, String groupName, String username) throws GroupAlreadyExistsException,
        SiteNotFoundException, UserNotFoundException, UserAlreadyExistsException, GroupNotFoundException {
        if (!groupExists(siteId, groupName)) {
           createGroup(groupName, "Externally managed group", siteId, true);
        }
        if (!userExistsInGroup(siteId, groupName, username)) {
            addUserToGroup(siteId, groupName, username);
        }
        return true;
    }

    public LdapTemplate getLdapTemplate() { return ldapTemplate; }
    public void setLdapTemplate(LdapTemplate ldapTemplate) { this.ldapTemplate = ldapTemplate; }

    public ActivityService getActivityService() { return activityService; }
    public void setActivityService(ActivityService activityService) { this.activityService = activityService; }

    protected LdapTemplate ldapTemplate;
    protected ActivityService activityService;
}
