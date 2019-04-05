/**
 * Copyright 2017-2019 the original author or authors from the JHipster Online project.
 *
 * This file is part of the JHipster Online project, see https://github.com/jhipster/jhipster-online
 * for more information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jhipster.online.web.rest;

import java.io.*;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import com.codahale.metrics.annotation.Timed;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import io.github.jhipster.online.domain.User;
import io.github.jhipster.online.domain.enums.GitProvider;
import io.github.jhipster.online.security.AuthoritiesConstants;
import io.github.jhipster.online.service.*;

@RestController
@RequestMapping("/api")
public class GeneratorResource {

    private final Logger log = LoggerFactory.getLogger(GeneratorResource.class);

    private final GeneratorService generatorService;

    private final GithubService githubService;

    private final GitlabService gitlabService;

    private final UserService userService;

    private final LogsService logsService;

    public GeneratorResource(GeneratorService generatorService,
        GithubService githubService,
        GitlabService gitlabService,
        UserService userService,
        LogsService logsService) {
        this.generatorService = generatorService;
        this.githubService = githubService;
        this.gitlabService = gitlabService;
        this.userService = userService;
        this.logsService = logsService;
    }

    @PostMapping("/generate-application")
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity generateApplicationOnGit(@RequestBody String applicationConfiguration) throws Exception {
        log.info("Generating application on GitHub - .yo-rc.json: {}", applicationConfiguration);
        User user = userService.getUser();
        log.debug("Reading application configuration");
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(applicationConfiguration);
        GitProvider provider = GitProvider.getGitProviderByValue(JsonPath.read(document, "$.git-provider"))
            .orElseThrow(() -> new Exception("No git provider"));
        String gitCompany = JsonPath.read(document, "$.git-company");
        String applicationName = JsonPath.read(document, "$.generator-jhipster.baseName");
        String repositoryName = JsonPath.read(document, "$.repository-name");
        String applicationId = UUID.randomUUID().toString();

        log.debug("Using provider: {} ({})", provider, JsonPath.read(document, "$.git-provider"));
        log.debug("Generating application in repository id={} - {} / {}", applicationId, gitCompany, repositoryName);
        this.logsService.addLog(applicationId, "Generating application in repository " + gitCompany + "/" +
            repositoryName);

        try {
            if (provider.equals(GitProvider.GITHUB)) {
                this.githubService.createGitProviderRepository(
                    user, applicationId, applicationConfiguration, gitCompany, repositoryName);
            } else if (provider.equals(GitProvider.GITLAB)) {
                this.gitlabService.createGitProviderRepository(
                    user, applicationId, applicationConfiguration, gitCompany, repositoryName);
            }

        } catch (Exception e) {
            log.error("Error generating application", e);
            this.logsService.addLog(applicationId, "An error has occurred: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(applicationId, HttpStatus.CREATED);
    }

    @GetMapping("/generate-application/{applicationId}")
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<String> generateApplicationOutput(@PathVariable String applicationId) {
        String logs = this.logsService.getLogs(applicationId);
        return new ResponseEntity<>(logs, HttpStatus.OK);
    }

    @PostMapping("/download-application")
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public @ResponseBody
    ResponseEntity downloadApplication(@RequestBody String applicationConfiguration) {
        log.info("Downloading application - .yo-rc.json: {}", applicationConfiguration);
        String applicationId = UUID.randomUUID().toString();
        String zippedApplication;
        try {
            zippedApplication = this.generatorService.generateZippedApplication(applicationId,
                applicationConfiguration);
        } catch (IOException ioe) {
            log.error("Error generating application", ioe);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(zippedApplication);
            byte[] out = IOUtils.toByteArray(inputStream);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("content-disposition", "attachment; filename=application.zip");
            responseHeaders.add("Content-Type", "application/octet-stream");
            responseHeaders.add("Content-Transfer-Encoding", "binary");
            responseHeaders.add("Content-Length", String.valueOf(out.length));
            return new ResponseEntity(out, responseHeaders, HttpStatus.OK);
        } catch (IOException ioe) {
            log.error("Error sending zipped application", ioe);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/jdl-generate")
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity generateJdlApps(@RequestBody String jdlContent) throws Exception {
        log.info("Generating microservices listed on jdl file ", jdlContent);
        String zippedApplication = this.generatorService.generateJdlApps(jdlContent);
        
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(zippedApplication);
            byte[] out = IOUtils.toByteArray(inputStream);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("content-disposition", "attachment; filename=applications.zip");
            responseHeaders.add("Content-Type", "application/octet-stream");
            responseHeaders.add("Content-Transfer-Encoding", "binary");
            responseHeaders.add("Content-Length", String.valueOf(out.length));
            return new ResponseEntity(out, responseHeaders, HttpStatus.OK);
        } catch (IOException ioe) {
            log.error("Error sending zipped application", ioe);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
    }

}
