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
import org.graalvm.compiler.graph.Graph;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.NodeInputList;
import org.graalvm.compiler.nodes.InvokeNode;
import org.graalvm.compiler.nodes.ValueNode;

import java.util.HashSet;
import java.util.Set;

public class FindLazifiable {

    public FindLazifiable(Graph graph) {
        this.lazifiable = calculateLazifiable(graph);
        this.promising = calculatePromising(graph); // TODO
    }

    private Set<LazifiableParam> calculateLazifiable(Graph graph) {
        Set<LazifiableParam> result = new HashSet<>();
        for (Node node : graph.getNodes()) {
            if (node instanceof InvokeNode) {
                InvokeNode invokeNode = (InvokeNode) node;
                ResolvedJavaMethod targetMethod = invokeNode.callTarget().targetMethod();
                ResolvedJavaMethod.Parameter[] parameters = targetMethod.getParameters();
                NodeInputList<ValueNode> arguments = invokeNode.callTarget().arguments();
                for (int i = 0; i < parameters.length; i++) {
                    result.add(new LazifiableParam(targetMethod, parameters[i], arguments.get(i)));
                }
            }
        }
        return result;
    }

    private Set<LazifiableParam> calculatePromising(Graph graph) {
        return null;
    }


    public static class LazifiableParam {
        public final ResolvedJavaMethod method;
        public final ResolvedJavaMethod.Parameter parameter;
        public final ValueNode value;
        public final InvokeNode callSite;

        public LazifiableParam(ResolvedJavaMethod method, ResolvedJavaMethod.Parameter parameter, ValueNode value, InvokeNode invokeNode) {
            this.method = method;
            this.parameter = parameter;
            this.value = value;
            this.callSite = invokeNode;
        }
    }

    private Set<LazifiableParam> promising;
    private Set<LazifiableParam> lazifiable;

    public Set<LazifiableParam> getPromising() {
        return promising;
    }

    public Set<LazifiableParam> getLazifiable() {
        return lazifiable;
    }



}
