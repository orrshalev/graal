/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.core.locks;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.IsolateThread;

import com.oracle.svm.core.SubstrateDiagnostics.DiagnosticThunk;
import com.oracle.svm.core.SubstrateDiagnostics.ErrorContext;
import com.oracle.svm.core.Uninterruptible;
import com.oracle.svm.core.heap.RestrictHeapAccess;
import com.oracle.svm.core.log.Log;

import jdk.graal.compiler.api.replacements.Fold;

public abstract class VMLockSupport {

    @Fold
    public static VMLockSupport singleton() {
        return ImageSingletons.lookup(VMLockSupport.class);
    }

    /**
     * Returns an array that contains all {@link VMMutex} objects that are present in the image or
     * null if that information is not available.
     */
    public abstract VMMutex[] getMutexes();

    /**
     * Returns an array that contains all {@link VMCondition} objects that are present in the image
     * or null if that information is not available.
     */
    public abstract VMCondition[] getConditions();

    /**
     * Returns an array that contains all {@link VMSemaphore} objects that are present in the image
     * or null if that information is not available.
     */
    public abstract VMSemaphore[] getSemaphores();

    /**
     * Initializes all {@link VMMutex}, {@link VMCondition}, and {@link VMSemaphore} objects.
     *
     * @return {@code true} if the initialization was successful, {@code false} if an error
     *         occurred.
     */
    @Uninterruptible(reason = "Too early for safepoints.")
    public final boolean initialize() {
        VMLockSupport support = VMLockSupport.singleton();
        for (VMMutex mutex : support.getMutexes()) {
            if (mutex.initialize() != 0) {
                return false;
            }
        }

        for (VMCondition condition : support.getConditions()) {
            if (condition.initialize() != 0) {
                return false;
            }
        }

        for (VMSemaphore semaphore : support.getSemaphores()) {
            if (semaphore.initialize() != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Destroys all {@link VMMutex}, {@link VMCondition}, and {@link VMSemaphore} objects.
     *
     * @return {@code true} if the destruction was successful, {@code false} if an error occurred.
     */
    @Uninterruptible(reason = "The isolate teardown is in progress.")
    public final boolean destroy() {
        VMLockSupport support = VMLockSupport.singleton();
        for (VMSemaphore semaphore : support.getSemaphores()) {
            if (semaphore.destroy() != 0) {
                return false;
            }
        }

        for (VMCondition condition : support.getConditions()) {
            if (condition.destroy() != 0) {
                return false;
            }
        }

        for (VMMutex mutex : support.getMutexes()) {
            if (mutex.destroy() != 0) {
                return false;
            }
        }

        return true;
    }

    public static class DumpVMMutexes extends DiagnosticThunk {
        @Override
        public int maxInvocationCount() {
            return 1;
        }

        @Override
        @RestrictHeapAccess(access = RestrictHeapAccess.Access.NO_ALLOCATION, reason = "Must not allocate while printing diagnostics.")
        public void printDiagnostics(Log log, ErrorContext context, int maxDiagnosticLevel, int invocationCount) {
            log.string("VM mutexes:").indent(true);

            VMLockSupport support = null;
            if (ImageSingletons.contains(VMLockSupport.class)) {
                support = ImageSingletons.lookup(VMLockSupport.class);
            }

            if (support == null || support.getMutexes() == null) {
                log.string("No mutex information is available.").newline();
            } else {
                for (VMMutex mutex : support.getMutexes()) {
                    IsolateThread owner = mutex.owner;
                    log.string("mutex \"").string(mutex.getName()).string("\" ");
                    if (owner.isNull()) {
                        log.string("is unlocked.");
                    } else {
                        log.string("is locked by ");
                        if (owner.equal(VMMutex.UNSPECIFIED_OWNER)) {
                            log.string("an unspecified thread.");
                        } else {
                            log.string("thread ").zhex(owner);
                        }
                    }
                    log.newline();
                }
            }

            log.indent(false);
        }
    }
}
