package collectors;

import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.*;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtReturnImpl;
import util.Constants;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FenixFrameworkCollector extends SpoonCollector {

    public FenixFrameworkCollector(int launcherChoice, String repoName, String projectPath)
            throws IOException {
        super(launcherChoice, repoName, projectPath);

        switch (launcherChoice) {
            case Constants.LAUNCHER:
            case Constants.MAVEN_LAUNCHER:
            case Constants.JAR_LAUNCHER:
                launcher.getEnvironment().setSourceClasspath(new String[]{
                        "./lib/fenix-framework-core-2.0.jar",
                        "./lib/spring-context-5.2.3.RELEASE.jar",
                        "./lib/bennu-core-6.6.0.jar"}
                );
                break;
            default:
                System.exit(1);
                break;
        }

        System.out.println("Generating AST...");
        launcher.buildModel();
    }

    @Override
    public void collectControllersAndEntities() {
        for(CtType<?> clazz : factory.Class().getAll()) {
            List<CtAnnotation<? extends Annotation>> annotations = clazz.getAnnotations();

            if (existsAnnotation(annotations, "Controller") ||
                existsAnnotation(annotations, "RestController") ||
                existsAnnotation(annotations, "RequestMapping") ||
                existsAnnotation(annotations, "GetMapping") ||
                existsAnnotation(annotations, "PostMapping") ||
                existsAnnotation(annotations, "PatchMapping") ||
                existsAnnotation(annotations, "PutMapping") ||
                existsAnnotation(annotations, "DeleteMapping") ||
                clazz.getSimpleName().endsWith("Controller") ||
                (clazz.getSuperclass() != null && clazz.getSuperclass().getSimpleName().equals("FenixDispatchAction"))
            ) {
                controllers.add((CtClass) clazz);
            } else {  //Domain class
                CtTypeReference<?> superclassReference = clazz.getSuperclass();
                if (superclassReference != null && superclassReference.getSimpleName().endsWith("_Base")) {
                    allDomainEntities.add(clazz.getSimpleName());
                }
            }
            allEntities.add(clazz.getSimpleName());
        }
    }

    @Override
    public void methodCallDFS(
            CtExecutable callerMethod,
            CtAbstractInvocation prevCalleeLocation,
            Stack<SourcePosition> methodStack,
            Stack<String> toReturnNodeIdStack
    ) {
        methodStack.push(callerMethod.getPosition());

        callerMethod.accept(new CtScanner() {
            private boolean lastStatementWasReturnValue;
            private Stack<String> branchOriginNodeId = new Stack<>();
            private Stack<Boolean> lastStatementWasReturnStack = new Stack<>();
            private Stack<Node> afterNode = new Stack<>();

            private <T> void visitCtAbstractInvocation(CtAbstractInvocation calleeLocation) {
                try {
                    if (calleeLocation == null)
                        return;
                    else if (calleeLocation.getExecutable().getDeclaringType().getSimpleName().endsWith("_Base")) {
                        registerBaseClass(calleeLocation.getExecutable().getExecutableDeclaration(), calleeLocation);
                    }
                    else if (calleeLocation.getExecutable().getDeclaringType().getSimpleName().equals("FenixFramework") &&
                            calleeLocation.getExecutable().getSimpleName().equals("getDomainObject")) {
                        registerDomainObject(calleeLocation);
                    }
                    else if (allEntities.contains(calleeLocation.getExecutable().getDeclaringType().getSimpleName())) {
                        if (!methodStack.contains(calleeLocation.getExecutable().getExecutableDeclaration().getPosition())) {
                            methodCallDFS(calleeLocation.getExecutable().getExecutableDeclaration(), calleeLocation, methodStack, toReturnNodeIdStack);
                        }
                    }
                } catch (Exception e) {
                    // cast error, proceed
                }
            }

            @Override
            public <S> void visitCtSwitch(CtSwitch<S> switchStatement) {
                afterNode.push(createGraphNode(switchStatement.toString() + "END"));
                super.visitCtSwitch(switchStatement);
                currentParentNodeId = afterNode.pop().getId();
            }

            @Override
            public <T, S> void visitCtSwitchExpression(CtSwitchExpression<T, S> switchExpression) {
                super.visitCtSwitchExpression(switchExpression);
                System.err.println("visitCtSwitchExpression not supported");
                System.exit(1);
            }

            @Override
            public <R> void visitCtReturn(CtReturn<R> returnStatement) {
                super.visitCtReturn(returnStatement);
                lastStatementWasReturnStack.pop();
                lastStatementWasReturnStack.push(true);
            }

            @Override
            public <R> void visitCtBlock(CtBlock<R> block) {
                afterNode.push(createGraphNode(block.toString() + "END"));
                lastStatementWasReturnStack.push(false);
                super.visitCtBlock(block);
                lastStatementWasReturnValue = lastStatementWasReturnStack.pop();
                Node pop = afterNode.pop();
                linkNodes(currentParentNodeId, pop.getId());
                currentParentNodeId = pop.getId();
            }

            @Override
            public void scan(CtRole role, CtElement element) {
                // pre stuff before scan
                switch (role) {
                    case THEN:
                        branchOriginNodeId.push(currentParentNodeId); // then
                        branchOriginNodeId.push(currentParentNodeId); // else
                        break;
                    case ELSE:
                        break;
                    case CASE:
                        CtSwitch switchExpression = (CtSwitch) element.getParent();
                        if (switchExpression.getCases().get(0).equals(element)) {
                            // the first case will populate the origin nodes stack
                            int size = switchExpression.getCases().size();
                            for (int i = 0 ; i < size; i++)
                                branchOriginNodeId.push(currentParentNodeId);
                        }
                        // case statements are not considered to have blocks inside (although they can return)
                        lastStatementWasReturnStack.push(false);
                        break;
                    case BODY:
                        branchOriginNodeId.push(currentParentNodeId);
                        CtElement parent = element.getParent();

                        if (parent instanceof CtFor) {
                            // flow where for is not executed
                            linkNodes(currentParentNodeId, afterNode.peek().getId());
                        }
                        break;
                }

                Node elementNode = null;
                switch (role) {
                    case THEN:
                    case ELSE:
                    case CASE:
                    case BODY:
                        if (element != null) {
                            elementNode = createGraphNode(element.toString());

                            // corner case of cases without breaks/returns
                            if (role.equals(CtRole.CASE)) {
                                // if the previous case didn't end in break/return, we have to link it to the current case
                                List<CtCase> cases = ((CtSwitch) element.getParent()).getCases();
                                for (int i = 0; i < cases.size(); i++) {
                                    CtCase ctCase = cases.get(i);
                                    if (ctCase.equals(element)) {
                                        if (i != 0) {
                                            CtCase previousCase = cases.get(i - 1);
                                            List<CtStatement> previousCaseStmts = previousCase.getStatements();
                                            CtStatement lastStmt = previousCaseStmts.get(previousCaseStmts.size() - 1);
                                            if (!(lastStmt instanceof CtBreak) && !(lastStmt instanceof CtReturn)) {
                                                // the end of the case inspection link the switch case to the afterSwitchNode
                                                // by default, so we have to unlink it, and relink to the next case
                                                unlinkLast(currentParentNodeId);
                                                // link end of previous case with current case
                                                linkNodes(currentParentNodeId, elementNode.getId());
                                            }
                                        }
                                        break;
                                    }
                                }
                            }

                            currentParentNodeId = elementNode.getId();
                            linkNodes(branchOriginNodeId.pop(), elementNode.getId());
                        }
                        else { // null else case
                            // if there is no else, we can skip if and go to post-if (afterNode)
                            linkNodes(branchOriginNodeId.pop(), afterNode.peek().getId());
                        }
                        break;
                }

                super.scan(role, element);

                // pre stuff after scan
                switch (role) {
                    case CASE:
                        // case statements are not considered to have blocks inside (although they can return)
                        lastStatementWasReturnValue = lastStatementWasReturnStack.pop();
                        break;
                }

                switch (role) {
                    case THEN:
                    case ELSE:
                    case CASE:
                    case BODY:
                        if (elementNode != null) {
                            if (!lastStatementWasReturnValue) {
                                Node peek = afterNode.peek();
                                if (peek != null) {
                                    // pode não existir after node se estivermos no fim do metodo
                                    linkNodes(currentParentNodeId, peek.getId());
                                }
                            }
                            else {
                                // branch that ended in return
                                // must be linked to the end of the method execution
                                if (!toReturnNodeIdStack.isEmpty()) {
                                    String peekId = toReturnNodeIdStack.peek();
                                    linkNodes(currentParentNodeId, peekId);
                                }
                            }
                        }
                        break;
                }
            }

            @Override
            public void visitCtFor(CtFor forLoop) {
                afterNode.push(createGraphNode(forLoop.toString() + "END"));
                super.visitCtFor(forLoop);
                currentParentNodeId = afterNode.pop().getId();
            }

            @Override
            public void visitCtIf(CtIf ifElement) {
                afterNode.push(createGraphNode(ifElement.toString() + "END"));
                super.visitCtIf(ifElement);
                currentParentNodeId = afterNode.pop().getId();
            }

            @Override
            public <T> void visitCtConditional(CtConditional<T> conditional) {
                afterNode.push(createGraphNode(conditional.toString() + "END"));
                super.visitCtConditional(conditional);
                currentParentNodeId = afterNode.pop().getId();
            }

            @Override
            public void visitCtWhile(CtWhile whileLoop) {
                afterNode.push(createGraphNode(whileLoop.toString() + "END"));
                super.visitCtWhile(whileLoop);
                currentParentNodeId = afterNode.pop().getId();
            }

            @Override
            public void visitCtTryWithResource(CtTryWithResource tryWithResource) {
                super.visitCtTryWithResource(tryWithResource);
            }

            @Override
            public void visitCtTry(CtTry tryBlock) {
                super.visitCtTry(tryBlock);
            }

            @Override
            public void visitCtCatch(CtCatch catchBlock) {
                super.visitCtCatch(catchBlock);
            }

            @Override
            public void visitCtThrow(CtThrow throwStatement) {
                super.visitCtThrow(throwStatement);
            }

            @Override
            public <T> void visitCtInvocation(CtInvocation<T> invocation) {
                toReturnNodeIdStack.push(afterNode.peek().getId());

                super.visitCtInvocation(invocation);
                visitCtAbstractInvocation(invocation);

                String toReturnNodeId = toReturnNodeIdStack.pop();
            }

            @Override
            public <T> void visitCtConstructorCall(CtConstructorCall<T> ctConstructorCall) {
                super.visitCtConstructorCall(ctConstructorCall);
                visitCtAbstractInvocation(ctConstructorCall);
            }
        });

        methodStack.pop();
    }

    private void registerBaseClass(CtExecutable callee, CtAbstractInvocation calleeLocation) {
        String methodName = callee.getSimpleName();
        String mode = "";
        String returnType = "";
        List<String> argTypes = new ArrayList<>();
        if (methodName.startsWith("get")) {
            mode = "R";
            CtTypeReference returnTypeReference = callee.getType();
            if (returnTypeReference.isParameterized())
                returnType = returnTypeReference.getActualTypeArguments().get(0).getSimpleName();
            else
                returnType = returnTypeReference.getSimpleName();
        }
        else if (methodName.startsWith("set") || methodName.startsWith("add") || methodName.startsWith("remove")) {
            mode = "W";
            List<CtParameter> calleeParameters = callee.getParameters();
            for (CtParameter ctP : calleeParameters) {
                CtTypeReference parameterType = ctP.getType();
                if (parameterType.isParameterized()) {
                    for (CtTypeReference ctTR : parameterType.getActualTypeArguments())
                        argTypes.add(ctTR.getSimpleName());
                }
                else
                    argTypes.add(ctP.getType().getSimpleName());
            }
        }

        String baseClassName = callee.getParent(CtClass.class).getSimpleName();
        baseClassName = baseClassName.substring(0, baseClassName.length()-5); //remove _Base

        String resolvedType = " ";
        resolvedType = resolveTypeBase(calleeLocation);

        if (mode.equals("R")) {
            // Class Read
            if (allDomainEntities.contains(resolvedType))
                addEntitiesSequenceAccess(resolvedType, mode);
            else if (allDomainEntities.contains(baseClassName))
                addEntitiesSequenceAccess(baseClassName, mode);

            // Return Type Read
            if (allDomainEntities.contains(returnType))
                addEntitiesSequenceAccess(returnType, mode);
        }
        else if (mode.equals("W")) {
            // Class Read
            if (allDomainEntities.contains(resolvedType))
                addEntitiesSequenceAccess(resolvedType, mode);
            else if (allDomainEntities.contains(baseClassName))
                addEntitiesSequenceAccess(baseClassName, mode);

            // Argument Types Read
            for (String type : argTypes) {
                if (allDomainEntities.contains(type))
                    addEntitiesSequenceAccess(type, mode);
            }
        }
    }

    private String resolveTypeBase(CtAbstractInvocation calleeLocation) {
        CtExpression target = ((CtInvocationImpl) calleeLocation).getTarget();
        if (target.getTypeCasts().size() > 0)
            return ((CtTypeReference) target.getTypeCasts().get(0)).getSimpleName();
        else
            return target.getType().getSimpleName();
    }

    private void registerDomainObject(CtAbstractInvocation abstractCalleeLocation) throws Exception {
        // Example: final CurricularCourse course = FenixFramework.getDomainObject(values[0]);
        // Example: return (DepartmentUnit) FenixFramework.getDomainObject(this.getSelectedDepartmentUnitID());
        CtInvocation calleeLocation = (CtInvocation) abstractCalleeLocation;
        String resolvedType;
        resolvedType = resolveTypeDomainObject(calleeLocation);

        if (allDomainEntities.contains(resolvedType)) {
            addEntitiesSequenceAccess(resolvedType, "R");
        }
    }

    private String resolveTypeDomainObject(CtInvocation calleeLocation) throws Exception {
        CtElement parent = calleeLocation.getParent();
        if (calleeLocation.getTypeCasts().size() > 0)
            return ((CtTypeReference) calleeLocation.getTypeCasts().get(0)).getSimpleName();
        else if (parent instanceof CtReturnImpl)
            return parent.getParent(CtMethod.class).getType().getSimpleName();
        else if (parent instanceof CtLocalVariable)
            return ((CtLocalVariable) parent).getType().getSimpleName();
        else if (parent instanceof CtAssignment)
            return ((CtAssignment) parent).getAssigned().getType().getSimpleName();
        else
            throw new Exception("Couldn't Resolve Type.");
    }
}