/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All rights reserved.
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
 *
 */

package org.craftercms.studio.impl.v2.service.security;

import org.craftercms.studio.api.v1.service.activity.ActivityService;
import org.craftercms.studio.api.v1.util.StudioConfiguration;
import org.craftercms.studio.api.v2.dal.GroupDAO;
import org.craftercms.studio.api.v2.dal.UserDAO;
import org.craftercms.studio.api.v2.service.security.AuthenticationChain;
import org.craftercms.studio.api.v2.service.security.AuthenticationProvider;
import org.craftercms.studio.api.v2.service.security.internal.UserServiceInternal;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.craftercms.engine.targeting.impl.TargetedContentStoreAdapter.logger;

public class AuthenticationChainImpl implements AuthenticationChain {

    private List<AuthenticationProvider> authentitcationChain;

    private String chainConfigLocation;
    private UserServiceInternal userServiceInternal;
    private ActivityService activityService;
    private StudioConfiguration studioConfiguration;
    private UserDAO userDao;
    private GroupDAO groupDao;

    public void init() {
        Resource resource = new ClassPathResource(chainConfigLocation);
        try (InputStream in = resource.getInputStream()) {
            Yaml yaml = new Yaml();
            Iterable iterable = yaml.loadAll(in);
            Iterator<AuthenticationProvider> iterator = iterable.iterator();
            authentitcationChain = new ArrayList<AuthenticationProvider>();
            while (iterator.hasNext()) {
                authentitcationChain.add(iterator.next());
            }
            logger.debug("Loaded authentication chain configuration from location: " + authentitcationChain);
        } catch (IOException e) {
            logger.error("Failed to load authentication chain configuration from: " + authentitcationChain);
        }
    }

    @Override
    public boolean doAuthenticate(HttpServletRequest request, HttpServletResponse response) {
        boolean authenticated = false;
        Iterator<AuthenticationProvider> iterator = authentitcationChain.iterator();
        while (iterator.hasNext() || !authenticated) {
            AuthenticationProvider authProvider = iterator.next();
            authenticated = authProvider.doAuthenticate(request, response, this);
        }
        return authenticated;
    }

    public UserServiceInternal getUserServiceInternal() {
        return userServiceInternal;
    }

    public void setUserServiceInternal(UserServiceInternal userServiceInternal) {
        this.userServiceInternal = userServiceInternal;
    }

    public ActivityService getActivityService() {
        return activityService;
    }

    public void setActivityService(ActivityService activityService) {
        this.activityService = activityService;
    }

    public StudioConfiguration getStudioConfiguration() {
        return studioConfiguration;
    }

    public void setStudioConfiguration(StudioConfiguration studioConfiguration) {
        this.studioConfiguration = studioConfiguration;
    }

    public UserDAO getUserDao() {
        return userDao;
    }

    public void setUserDao(UserDAO userDao) {
        this.userDao = userDao;
    }

    public GroupDAO getGroupDao() {
        return groupDao;
    }

    public void setGroupDao(GroupDAO groupDao) {
        this.groupDao = groupDao;
    }

    public String getChainConfigLocation() {
        return chainConfigLocation;
    }

    public void setChainConfigLocation(String chainConfigLocation) {
        this.chainConfigLocation = chainConfigLocation;
    }
}
