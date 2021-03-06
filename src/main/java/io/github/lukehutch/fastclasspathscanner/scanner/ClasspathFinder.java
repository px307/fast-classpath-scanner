/*
 * This file is part of FastClasspathScanner.
 * 
 * Author: Luke Hutchison
 * 
 * Hosted at: https://github.com/lukehutch/fast-classpath-scanner
 * 
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Luke Hutchison
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.lukehutch.fastclasspathscanner.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.classloaderhandler.ClassLoaderHandler;
import io.github.lukehutch.fastclasspathscanner.classloaderhandler.ClassLoaderHandlerRegistry;
import io.github.lukehutch.fastclasspathscanner.utils.AdditionOrderedSet;
import io.github.lukehutch.fastclasspathscanner.utils.FileUtils;
import io.github.lukehutch.fastclasspathscanner.utils.JarUtils;
import io.github.lukehutch.fastclasspathscanner.utils.LogNode;
import io.github.lukehutch.fastclasspathscanner.utils.NestedJarHandler;

/** A class to find the unique ordered classpath elements. */
public class ClasspathFinder {
    private final NestedJarHandler nestedJarHandler;
    private static String currDirPathStr = FileUtils.getCurrDirPathStr();

    /** The list of raw classpath elements. */
    private final List<ClasspathRelativePath> rawClasspathElements = new ArrayList<>();
    private final Set<ClasspathRelativePath> rawClasspathElementsSet = new HashSet<>();

    /**
     * Add a classpath element relative to a base file. May be called by a ClassLoaderHandler to add classpath
     * elements that it knows about.
     */
    private void addClasspathElement(final ClasspathRelativePath classpathEltPath, final LogNode log) {
        if (rawClasspathElementsSet.add(classpathEltPath)) {
            rawClasspathElements.add(classpathEltPath);
            if (log != null) {
                log.log("Found classpath element: " + classpathEltPath);
            }
        } else {
            if (log != null) {
                log.log("Ignoring duplicate classpath element: " + classpathEltPath);
            }
        }
    }

    /**
     * Add a classpath element relative to a base file. May be called by a ClassLoaderHandler to add classpath
     * elements that it knows about.
     * 
     * @param pathElement
     *            the URL or path of the classpath element.
     * @param classLoader
     *            the ClassLoader that this classpath element was obtained from.
     * @param log
     *            the LogNode instance to use if logging in verbose mode.
     * 
     * @return true (and add the classpath element) if pathElement is not null or empty, otherwise return false.
     */
    public boolean addClasspathElement(final String pathElement, final ClassLoader classLoader, final LogNode log) {
        if (pathElement == null || pathElement.isEmpty()) {
            return false;
        } else {
            addClasspathElement(new ClasspathRelativePath(currDirPathStr, pathElement, Arrays.asList(classLoader),
                    nestedJarHandler), log);
            return true;
        }
    }

    /**
     * Add a classpath element relative to a base file. May be called by a ClassLoaderHandler to add classpath
     * elements that it knows about.
     * 
     * @param pathElement
     *            the URL or path of the classpath element.
     * @param classLoaders
     *            the ClassLoader(s) that this classpath element was obtained from.
     * @param log
     *            the LogNode instance to use if logging in verbose mode.
     * 
     * @return true (and add the classpath element) if pathElement is not null or empty, otherwise return false.
     */
    public boolean addClasspathElement(final String pathElement, final List<ClassLoader> classLoaders,
            final LogNode log) {
        if (pathElement == null || pathElement.isEmpty()) {
            return false;
        } else {
            addClasspathElement(
                    new ClasspathRelativePath(currDirPathStr, pathElement, classLoaders, nestedJarHandler), log);
            return true;
        }
    }

    /**
     * Add classpath elements, separated by the system path separator character. May be called by a
     * ClassLoaderHandler to add a path string that it knows about.
     * 
     * @param pathStr
     *            the delimited string of URLs or paths of the classpath.
     * @param classLoader
     *            the ClassLoader that this classpath was obtained from.
     * @param log
     *            the LogNode instance to use if logging in verbose mode.
     * 
     * @return true (and add the classpath element) if pathElement is not null or empty, otherwise return false.
     */
    public boolean addClasspathElements(final String pathStr, final ClassLoader classLoader, final LogNode log) {
        if (pathStr == null || pathStr.isEmpty()) {
            return false;
        } else {
            for (final String pathElement : pathStr.split(File.pathSeparator)) {
                addClasspathElement(pathElement, classLoader, log);
            }
            return true;
        }
    }

    /**
     * Add classpath elements, separated by the system path separator character. May be called by a
     * ClassLoaderHandler to add a path string that it knows about.
     * 
     * @param pathStr
     *            the delimited string of URLs or paths of the classpath.
     * @param classLoaders
     *            the ClassLoader(s) that this classpath was obtained from.
     * @param log
     *            the LogNode instance to use if logging in verbose mode.
     * 
     * @return true (and add the classpath element) if pathElement is not null or empty, otherwise return false.
     */
    public boolean addClasspathElements(final String pathStr, final List<ClassLoader> classLoaders,
            final LogNode log) {
        if (pathStr == null || pathStr.isEmpty()) {
            return false;
        } else {
            for (final String pathElement : pathStr.split(File.pathSeparator)) {
                addClasspathElement(pathElement, classLoaders, log);
            }
            return true;
        }
    }

    // -------------------------------------------------------------------------------------------------------------

    /** A class to find the unique ordered classpath elements. */
    ClasspathFinder(final ScanSpec scanSpec, final NestedJarHandler nestedJarHandler, final LogNode log) {
        // Get all classloaders, in classpath resolution order
        final AdditionOrderedSet<ClassLoader> allClassLoaders = new AdditionOrderedSet<>();
        for (final ClassLoader classLoader : scanSpec.classLoaders) {
            final ArrayList<ClassLoader> parentClassLoaders = new ArrayList<>();
            for (ClassLoader cl = classLoader; cl != null; cl = cl.getParent()) {
                parentClassLoaders.add(cl);
            }
            // OpenJDK calls classloaders in a top-down order
            for (int i = parentClassLoaders.size() - 1; i >= 0; --i) {
                allClassLoaders.add(parentClassLoaders.get(i));
            }
        }
        final List<ClassLoader> classLoaderOrder = allClassLoaders.getList();

        this.nestedJarHandler = nestedJarHandler;
        if (scanSpec.overrideClasspath != null) {
            // Manual classpath override
            if (scanSpec.overrideClassLoaders) {
                if (log != null) {
                    log.log("It is not possible to override both the classpath and the ClassLoaders -- "
                            + "ignoring the ClassLoader override");
                }
            }
            final LogNode overrideLog = log == null ? null : log.log("Overriding classpath");
            addClasspathElements(scanSpec.overrideClasspath, classLoaderOrder, overrideLog);
            if (overrideLog != null) {
                log.log("WARNING: when the classpath is overridden, there is no guarantee that the classes "
                        + "found by classpath scanning will be the same as the classes loaded by the context "
                        + "classloader");
            }
        } else {
            // If system jars are not blacklisted, need to manually add rt.jar at the beginning of the classpath,
            // because it is included implicitly by the JVM.
            if (!scanSpec.blacklistSystemJars()) {
                // There should only be zero or one of these.
                final String rtJarPath = JarUtils.getRtJarPath();
                if (rtJarPath != null) {
                    // Insert rt.jar as the first entry in the classpath.
                    addClasspathElement(rtJarPath, classLoaderOrder, log);
                }
            }
            // Get all manually-added ClassLoaderHandlers
            final List<ClassLoaderHandler> classLoaderHandlers = new ArrayList<>();
            for (final Class<? extends ClassLoaderHandler> classLoaderHandler : scanSpec.extraClassLoaderHandlers) {
                try {
                    classLoaderHandlers.add(classLoaderHandler.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    if (log != null) {
                        log.log("Could not instantiate " + classLoaderHandler.getName(), e);
                    }
                }
            }
            // Add all default ClassLoaderHandlers after manually-added ClassLoaderHandlers
            for (final Class<? extends ClassLoaderHandler> classLoaderHandlerClass : //
            ClassLoaderHandlerRegistry.DEFAULT_CLASS_LOADER_HANDLERS) {
                try {
                    classLoaderHandlers.add(classLoaderHandlerClass.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    if (log != null) {
                        log.log("Could not instantiate " + classLoaderHandlerClass.getName(), e);
                    }
                }
            }
            if (log != null) {
                final LogNode classLoaderHandlerLog = log.log("ClassLoaderHandlers loaded:");
                for (final ClassLoaderHandler classLoaderHandler : classLoaderHandlers) {
                    classLoaderHandlerLog.log(classLoaderHandler.getClass().getName());
                }
            }

            // Try finding a handler for each of the classloaders discovered above
            for (final ClassLoader classLoader : classLoaderOrder) {
                // Skip system classloaders for efficiency if system jars are not going to be scanned.
                // TODO: Update to include JDK9 system classloader names.
                if (!scanSpec.blacklistSystemJars()
                        || !classLoader.getClass().getName().startsWith("sun.misc.Launcher$ExtClassLoader")) {
                    final LogNode classLoaderLog = log == null ? null
                            : log.log("Finding classpath elements in ClassLoader " + classLoader);
                    // Iterate through registered ClassLoaderHandlers
                    boolean classloaderFound = false;
                    for (final ClassLoaderHandler handler : classLoaderHandlers) {
                        try {
                            if (handler.handle(classLoader, this, classLoaderLog)) {
                                classloaderFound = true;
                                break;
                            }
                        } catch (final Exception e) {
                            if (classLoaderLog != null) {
                                classLoaderLog.log("Exception in ClassLoaderHandler", e);
                            }
                        }
                    }
                    if (!classloaderFound) {
                        if (classLoaderLog != null) {
                            classLoaderLog.log("Unknown ClassLoader type, cannot scan classes");
                        }
                    }
                }
            }

            if (!scanSpec.overrideClassLoaders) {
                // Add entries found in java.class.path, in case those entries were missed above due to some
                // non-standard classloader that uses this property
                final LogNode sysPropLog = log == null ? null
                        : log.log("Getting classpath entries from java.class.path");
                addClasspathElements(System.getProperty("java.class.path"), classLoaderOrder, sysPropLog);
            }
        }
    }

    /** Get the raw classpath elements obtained from ClassLoaders. */
    public List<ClasspathRelativePath> getRawClasspathElements() {
        return rawClasspathElements;
    }

    // -------------------------------------------------------------------------------------------------------------

    /** Used for resolving the classes in the call stack. Requires RuntimePermission("createSecurityManager"). */
    private static CallerResolver CALLER_RESOLVER;

    static {
        try {
            // This can fail if the current SecurityManager does not allow
            // RuntimePermission ("createSecurityManager"):
            CALLER_RESOLVER = new CallerResolver();
        } catch (final SecurityException e) {
            // Handled in findAllClassLoaders()
        }
    }

    // Using a SecurityManager gets around the fact that Oracle removed sun.reflect.Reflection.getCallerClass, see:
    // https://www.infoq.com/news/2013/07/Oracle-Removes-getCallerClass
    // http://www.javaworld.com/article/2077344/core-java/find-a-way-out-of-the-classloader-maze.html
    private static final class CallerResolver extends SecurityManager {
        @Override
        protected Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }

    /** Return true if cl0 is a descendant of cl1. */
    private static boolean isDescendantOf(final ClassLoader cl0, final ClassLoader cl1) {
        for (ClassLoader cl = cl0; cl != null; cl = cl.getParent()) {
            if (cl == cl1) {
                return true;
            }
        }
        return false;
    }

    /** Find all unique classloaders. */
    public static List<ClassLoader> findAllClassLoaders(final LogNode log) {
        // Need both a set and a list so we can keep them unique, but in an order that (hopefully) reflects
        // the order in which the JDK calls classloaders.
        //
        // See:
        // https://docs.oracle.com/javase/8/docs/technotes/tools/findingclasses.html
        // http://www.javaworld.com/article/2077344/core-java/find-a-way-out-of-the-classloader-maze.html?page=2
        //
        final AdditionOrderedSet<ClassLoader> classLoadersSet = new AdditionOrderedSet<>();

        // Look for classloaders on the call stack
        // Find the first caller in the call stack to call a method in the FastClasspathScanner package
        ClassLoader callerLoader = null;
        if (CALLER_RESOLVER == null) {
            if (log != null) {
                log.log(ClasspathFinder.class.getSimpleName() + " could not create "
                        + CallerResolver.class.getSimpleName() + ", current SecurityManager does not grant "
                        + "RuntimePermission(\"createSecurityManager\")");
            }
        } else {
            final Class<?>[] callStack = CALLER_RESOLVER.getClassContext();
            if (callStack == null) {
                if (log != null) {
                    log.log(ClasspathFinder.class.getSimpleName() + ": " + CallerResolver.class.getSimpleName()
                            + "#getClassContext() returned null");
                }
            } else {
                final String fcsPkgPrefix = FastClasspathScanner.class.getPackage().getName() + ".";
                int fcsIdx;
                for (fcsIdx = callStack.length - 1; fcsIdx >= 0; --fcsIdx) {
                    if (callStack[fcsIdx].getName().startsWith(fcsPkgPrefix)) {
                        break;
                    }
                }
                if (fcsIdx < 0 || fcsIdx == callStack.length - 1) {
                    // Should not happen
                    throw new RuntimeException("Could not find caller of " + fcsPkgPrefix + "* in call stack");
                }

                // Get the caller's current classloader
                callerLoader = callStack[fcsIdx + 1].getClassLoader();
            }
        }
        boolean useCallerLoader = callerLoader != null;

        // Get the context classloader
        final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        boolean useContextLoader = contextLoader != null;

        // Get the system classloader
        final ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        boolean useSystemLoader = systemLoader != null;

        // Establish descendancy relationships, and ignore any classloader that is an ancestor of another.
        if (useCallerLoader && useContextLoader && isDescendantOf(callerLoader, contextLoader)) {
            useContextLoader = false;
        }
        if (useContextLoader && useCallerLoader && isDescendantOf(contextLoader, callerLoader)) {
            useCallerLoader = false;
        }
        if (useSystemLoader && useContextLoader && isDescendantOf(systemLoader, contextLoader)) {
            useContextLoader = false;
        }
        if (useContextLoader && useSystemLoader && isDescendantOf(contextLoader, systemLoader)) {
            useSystemLoader = false;
        }
        if (useSystemLoader && useCallerLoader && isDescendantOf(systemLoader, callerLoader)) {
            useCallerLoader = false;
        }
        if (useCallerLoader && useSystemLoader && isDescendantOf(callerLoader, systemLoader)) {
            useSystemLoader = false;
        }
        if (!useCallerLoader && !useContextLoader && !useSystemLoader) {
            // Should not happen
            throw new RuntimeException("Could not find a usable ClassLoader");
        }
        // There will generally only be one class left after this. In rare cases, you may have a separate
        // callerLoader and contextLoader, but those cases are ill-defined -- see:
        // http://www.javaworld.com/article/2077344/core-java/find-a-way-out-of-the-classloader-maze.html?page=2
        // We specifically add the classloaders in the following order however, so that in the case that there
        // are two of them left, they are resolved in this order. The relative ordering of callerLoader and
        // contextLoader is due to the recommendation at the above URL.
        if (useSystemLoader) {
            classLoadersSet.add(systemLoader);
        }
        if (useCallerLoader) {
            classLoadersSet.add(callerLoader);
        }
        if (useContextLoader) {
            classLoadersSet.add(contextLoader);
        }

        final List<ClassLoader> classLoaders = classLoadersSet.getList();
        if (log != null) {
            for (final ClassLoader classLoader : classLoaders) {
                log.log("Found ClassLoader " + classLoader.toString());
            }
            log.addElapsedTime();
        }
        return classLoaders;
    }
}
