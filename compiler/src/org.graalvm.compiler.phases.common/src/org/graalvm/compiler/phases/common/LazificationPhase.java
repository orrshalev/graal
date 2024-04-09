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
import org.graalvm.compiler.nodes.BeginNode;
import org.graalvm.compiler.nodes.EndNode;
import org.graalvm.compiler.nodes.InvokeNode;
import org.graalvm.compiler.nodes.PhiNode;
import org.graalvm.compiler.nodes.StartNode;
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
import java.io.FileWriter;
import java.io.IOException;


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
           debugPrintDataDependencies(graph);
       }
   }

    static private void debugComputeDataDependencies(StructuredGraph graph) {
        for (Node node : graph.getNodes()) {
            if (node instanceof InvokeNode ) {
                ProgramSlice.OutlinableParameter[][] outlinableParameters = ProgramSlice.canOutline((InvokeNode) node);
                for (int i = 0; i < outlinableParameters.length; i++) {
                    Set<Node> dataDeps = ProgramSlice.computeDataDependencies(graph, (InvokeNode) node, outlinableParameters[i][0].getParameterIndex());
                }
            }
        }
    }

   static private void debugPrintNodeOfInterest(StructuredGraph graph) {
       for (Node node : graph.getNodes()) {
           if (node instanceof BeginNode) {
               System.out.printf("%s ", node);
               System.out.print("inputs:");
               for (Node input : node.inputs()) {
                   System.out.printf(" %s ", input);
               }
               System.out.print("usages:");
               for (Node usage : node.usages()) {
                    System.out.printf(" %s ", usage);
               }
               System.out.println();
           }
       }
   }
   static private void debugPrintPhiNodes(StructuredGraph graph) {
       for (Node node : graph.getNodes()) {
           if (node instanceof ValuePhiNode) {
               System.out.print(node);
               System.out.print(" inputs:");
               for (Node input : node.inputs()) {
                   System.out.printf(" %s ", input);
               }
               System.out.print(" values: ");
               for (Node input : ((ValuePhiNode) node).values()) {
                   System.out.printf(" %s ", input);
               }
               System.out.print(" merge: ");
               System.out.printf(" %s ", ((ValuePhiNode) node).merge());
               System.out.println();
           }
       }
   }

   // TODO: fix if needed to include multiple parameters
   static private void debugPrintDataDependencies(StructuredGraph graph) {
       // create file called dataDeps.txt

       try {
           FileWriter myWriter = new FileWriter("dataDeps.txt", true);
           for (Node node : graph.getNodes()) {
               if (node instanceof InvokeNode) {
                   myWriter.write("Invoke node: " + node + "\n");
                   ProgramSlice.OutlinableParameter[][] outlinableParameters = ProgramSlice.canOutline((InvokeNode) node);
                   for (int i = 0; i < outlinableParameters.length; i++) {
                       myWriter.write("\nSTART OF GROUP");
                       for (int j = 0; j < outlinableParameters[i].length; j++) {
                           Set<Node> visited = new HashSet<>();
                           myWriter.write(String.format("\nOutlinable parameter: %d ", outlinableParameters[i][j].getParameterIndex()));
                           // System.out.println();
                           Set<Node> dataDeps = ProgramSlice.computeDataDependencies(graph, (InvokeNode) node, outlinableParameters[i][j].getParameterIndex());
                           for (Node dataDep : dataDeps) {
                               myWriter.write(String.format(" %s ", dataDep));
                               visited.add(dataDep);
                           }
                           myWriter.write("Not included: ");
                           for (Node unvisitedNode : graph.getNodes()) {
                               if (!visited.contains(unvisitedNode)) {
                                   myWriter.write(String.format(" %s ", unvisitedNode));
                               }
                           }
                           // System.out.println();
                       }
                       myWriter.write("\nEND OF GROUP\n");
                   }
               }
           }
           myWriter.close();
       } catch (IOException e) {
           System.out.println("An error occurred.");
           e.printStackTrace();
       }
   }
   static private void debugPrintBlocks(StructuredGraph graph) {
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
