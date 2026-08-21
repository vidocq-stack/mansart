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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.maven;

import java.io.IOException;
import java.lang.classfile.Annotation;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.Attributes;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.reflect.AccessFlag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Zero-dependency scanner that reads compiled {@code .class} files on the project classpath
 * with the JDK {@link java.lang.classfile Class-File API} (JEP 484) to discover
 * {@code @jakarta.persistence.Entity} classes, and remembers every class name it sees so
 * that entities which already ship a generated {@code <Entity>Metadata} companion (APT-built
 * jars) are not regenerated.
 */
final class EntityClassScanner {

    private static final String ENTITY_DESC = "Ljakarta/persistence/Entity;";

    private EntityClassScanner() {
    }

    /**
     * @param classpath the classpath elements (jars and class directories) to scan
     * @param packages  optional package-prefix filters; empty or null accepts every package
     * @return canonical FQNs of every {@code @Entity} class on {@code classpath} that matches
     *         the package filters and has no {@code <fqn>Metadata} companion class
     */
    static List<String> findEntities(List<String> classpath, List<String> packages) throws IOException {
        Set<String> entities = new LinkedHashSet<>();
        Set<String> allClasses = new LinkedHashSet<>();
        for (String element : classpath) {
            Path p = Path.of(element);
            if (!Files.exists(p)) {
                continue;
            }
            if (Files.isDirectory(p)) {
                // Class directories are the project's own build output: companions found
                // there come from a PREVIOUS run of this plugin and must not suppress
                // regeneration (they may be stale) — collect entities only.
                scanDirectory(p, entities, new LinkedHashSet<>());
            } else if (element.endsWith(".jar")) {
                scanJar(p, entities, allClasses);
            }
        }

        List<String> result = new ArrayList<>();
        for (String entity : entities) {
            if (!matchesPackages(entity, packages)) {
                continue;
            }
            if (allClasses.contains(entity + "Metadata")) {
                continue; // APT already generated the companion in the dependency jar
            }
            result.add(entity);
        }
        return result;
    }

    private static boolean matchesPackages(String fqn, List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return true;
        }
        for (String prefix : packages) {
            if (fqn.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void scanDirectory(Path dir, Set<String> entities, Set<String> allClasses) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) walk::iterator) {
                if (Files.isRegularFile(p) && p.toString().endsWith(".class")) {
                    inspect(Files.readAllBytes(p), entities, allClasses);
                }
            }
        }
    }

    private static void scanJar(Path jar, Set<String> entities, Set<String> allClasses) throws IOException {
        try (JarFile jf = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry entry = en.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")
                        && !entry.getName().startsWith("META-INF/")) {
                    try (var in = jf.getInputStream(entry)) {
                        inspect(in.readAllBytes(), entities, allClasses);
                    }
                }
            }
        }
    }

    private static void inspect(byte[] bytes, Set<String> entities, Set<String> allClasses) {
        ClassModel cm;
        try {
            cm = ClassFile.of().parse(bytes);
        } catch (RuntimeException malformed) {
            return; // not a parseable class file — skip
        }
        String internal = cm.thisClass().asInternalName();
        if (internal.indexOf('$') >= 0) {
            return; // nested classes are not supported as external entities
        }
        String fqn = internal.replace('/', '.');
        allClasses.add(fqn);
        if (cm.flags().has(AccessFlag.INTERFACE)) {
            return;
        }
        var attr = cm.findAttribute(Attributes.runtimeVisibleAnnotations());
        if (attr.isEmpty()) {
            return;
        }
        for (Annotation a : ((RuntimeVisibleAnnotationsAttribute) attr.get()).annotations()) {
            if (ENTITY_DESC.equals(a.className().stringValue())) {
                entities.add(fqn);
                return;
            }
        }
    }
}
