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
package org.graalvm.compiler.phases.common.lazification;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import org.graalvm.compiler.core.common.cfg.AbstractControlFlowGraph;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.InvokeNode;
import org.graalvm.compiler.nodes.PhiNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.calc.BinaryNode;
import org.graalvm.compiler.nodes.cfg.Block;
import org.graalvm.compiler.nodes.cfg.ControlFlowGraph;
import org.graalvm.compiler.nodes.java.MethodCallTargetNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class ProgramSlice {
    StructuredGraph graph;
    ValueNode initial;
    ResolvedJavaMethod parentMethod;
    InvokeNode callSite;
    ResolvedJavaMethod outlinedMethod;

    public ProgramSlice(StructuredGraph graph, ValueNode initial, ResolvedJavaMethod parentMethod, InvokeNode callSite) {
        this.graph = graph;
        this.initial = initial;
        this.parentMethod = parentMethod;
        this.callSite = callSite;
        if (canOutline())
            this.outlinedMethod = outline();
    }
    public ResolvedJavaMethod outline() {
        throw new UnsupportedOperationException();
    }

    public boolean canOutline() {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the parameter has a path in the control flow graph where it is not used
     * @return
     */
    public boolean shouldOutline() {
        throw new UnsupportedOperationException();
    }

    public ValueNode[] getParameters() {
        return callSite.callTarget().arguments().toArray(new ValueNode[0]);
    }

    public ResolvedJavaMethod getOutlinedMethod() {
        return outlinedMethod;
    }

    public ResolvedJavaMethod.Parameter[] getParameterTypes() {
        return callSite.callTarget().targetMethod().getParameters();
    }

    /**
     * Update arguments used in original method to lazified version if value
     * used more than once after call site.
     */
    private static void updateArgUses() {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if a post dominates b
     */
    static private boolean postDominates(Block a, Block b) {
        while (b != null && b != a) {
            b = b.getPostdominator();
        }
        return a == b;
    }

    /**
     *  Returns the block whose predicate should control the phi-functions in blocks
     */
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

    /**
     * Computes the gates for all blocks in the slice
     * @return a map from blocks to their gates
     */
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

    /**
     * Computes the thunk type for the outlined method
     * @return the thunk type
     */
    private ResolvedJavaType computeThunkType(ResolvedJavaType originalClass) {
        throw new UnsupportedOperationException();
    }

    /**
     * Computes the first dominator blocks for each block in the original function
     */
    private void computeAttractorBlocks() {
        throw new UnsupportedOperationException();
    }

    /**
     * Add branches from immediate dominators which existed in the original function
     * the slice
     */
    private void addDominatorBranches() {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove unnecessary predecessors to phi nodes in new slice
     */
    private void updatePhiNodes(StructuredGraph graph) {
        throw new UnsupportedOperationException();
    }


    /**
     * Inserts new block into graph based on parameters of original method
     */
    private void insertNewBlock(StructuredGraph graph, Block block) {
        throw new UnsupportedOperationException();
    }

    /**
     * Inserts new blocks into new slice
     */
    private void populateFunctionWithBlocks(StructuredGraph graph) {
        throw new UnsupportedOperationException();
    }

    /**
     * Inserts new nodes into new slice
     */
    private void populateBlocksWithNodes(StructuredGraph graph) {
        throw new UnsupportedOperationException();
    }

    /**
     * Updates uses of new slice to use clones
     */
    private void reorganizeUses(StructuredGraph graph) {
        throw new UnsupportedOperationException();
    }

    /**
     * Add terimnators to new slice that are part of old method and were not included
     * but are necesary to preserve control flow
     */
    private void addMissingTerminators(StructuredGraph graph) {
        throw new UnsupportedOperationException();
    }

    /**
     * Ensure entry block is first block in new slice
     */
    private void reorderBlocks(StructuredGraph graph) {
        throw new UnsupportedOperationException();
    }

    /**
     * Add return node to new slice and ensure last block in new slice is the return block
     */
    private void addReturnValue(StructuredGraph graph) {
        throw new UnsupportedOperationException();
    }

    /**
     * Update loads in new slice to use inputs for thunk rather than for original method
     */
    private void updateLoads(StructuredGraph graph) {
        throw new UnsupportedOperationException();
    }

    static class DataDependencies {
        Set<Block> blocks;
        Set<Node> values;
        DataDependencies(Set<Block> blocks, Set<Node> values) {
            this.blocks = blocks;
            this.values = values;
        }
    }

    /**
     * Computes the backwards data dependencies of the parameter to compute which values
     * should be part of the slice
     * @return a set of blocks and a set of values that are part of the slice
     */
    private DataDependencies computeDataDependencies(StructuredGraph graph, InvokeNode I, int parameter, HashMap<Block, ArrayList<ValueNode>> gates) {

        Set<Node> dependencies = new HashSet<>();
        Set<Block> blocks = new HashSet<>();
        HashSet<Node> visited = new HashSet<>();
        Queue<Node> to_visit = new LinkedList<>();

        ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, true, true);

        ValueNode parameterNode = I.callTarget().arguments().get(parameter);
        to_visit.add(parameterNode);

        while (!to_visit.isEmpty()) {
            Node current = to_visit.remove();
            dependencies.add(current);

            if (current instanceof ValueNode) {
                blocks.add(cfg.blockFor(current));
                for (Node input : current.inputs()) {
                    if ((!(input instanceof ValueNode) && !(input instanceof MethodCallTargetNode)) || visited.contains(input)) continue;
                    visited.add(input);
                    to_visit.add(input);
                }
            }
            if (current instanceof PhiNode) {
                for (Node input : current.inputs()) {
                    Block block = cfg.blockFor(input);
                    if (block != null) {
                        blocks.add(block);
                    }
                }
                for (Node gate : gates.get(cfg.blockFor(current))) {
                    if (gate != null && !visited.contains(gate)) {
                        to_visit.add(gate);
                    }
                }
            }
        }

        return new DataDependencies(blocks, dependencies);
    }

    /**
     * Builds control flow of slice
     */
    private void rerouteBranches(StructuredGraph graph, InvokeNode I, int parameter, HashMap<Block, ArrayList<ValueNode>> gates) {
        DataDependencies dataDependencies = computeDataDependencies(graph, I, parameter, gates);
        Set<Block> blocks = dataDependencies.blocks;
        Set<ValueNode> dependencies = dataDependencies.values;

        ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, true, true);

        for (Block block : blocks) {
            for (Block successor : block.getSuccessors()) {
                if (blocks.contains(successor)) {
                    // TODO
                }
            }
        }
    }
}
