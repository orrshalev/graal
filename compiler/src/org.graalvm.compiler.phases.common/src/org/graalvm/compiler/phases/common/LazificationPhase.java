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
