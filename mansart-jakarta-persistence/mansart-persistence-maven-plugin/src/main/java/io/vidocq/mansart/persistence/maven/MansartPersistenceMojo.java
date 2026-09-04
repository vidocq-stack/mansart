/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Maven plugin for Mansart Jakarta Persistence 3.2.
 * Stub implementation for JP-01.
 */
@Mojo(name = "enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class MansartPersistenceMojo extends AbstractMojo {
    @Override
    public void execute() {
        getLog().warn("not implemented: MansartPersistenceMojo.execute()");
    }
}
