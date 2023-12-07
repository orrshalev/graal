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
import org.graalvm.compiler.core.common.cfg.AbstractControlFlowGraph;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.InvokeNode;
import org.graalvm.compiler.nodes.PhiNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import javassist.ClassPool;
import org.graalvm.compiler.nodes.calc.BinaryNode;
import org.graalvm.compiler.nodes.cfg.Block;
import org.graalvm.compiler.nodes.cfg.ControlFlowGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class ProgramSlice {
    ValueNode initial;
    ResolvedJavaMethod parentMethod;
    InvokeNode callSite;

    public ProgramSlice(ValueNode initial, ResolvedJavaMethod parentMethod, InvokeNode callSite) {
        this.initial = initial;
        this.parentMethod = parentMethod;
        this.callSite = callSite;
    }
    public ResolvedJavaMethod outline() {
        throw new UnsupportedOperationException();
    }

    public boolean canOutline() {
        throw new UnsupportedOperationException();
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

    static class DataDependencies {
        Set<Block> blocks;
        Set<ValueNode> values;
        DataDependencies(Set<Block> blocks, Set<ValueNode> values) {
            this.blocks = blocks;
            this.values = values;
        }
    }

    private DataDependencies computeDataDependencies(StructuredGraph graph, InvokeNode I, int parameter, HashMap<Block, ArrayList<ValueNode>> gates) {

        Set<ValueNode> dependencies = new HashSet<>();
        Set<Block> blocks = new HashSet<>();
        HashSet<ValueNode> visited = new HashSet<>();
        Queue<ValueNode> to_visit = new LinkedList<>();

        ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, true, true);

        ValueNode parameterNode = I.callTarget().arguments().get(parameter);
        to_visit.add(parameterNode);

        while (!to_visit.isEmpty()) {
            ValueNode current = to_visit.remove();
            dependencies.add(current);

            if (current instanceof BinaryNode) {
                blocks.add(cfg.blockFor(current));
                for (Node input : current.usages()) {
                    // TODO
                }
            }
            if (current instanceof PhiNode) {
                // TODO
            }
        }

        return new DataDependencies(blocks, dependencies);
    }
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


    static private Set<Node> SliceCriterion(StructuredGraph graph, InvokeNode callSite) {
        Set<Node> backwardSlice = new HashSet<>();
        ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, true, true);
        for (ValueNode parameter : callSite.callTarget().arguments()) {
        }
        return backwardSlice;
    }
}
