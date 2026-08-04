/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.data.bench;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Main entry point to run all JMH benchmarks for Mansart Data.
 *
 * <p>Usage:
 * <pre>
 *   java -jar target/benchmarks.jar
 *   # or
 *   mvn -Pbench verify
 * </pre>
 */
public class BenchRunner {
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include("*.*Bench")
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .threads(1, 4, 16)  // Test single-thread, 4 VTs, 16 VTs
                .build();
        new Runner(opt).run();
    }
}
