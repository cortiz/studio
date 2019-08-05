/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.studio.controller.rest.v2;

import javax.activation.MimetypesFileTypeMap;

import org.craftercms.studio.api.v1.exception.ContentNotFoundException;
import org.craftercms.studio.api.v2.service.config.ConfigurationService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that provides the UI with plugin related files
 * @author joseross
 * @since 3.1.1
 */
@RestController
@RequestMapping("/api/2/plugin")
public class PluginController {

    /**
     * The configuration service
     */
    protected ConfigurationService configurationService;

    public PluginController(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * Returns a single file for a given plugin
     */
    @GetMapping("/file")
    public ResponseEntity<Resource> getPluginFile(@RequestParam String siteId, @RequestParam String type,
                                                  @RequestParam String extension, @RequestParam String filename)
        throws ContentNotFoundException {

        Resource resource = configurationService.getPluginFile(siteId, type, extension, filename);

        MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap();
        String contentType = mimeMap.getContentType(filename);

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, contentType).body(resource);
    }

}
