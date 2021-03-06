/*
 * Copyright (c) 2014 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.helios.agent;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.ImageInfo;
import com.spotify.helios.common.descriptors.Job;

import java.util.List;
import java.util.Set;

/**
 * Bind mounts /usr/lib/helios inside the container as /helios, and uses the syslog-redirector
 * executable there to redirect container stdout/err to syslog.
 */
public class SyslogRedirectingContainerDecorator implements ContainerDecorator {

  private final String syslogHostPort;

  public SyslogRedirectingContainerDecorator(String syslogHostPort) {
    this.syslogHostPort = syslogHostPort;
  }

  @Override
  public void decorateHostConfig(HostConfig.Builder hostConfig) {
    final List<String> binds = Lists.newArrayList();
    if (hostConfig.binds() != null) {
      binds.addAll(hostConfig.binds());
    }
    binds.add("/usr/lib/helios:/helios:ro");
    hostConfig.binds(binds);
  }

  @Override
  public void decorateContainerConfig(Job job, ImageInfo imageInfo,
                                      ContainerConfig.Builder containerConfig) {
    ContainerConfig imageConfig = imageInfo.containerConfig();

    final List<String> entrypoint = Lists.newArrayList("/helios/syslog-redirector",
                                                       "-h", syslogHostPort,
                                                       "-n", job.getId().toString(),
                                                       "--");
    if (imageConfig.entrypoint() != null) {
      entrypoint.addAll(imageConfig.entrypoint());
    }
    containerConfig.entrypoint(entrypoint);

    final Set<String> volumes = Sets.newHashSet();
    if (containerConfig.volumes() != null) {
      volumes.addAll(containerConfig.volumes());
    }
    volumes.add("/helios");
    containerConfig.volumes(volumes);
  }
}
