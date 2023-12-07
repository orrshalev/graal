/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.phases.common;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import org.graalvm.compiler.debug.DebugContext;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.spi.CoreProviders;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.phases.common.lazification.FindLazifiable;
import org.graalvm.compiler.phases.common.lazification.ProgramSlice;



/**
 * Performs lazification of lazifiable call sites. This is done by optimizing
 * {@link org.graalvm.compiler.nodes.Invoke} nodes that have parameters
 * that are read-only by the calee and that are computed in the caller without
 * side effects.
 */
public class LazificationPhase extends BasePhase<CoreProviders> {
   @Override
   @SuppressWarnings("try")
   protected void run(StructuredGraph graph, CoreProviders context) {
       try (DebugContext.Scope s = graph.getDebug().scope("Lazification")) {
           FindLazifiable findLazifiable = new FindLazifiable(graph);
           for (FindLazifiable.LazifiableParam lazifiableParam : findLazifiable.getLazifiable()) {
               lazifyParam(graph, lazifiableParam);
           }
       }
   }

   static private void lazifyParam(StructuredGraph graph, FindLazifiable.LazifiableParam lazifiableParam) {
       ProgramSlice programSlice = new ProgramSlice(lazifiableParam.value, graph.method(), lazifiableParam.callSite);
       if (!programSlice.canOutline()) {
           return;
       }
       ResolvedJavaMethod outlinedMethod = programSlice.outline();
       removeRedundantNodes(graph, outlinedMethod, lazifiableParam);
       // reroute(graph, programSlice);
       updateArgUses(graph, outlinedMethod, lazifiableParam);
   }

}
