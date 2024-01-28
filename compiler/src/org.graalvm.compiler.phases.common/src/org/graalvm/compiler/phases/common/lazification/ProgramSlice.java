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

import jdk.internal.org.objectweb.asm.tree.analysis.Value;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import org.graalvm.compiler.core.common.cfg.AbstractControlFlowGraph;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.iterators.NodeIterable;
import org.graalvm.compiler.nodes.AbstractMergeNode;
import org.graalvm.compiler.nodes.ControlSplitNode;
import org.graalvm.compiler.nodes.FrameState;
import org.graalvm.compiler.nodes.IfNode;
import org.graalvm.compiler.nodes.InvokeNode;
import org.graalvm.compiler.nodes.LoopBeginNode;
import org.graalvm.compiler.nodes.LoopExitNode;
import org.graalvm.compiler.nodes.MergeNode;
import org.graalvm.compiler.nodes.PhiNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.ValuePhiNode;
import org.graalvm.compiler.nodes.calc.BinaryNode;
import org.graalvm.compiler.nodes.cfg.Block;
import org.graalvm.compiler.nodes.cfg.ControlFlowGraph;
import org.graalvm.compiler.nodes.java.MethodCallTargetNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;


public class ProgramSlice {
    StructuredGraph graph;
    ValueNode initial;
    ResolvedJavaMethod parentMethod;
    InvokeNode callSite;
    ResolvedJavaMethod outlinedMethod;

    private ArrayList<ValueNode> dependencyArguments;
    private HashSet<Block> blocksInSlice;

    private HashMap<Block, Block> attractors;

    private HashMap<Block, Block> originalToNewBlocks;

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
    static private Optional<Block> GetController(Block block) {
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
    static private Set<ControlSplitNode> computeGates(StructuredGraph graph) {
        Set<ControlSplitNode> gates = new HashSet<>();
        for (Node node: graph.getNodes()) {
            if (node instanceof ControlSplitNode) {
                gates.add((ControlSplitNode) node);
            }
        }
        return gates;
    }

    /**
     * Computes the thunk type for the outlined method
     * @return the thunk type
     */
    private ResolvedJavaType computeThunkType(ResolvedJavaType parentType) {
        // create a new class
        ClassPool pool = ClassPool.getDefault();
        Class runtimeClass = parentType.getClass();
        LoopBeginNode n;
        throw new UnsupportedOperationException();
    }

    /**
     * Computes the first dominator blocks for each block in the original function
     */
    private void computeAttractorBlocks() {
       ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, true, true);
       Block dominator = cfg.blockFor(callSite).getDominator();
       HashMap<Block, Block> attractorBlocks = new HashMap<>();

       for (Block block : cfg.getBlocks()) {
           if (attractorBlocks.containsKey(block)) {
               continue;
           }
           if (this.blocksInSlice.contains(block)) {
               attractorBlocks.put(block, block);
               continue;
           }

           Block dominatorBlock = block.getDominator();
           Block Cand = dominatorBlock.getDominator();
           while (Cand != null) {
               if (this.blocksInSlice.contains(Cand)) {
                   break;
               }
               Cand = Cand.getDominator();
           }
           if (Cand != null) {
               attractorBlocks.put(block, Cand);
           }
       }
       this.attractors = attractorBlocks;
    }

    /**
     * Add branches from immediate dominators which existed in the original function
     * the slice
     */
    private void addDominatorBranches(Block current, Block parent, HashSet<Block> visited) {
        if (this.blocksInSlice.contains(current)) {
            parent = current;
        }

        for (Block successor : current.getSuccessors()) {
            if (!visited.contains(successor)) {
                visited.add(successor);
                addDominatorBranches(successor, parent, visited);
            }
            if (this.blocksInSlice.contains(successor) && parent != null) {
                Block parentBlock = this.originalToNewBlocks.get(parent);
                Block successorBlock = this.originalToNewBlocks.get(successor);
                // add branch path from parentBlock to successorBlock
                Block[] successors = parentBlock.getSuccessors();
                Block[] newSuccesors = Arrays.copyOf(successors, successors.length + 1);
                newSuccesors[newSuccesors.length - 1] = successorBlock;
                parentBlock.setSuccessors(newSuccesors);
            }
        }
    }

    /**
     * Remove unnecessary predecessors to phi nodes in new slice
     */
    private void updatePhiNodes(StructuredGraph graph) {
        

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
        Set<ValueNode> values;
        DataDependencies(Set<Block> blocks, Set<ValueNode> values) {
            this.blocks = blocks;
            this.values = values;
        }
    }



    /**
     * Computes the backwards data dependencies of the parameter to compute which values
     * should be part of the slice
     * @return a set of blocks and a set of values that are part of the slice
     */
public static Set<Node> computeDataDependencies(StructuredGraph graph, InvokeNode I, int parameter) {

    ValueNode parameterNode = I.callTarget().arguments().get(parameter);

    Queue<Node> queue = new LinkedList<>();
    Set<Node> visited = new HashSet<>();

    queue.add(parameterNode);

    while (!queue.isEmpty()) {
        Node current = queue.poll();
        visited.add(current);

        for (Node input : current.inputs()) {
            if (!visited.contains(input)) {
                queue.add(input);
            }
        }
    }

    Set<ControlSplitNode> gates = computeGates(graph);

    if (gates.isEmpty()) return visited;

    Set<Node> visitedFromGate = new HashSet<>();

    for (ControlSplitNode gate : gates) {
        queue.add(gate);
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            visitedFromGate.add(current);

            for (Node input : current.inputs()) {
                if (!visitedFromGate.contains(input)) {
                    queue.add(input);
                }
            }
        }
        for (Node node : visited) {
            if (node instanceof PhiNode && visitedFromGate.contains(node)) {
                visited.addAll(visitedFromGate);
                break;
            }
        }
        visitedFromGate.clear();
    }


    return visited;
}

    /**
     * Builds control flow of slice
     */
    private void rerouteBranches(StructuredGraph graph, InvokeNode I, int parameter, HashMap<Block, ArrayList<ValueNode>> gates) {
        throw new UnsupportedOperationException();
    }
}
