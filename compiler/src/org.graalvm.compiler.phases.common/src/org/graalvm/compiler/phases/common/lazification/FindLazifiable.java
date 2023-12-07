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
