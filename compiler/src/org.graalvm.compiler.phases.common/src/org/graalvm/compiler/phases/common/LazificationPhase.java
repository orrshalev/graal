package org.graalvm.compiler.phases.common;

import org.graalvm.compiler.core.common.cfg.AbstractControlFlowGraph;
import org.graalvm.compiler.debug.DebugContext;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.InvokeNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.cfg.ControlFlowGraph;
import org.graalvm.compiler.nodes.spi.CoreProviders;
import org.graalvm.compiler.options.Option;
import org.graalvm.compiler.phases.BasePhase;
import org.graalvm.compiler.nodes.cfg.Block;
import org.graalvm.compiler.phases.Phase;
import org.graalvm.compiler.nodes.Invokable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
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
       System.out.println("LazificationPhase");
       // try (DebugContext.Scope s = graph.getDebug().scope("Lazification")) {
           for (Node node : graph.getNodes()) {
              if (node instanceof InvokeNode) {
                  InvokeNode invokeNode = (InvokeNode) node;
                  Set<Node> backwardSlice = SliceCriterion(graph, invokeNode);
              }
           }
       // }
   }

   static private boolean postDominates(Block a, Block b) {
       while (b != null && b != a) {
           b = b.getPostdominator();
       }
       return a == b;
   }

   static private Optional<Block> GetController(Block block, ControlFlowGraph cfg) {
       Block dominator = block.getDominator();
       while (dominator != null) {
           if (!postDominates(block, dominator)) {
               return Optional.of(dominator);
           } else {
               dominator = dominator.getDominator();
           }
       }
       return Optional.empty();
   }

   static private HashMap<Block, ArrayList<ValueNode>> computeGates(StructuredGraph graph) {
       HashMap<Block, ArrayList<ValueNode>> gates = new HashMap<>();
       ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, true, true);
       for (Block block : cfg.getBlocks()) {
           ArrayList<ValueNode> blockGate = new ArrayList<>();
           for (Block predecessor : block.getPredecessors()) {
               // if the predecessor dominates blocks and if block does not post dominate predecessor
               if (AbstractControlFlowGraph.dominates(block, predecessor) && !postDominates(block, predecessor)) {
                   blockGate.add(predecessor.getEndNode());
               } else {
                   Optional<Block> controlBlock = GetController(predecessor, cfg);
                   controlBlock.ifPresent(value -> blockGate.add(value.getEndNode()));
               }
           }
           gates.put(block, blockGate);
       }
       return gates;
   }

   static private Set<Node> SliceCriterion(StructuredGraph graph, InvokeNode callSite) {
       Set<Node> backwardSlice = new HashSet<>();
       ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, true, true);
       for (ValueNode parameter : callSite.callTarget().arguments()) {
       }
       return backwardSlice;
   }
}
