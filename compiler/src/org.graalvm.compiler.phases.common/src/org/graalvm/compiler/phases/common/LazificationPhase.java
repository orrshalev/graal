package org.graalvm.compiler.phases.common;

import org.graalvm.compiler.debug.DebugContext;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.InvokeNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.cfg.ControlFlowGraph;
import org.graalvm.compiler.nodes.spi.CoreProviders;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.nodes.cfg.Block;
import org.graalvm.compiler.phases.Phase;
import org.graalvm.compiler.nodes.Invokable;

import java.util.Set;
import java.util.HashSet;

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
           for (Node node : graph.getNodes()) {
              if (node instanceof InvokeNode) {
                  InvokeNode invokeNode = (InvokeNode) node;
                  Set<Block> blocks = SliceCriterion(graph, cfg, invokeNode);
              }
           }
       }
   }

    // iterate over all invoke nodes
   static private Set<Block> SliceCriterion(StructuredGraph graph, ControlFlowGraph cfg, InvokeNode callSite) {
       Set<Block> blocks = new HashSet<>();
       for (ValueNode parameter : callSite.callTarget().arguments()) {
           for (Node usage : parameter.usages()) {
               Block block = cfg.blockFor(usage);
               blocks.add(block);
           }
       }
       return blocks;
   }
}
