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
import jdk.vm.ci.meta.ResolvedJavaType;
import org.graalvm.compiler.debug.DebugContext;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.InvokeNode;
import org.graalvm.compiler.nodes.PhiNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValuePhiNode;
import org.graalvm.compiler.nodes.cfg.Block;
import org.graalvm.compiler.nodes.cfg.ControlFlowGraph;
import org.graalvm.compiler.nodes.spi.CoreProviders;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.phases.common.lazification.FindLazifiable;
import org.graalvm.compiler.phases.common.lazification.ProgramSlice;

import java.util.HashSet;
import java.util.Set;


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
           ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, true, true);
           // debugPrintDataDependencies(graph);
           // debugPrintPhiNodes(graph);
           debugPrintBlocks(graph);
           // debugPrintPhiMergeNodes(graph);
           // FindLazifiable findLazifiable = new FindLazifiable(graph);
           // for (FindLazifiable.LazifiableParam lazifiableParam : findLazifiable.getLazifiable()) {
           //     lazifyParam(graph, lazifiableParam);
           // }
       }
   }

   static private boolean hasCaleeNode(StructuredGraph graph) {
         for (Node node : graph.getNodes()) {
              if (node instanceof InvokeNode && node.toString().contains("calee")) {
                return true;
              }
         }
         return false;
   }
   static private void debugPrintPhiNodes(StructuredGraph graph) {
       if (!hasCaleeNode(graph)) {
           return;
       }
       for (Node node : graph.getNodes()) {
           if (node instanceof ValuePhiNode) {
               System.out.print(node);
               for (Node input : ((ValuePhiNode) node).values()) {
                   System.out.printf(" %s ", input);
               }
               System.out.println();
           }
       }
   }

   static private void debugPrintDataDependencies(StructuredGraph graph) {
       if (!hasCaleeNode(graph)) {
            return;
       }
       Set<Node> visited = new HashSet<>();
       for (Node node : graph.getNodes()) {
           if (node instanceof InvokeNode && node.toString().contains("calee")) {
               System.out.printf("Invoke node:%s\t", node);
               visited.add(node);
               Set<Node> dataDeps = ProgramSlice.computeDataDependencies(graph, (InvokeNode) node, 1);
               for (Node dataDep : dataDeps) {
                   System.out.printf(" %s ", dataDep);
                   visited.add(dataDep);
               }
               System.out.println();
           }
       }
       System.out.print("Not included: ");
       for (Node node : graph.getNodes()) {
           if (!visited.contains(node)) {
                System.out.printf(" %s ", node);
           }
       }
       System.out.println();
   }
   static private void debugPrintBlocks(StructuredGraph graph) {
       if (!hasCaleeNode(graph)) {
            return;
       }
       ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, true, true);
       System.out.println(graph);
       System.out.println();
       System.out.println(cfg);
       System.out.println();
       System.out.printf("DominatorTree: %s", cfg.dominatorTreeString());
       System.out.println();
       for (Block block : cfg.getBlocks()) {
           System.out.printf("\nBLOCKSTART: %s\n", block);
           for (Node node : block.getNodes()) {
               System.out.printf("%s", node);
               System.out.println();
           }
           System.out.printf("\nBLOCKEND: %s\n", block);
       }
       System.out.println("\nGRAPHEND\n");
   }

   static private void debugPrintPhiMergeNodes(StructuredGraph graph) {
       for (Node node : graph.getNodes()) {
           if (node instanceof org.graalvm.compiler.nodes.PhiNode) {
               System.out.println(((PhiNode) node).merge());
           }
       }
   }

   static private void lazifyParam(StructuredGraph graph, FindLazifiable.LazifiableParam lazifiableParam) {
       ProgramSlice programSlice = new ProgramSlice(graph, lazifiableParam.value, graph.method(), lazifiableParam.callSite);
       ResolvedJavaMethod outlinedMethod = programSlice.getOutlinedMethod();
       ResolvedJavaType originalClass = graph.method().getDeclaringClass();
       updateWithThunk(originalClass, outlinedMethod);
   }

    /**
     * Updates the original class with a thunk that calls the outlined method
     */
   private static void updateWithThunk(ResolvedJavaType originalClass, ResolvedJavaMethod outlinedMethod) {
      throw new UnsupportedOperationException();
   }
}
