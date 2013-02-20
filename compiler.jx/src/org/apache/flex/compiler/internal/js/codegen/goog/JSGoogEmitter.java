/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.flex.compiler.internal.js.codegen.goog;

import java.io.FilterWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.flex.compiler.common.ASModifier;
import org.apache.flex.compiler.common.ModifiersSet;
import org.apache.flex.compiler.constants.IASKeywordConstants;
import org.apache.flex.compiler.constants.IASLanguageConstants;
import org.apache.flex.compiler.definitions.IClassDefinition;
import org.apache.flex.compiler.definitions.IDefinition;
import org.apache.flex.compiler.definitions.IFunctionDefinition;
import org.apache.flex.compiler.definitions.IPackageDefinition;
import org.apache.flex.compiler.definitions.ITypeDefinition;
import org.apache.flex.compiler.internal.definitions.ClassDefinition;
import org.apache.flex.compiler.internal.js.codegen.JSEmitter;
import org.apache.flex.compiler.internal.scopes.PackageScope;
import org.apache.flex.compiler.internal.tree.as.ChainedVariableNode;
import org.apache.flex.compiler.internal.tree.as.FunctionCallNode;
import org.apache.flex.compiler.internal.tree.as.FunctionNode;
import org.apache.flex.compiler.internal.tree.as.NamespaceAccessExpressionNode;
import org.apache.flex.compiler.js.codegen.goog.IJSGoogDocEmitter;
import org.apache.flex.compiler.js.codegen.goog.IJSGoogEmitter;
import org.apache.flex.compiler.problems.ICompilerProblem;
import org.apache.flex.compiler.projects.ICompilerProject;
import org.apache.flex.compiler.scopes.IASScope;
import org.apache.flex.compiler.tree.ASTNodeID;
import org.apache.flex.compiler.tree.as.IASNode;
import org.apache.flex.compiler.tree.as.IAccessorNode;
import org.apache.flex.compiler.tree.as.IBinaryOperatorNode;
import org.apache.flex.compiler.tree.as.IClassNode;
import org.apache.flex.compiler.tree.as.IContainerNode;
import org.apache.flex.compiler.tree.as.IDefinitionNode;
import org.apache.flex.compiler.tree.as.IExpressionNode;
import org.apache.flex.compiler.tree.as.IForLoopNode;
import org.apache.flex.compiler.tree.as.IFunctionCallNode;
import org.apache.flex.compiler.tree.as.IFunctionNode;
import org.apache.flex.compiler.tree.as.IGetterNode;
import org.apache.flex.compiler.tree.as.IIdentifierNode;
import org.apache.flex.compiler.tree.as.IInterfaceNode;
import org.apache.flex.compiler.tree.as.IParameterNode;
import org.apache.flex.compiler.tree.as.IScopedNode;
import org.apache.flex.compiler.tree.as.ISetterNode;
import org.apache.flex.compiler.tree.as.ITypeNode;
import org.apache.flex.compiler.tree.as.ITypedExpressionNode;
import org.apache.flex.compiler.tree.as.IVariableNode;

/**
 * Concrete implementation of the 'goog' JavaScript production.
 * 
 * @author Michael Schmalle
 */
public class JSGoogEmitter extends JSEmitter implements IJSGoogEmitter
{
    private static final String CONSTRUCTOR_EMPTY = "emptyConstructor";
    private static final String CONSTRUCTOR_FULL = "fullConstructor";
    private static final String SUPER_FUNCTION_CALL = "replaceSuperFunction";

    public static final String GOOG_ARRAY_FOREACH = "goog.array.forEach";
    public static final String GOOG_BASE = "goog.base";
    public static final String GOOG_INHERITS = "goog.inherits";
    public static final String GOOG_PROVIDE = "goog.provide";
    public static final String GOOG_REQUIRE = "goog.require";

    public static final String SELF = "self";

    private List<String> propertyNames = new ArrayList<String>();

    IJSGoogDocEmitter getDoc()
    {
        return (IJSGoogDocEmitter) getDocEmitter();
    }

    //--------------------------------------------------------------------------
    // 
    //--------------------------------------------------------------------------

    @Override
    public void emitPackageHeader(IPackageDefinition definition)
    {
        IASScope containedScope = definition.getContainedScope();
        ITypeDefinition type = findType(containedScope.getAllLocalDefinitions());
        if (type == null)
            return;

        /* goog.provide('x');\n\n */
        write(GOOG_PROVIDE);
        write(PARENTHESES_OPEN);
        write(SINGLE_QUOTE);
        write(type.getQualifiedName());
        write(SINGLE_QUOTE);
        write(PARENTHESES_CLOSE);
        write(SEMICOLON);
        writeNewline();
        writeNewline();
    }

    @Override
    public void emitPackageHeaderContents(IPackageDefinition definition)
    {
        PackageScope containedScope = (PackageScope) definition
                .getContainedScope();

        ITypeDefinition type = findType(containedScope.getAllLocalDefinitions());
        if (type == null)
            return;

        List<String> list = resolveImports(type);
        for (String imp : list)
        {
            if (imp.indexOf(AS3) != -1)
                continue;

            /* goog.require('x');\n */
            write(GOOG_REQUIRE);
            write(PARENTHESES_OPEN);
            write(SINGLE_QUOTE);
            write(imp);
            write(SINGLE_QUOTE);
            write(PARENTHESES_CLOSE);
            write(SEMICOLON);
            writeNewline();
        }

        // (erikdebruin) only write 'closing' line break when there are 
        //               actually imports...
        if (list.size() > 1
                || (list.size() == 1 && list.get(0).indexOf(AS3) == -1))
        {
            writeNewline();
        }
    }

    @Override
    public void emitPackageContents(IPackageDefinition definition)
    {
        IASScope containedScope = definition.getContainedScope();
        ITypeDefinition type = findType(containedScope.getAllLocalDefinitions());
        if (type == null)
            return;

        IClassNode cnode = (IClassNode) type.getNode();
        if (cnode == null)
            return;

        emitClass(cnode);
    }

    @Override
    public void emitPackageFooter(IPackageDefinition definition)
    {
    }

    //--------------------------------------------------------------------------
    // 
    //--------------------------------------------------------------------------

    @Override
    public void emitClass(IClassNode node)
    {
        IClassDefinition definition = node.getDefinition();

        // constructor
        emitMethod((IFunctionNode) definition.getConstructor().getNode());
        write(SEMICOLON);

        IDefinitionNode[] dnodes = node.getAllMemberNodes();
        for (IDefinitionNode dnode : dnodes)
        {
            if (dnode.getNodeID() == ASTNodeID.VariableID)
            {
                writeNewline();
                writeNewline();
                emitField((IVariableNode) dnode);
                write(SEMICOLON);
            }
            else if (dnode.getNodeID() == ASTNodeID.FunctionID)
            {
                if (!((IFunctionNode) dnode).isConstructor())
                {
                    writeNewline();
                    writeNewline();
                    emitMethod((IFunctionNode) dnode);
                    write(SEMICOLON);
                }
            }
            else if (dnode.getNodeID() == ASTNodeID.GetterID
                    || dnode.getNodeID() == ASTNodeID.SetterID)
            {
                writeNewline();
                writeNewline();
                emitAccessors((IAccessorNode) dnode);
                write(SEMICOLON);
            }
        }
    }

    @Override
    public void emitInterface(IInterfaceNode node)
    {
        getDoc().emitInterfaceDoc(node);

        write(node.getNamespace());
        write(SPACE);

        write(IASKeywordConstants.INTERFACE);
        write(SPACE);
        getWalker().walk(node.getNameExpressionNode());
        write(SPACE);

        write(CURLYBRACE_OPEN);
        writeNewline();
        write(CURLYBRACE_CLOSE);

        final IDefinitionNode[] members = node.getAllMemberDefinitionNodes();
        for (IDefinitionNode mnode : members)
        {
            boolean isAccessor = mnode.getNodeID() == ASTNodeID.GetterID
                    || mnode.getNodeID() == ASTNodeID.SetterID;

            String qname = node.getQualifiedName();

            if (!isAccessor || !propertyNames.contains(qname))
            {
                writeNewline();

                emitMemberName(node);
                write(PERIOD);
                write(PROTOTYPE);
                write(PERIOD);
                write(mnode.getQualifiedName());

                if (isAccessor && !propertyNames.contains(qname))
                {
                    propertyNames.add(qname);
                }
                else
                {
                    write(SPACE);
                    write(EQUALS);
                    write(SPACE);
                    write(FUNCTION);

                    emitParamters(((IFunctionNode) mnode).getParameterNodes());
                }

                write(SEMICOLON);
            }
        }
    }

    @Override
    public void emitField(IVariableNode node)
    {
        IClassDefinition definition = getClassDefinition(node);

        IDefinition def = null;
        IExpressionNode enode = node.getVariableTypeNode();//getAssignedValueNode();
        if (enode != null)
            def = enode.resolveType(getWalker().getProject());

        getDoc().emitFieldDoc(node, def);

        /* x.prototype.y = z */

        ModifiersSet modifierSet = node.getDefinition().getModifiers();
        String root = "";
        if (modifierSet != null && !modifierSet.hasModifier(ASModifier.STATIC))
        {
            root = PROTOTYPE;
            root += PERIOD;
        }
        write(definition.getQualifiedName() + PERIOD + root + node.getName());

        IExpressionNode vnode = node.getAssignedValueNode();
        if (vnode != null)
        {
            write(SPACE);
            write(EQUALS);
            write(SPACE);
            getWalker().walk(vnode);
        }

        if (!(node instanceof ChainedVariableNode))
        {
            int len = node.getChildCount();
            for (int i = 0; i < len; i++)
            {
                IASNode child = node.getChild(i);
                if (child instanceof ChainedVariableNode)
                {
                    write(SEMICOLON);
                    writeNewline();
                    writeNewline();
                    emitField((IVariableNode) child);
                }
            }
        }
    }

    @Override
    public void emitVarDeclaration(IVariableNode node)
    {
        if (!(node instanceof ChainedVariableNode) && !node.isConst())
        {
            emitMemberKeyword(node);
        }

        IExpressionNode avnode = node.getAssignedValueNode();
        if (avnode != null)
        {
            IDefinition def = avnode.resolveType(getWalker().getProject());

            String opcode = avnode.getNodeID().getParaphrase();
            if (opcode != "AnonymousFunction")
                getDoc().emitVarDoc(node, def);
        }
        else
        {
            getDoc().emitVarDoc(node, null);
        }

        emitDeclarationName(node);
        emitAssignedValue(avnode);

        if (!(node instanceof ChainedVariableNode))
        {
            // check for chained variables
            int len = node.getChildCount();
            for (int i = 0; i < len; i++)
            {
                IASNode child = node.getChild(i);
                if (child instanceof ChainedVariableNode)
                {
                    write(COMMA);
                    write(SPACE);
                    emitVarDeclaration((IVariableNode) child);
                }
            }
        }
    }

    @Override
    public void emitGetAccessor(IGetterNode node)
    {
        emitObjectDefineProperty(node);
    }

    @Override
    public void emitSetAccessor(ISetterNode node)
    {
        emitObjectDefineProperty(node);
    }

    private void emitAccessors(IAccessorNode node)
    {
        String qname = node.getQualifiedName();
        if (!propertyNames.contains(qname))
        {
            emitField(node);
            write(SEMICOLON);
            writeNewline();
            writeNewline();

            propertyNames.add(qname);
        }

        if (node.getNodeID() == ASTNodeID.GetterID)
        {
            emitGetAccessor((IGetterNode) node);
        }
        else if (node.getNodeID() == ASTNodeID.SetterID)
        {
            emitSetAccessor((ISetterNode) node);
        }
    }

    @Override
    public void emitMethod(IFunctionNode node)
    {
        FunctionNode fn = (FunctionNode) node;
        fn.parseFunctionBody(new ArrayList<ICompilerProblem>());

        ICompilerProject project = getWalker().getProject();

        getDoc().emitMethodDoc(node, project);

        boolean isConstructor = node.isConstructor();

        String qname = getTypeDefinition(node).getQualifiedName();
        if (qname != null && !qname.equals(""))
        {
            write(qname);
            if (!isConstructor)
            {
                write(PERIOD);
                if (!fn.hasModifier(ASModifier.STATIC))
                {
                    write(PROTOTYPE);
                    write(PERIOD);
                }
            }
        }

        if (!isConstructor)
            emitMemberName(node);

        write(SPACE);
        write(EQUALS);
        write(SPACE);
        write(FUNCTION);

        emitParamters(node.getParameterNodes());

        boolean hasSuperClass = hasSuperClass(node);

        if (isConstructor && node.getScopedNode().getChildCount() == 0)
        {
            write(SPACE);
            write(CURLYBRACE_OPEN);
            if (hasSuperClass)
                emitSuperCall(node, CONSTRUCTOR_EMPTY);
            writeNewline();
            write(CURLYBRACE_CLOSE);
        }

        if (!isConstructor || node.getScopedNode().getChildCount() > 0)
            emitMethodScope(node.getScopedNode());

        if (isConstructor && hasSuperClass)
        {
            writeNewline();
            write(GOOG_INHERITS);
            write(PARENTHESES_OPEN);
            write(qname);
            write(COMMA);
            write(SPACE);
            String sname = getSuperClassDefinition(node, project)
                    .getQualifiedName();
            write(sname);
            write(PARENTHESES_CLOSE);
        }
    }

    @Override
    public void emitFunctionCall(IFunctionCallNode node)
    {
        IASNode cnode = node.getChild(0);

        if (cnode.getNodeID() == ASTNodeID.MemberAccessExpressionID)
            cnode = cnode.getChild(0);

        ASTNodeID id = cnode.getNodeID();
        if (id != ASTNodeID.SuperID)
        {
            if (node.isNewExpression())
            {
                write(IASKeywordConstants.NEW);
                write(SPACE);
            }

            getWalker().walk(node.getNameNode());

            write(PARENTHESES_OPEN);
            walkArguments(node.getArgumentNodes());
            write(PARENTHESES_CLOSE);
        }
        else
        {
            emitSuperCall(node, SUPER_FUNCTION_CALL);
        }
    }

    @Override
    public void emitIdentifier(IIdentifierNode node)
    {
        ICompilerProject project = getWalker().getProject();

        IClassNode cnode = (IClassNode) node
                .getAncestorOfType(IClassNode.class);

        IDefinition def = ((IIdentifierNode) node).resolve(project);

        ITypeDefinition type = ((IIdentifierNode) node).resolveType(project);

        IASNode pnode = node.getParent();
        ASTNodeID inode = pnode.getNodeID();

        boolean writeSelf = false;
        if (cnode != null)
        {
            IDefinitionNode[] members = cnode.getAllMemberNodes();
            for (IDefinitionNode mnode : members)
            {
                if ((type != null && type.getQualifiedName().equalsIgnoreCase(
                        IASLanguageConstants.Function))
                        || (def != null && def.getQualifiedName()
                                .equalsIgnoreCase(mnode.getQualifiedName())))
                {
                    if (!(pnode instanceof FunctionNode)
                            && inode != ASTNodeID.MemberAccessExpressionID)
                    {
                        writeSelf = true;
                        break;
                    }
                    else if (inode == ASTNodeID.MemberAccessExpressionID
                            && !def.isStatic())
                    {
                        String tname = type.getQualifiedName();
                        writeSelf = !tname.equalsIgnoreCase(cnode
                                .getQualifiedName())
                                && !tname.equals(IASLanguageConstants.Function);
                        break;
                    }
                }
            }
        }

        // XXX (erikdebruin) I desperately needed a way to bypass the addition
        //                   of the 'self' prefix when running the tests... Or 
        //                   I'd have to put the prefix in ~150 asserts!
        boolean isRunningInTestMode = cnode != null
                && cnode.getQualifiedName().equalsIgnoreCase("A");
        if (writeSelf && !isRunningInTestMode)
        {
            write(SELF);
            write(PERIOD);
        }
        else
        {
            String pname = (type != null) ? type.getPackageName() : "";
            if (pname != ""
                    && !pname.equalsIgnoreCase(cnode.getPackageName())
                    && inode != ASTNodeID.ArgumentID
                    && inode != ASTNodeID.VariableID
                    && inode != ASTNodeID.TypedExpressionID)
            {
                write(pname);
                write(PERIOD);
            }
        }

        super.emitIdentifier(node);
    }

    @Override
    public void emitFunctionBlockHeader(IFunctionNode node)
    {
        if (hasBody(node))
            emitSelfReference(node);

        if (node.isConstructor()
                && hasSuperClass(node) && !hasSuperCall(node.getScopedNode()))
            emitSuperCall(node, CONSTRUCTOR_FULL);

        emitRestParameterCodeBlock(node);

        emitDefaultParameterCodeBlock(node);
    }

    private void emitSelfReference(IFunctionNode node)
    {
        write(IASKeywordConstants.VAR);
        write(SPACE);
        write(SELF);
        write(SPACE);
        write(EQUALS);
        write(SPACE);
        write(IASKeywordConstants.THIS);
        write(SEMICOLON);
        writeNewline();
    }

    private void emitSuperCall(IASNode node, String type)
    {
        IFunctionNode fnode = (node instanceof IFunctionNode) ? (IFunctionNode) node
                : null;
        IFunctionCallNode fcnode = (node instanceof IFunctionCallNode) ? (FunctionCallNode) node
                : null;

        if (type == CONSTRUCTOR_EMPTY)
        {
            indentPush();
            writeNewline();
            indentPop();
        }
        else if (type == SUPER_FUNCTION_CALL)
        {
            if (fnode == null)
                fnode = (IFunctionNode) fcnode
                        .getAncestorOfType(IFunctionNode.class);
        }

        write(GOOG_BASE);
        write(PARENTHESES_OPEN);
        write(IASKeywordConstants.THIS);

        if (fnode != null && !fnode.isConstructor())
        {
            write(COMMA);
            write(SPACE);
            write(SINGLE_QUOTE);
            write(fnode.getName());
            write(SINGLE_QUOTE);
        }

        IASNode[] anodes = null;
        boolean writeArguments = false;
        if (fcnode != null)
        {
            anodes = fcnode.getArgumentNodes();

            writeArguments = anodes.length > 0;
        }
        else if (fnode.isConstructor())
        {
            anodes = fnode.getParameterNodes();

            writeArguments = (anodes != null && anodes.length > 0);
        }

        if (writeArguments)
        {
            int len = anodes.length;
            for (int i = 0; i < len; i++)
            {
                write(COMMA);
                write(SPACE);

                getWalker().walk(anodes[i]);
            }
        }

        write(PARENTHESES_CLOSE);

        if (type == CONSTRUCTOR_FULL)
        {
            write(SEMICOLON);
            writeNewline();
        }
        else if (type == CONSTRUCTOR_EMPTY)
        {
            write(SEMICOLON);
        }
    }

    private void emitDefaultParameterCodeBlock(IFunctionNode node)
    {
        IParameterNode[] pnodes = node.getParameterNodes();
        if (pnodes.length == 0)
            return;

        Map<Integer, IParameterNode> defaults = getDefaults(pnodes);

        if (defaults != null)
        {
            final StringBuilder code = new StringBuilder();

            if (!hasBody(node))
            {
                indentPush();
                write(INDENT);
            }

            List<IParameterNode> parameters = new ArrayList<IParameterNode>(
                    defaults.values());

            for (int i = 0, n = parameters.size(); i < n; i++)
            {
                IParameterNode pnode = parameters.get(i);

                if (pnode != null)
                {
                    code.setLength(0);

                    /* x = typeof y !== 'undefined' ? y : z;\n */
                    code.append(pnode.getName());
                    code.append(SPACE);
                    code.append(EQUALS);
                    code.append(SPACE);
                    code.append(IASKeywordConstants.TYPEOF);
                    code.append(SPACE);
                    code.append(pnode.getName());
                    code.append(SPACE);
                    code.append(ASTNodeID.Op_StrictNotEqualID.getParaphrase());
                    code.append(SPACE);
                    code.append(SINGLE_QUOTE);
                    code.append(IASLanguageConstants.UNDEFINED);
                    code.append(SINGLE_QUOTE);
                    code.append(SPACE);
                    code.append(ASTNodeID.TernaryExpressionID.getParaphrase());
                    code.append(SPACE);
                    code.append(pnode.getName());
                    code.append(SPACE);
                    code.append(COLON);
                    code.append(SPACE);
                    code.append(pnode.getDefaultValue());
                    code.append(SEMICOLON);

                    write(code.toString());

                    if (i == n - 1 && !hasBody(node))
                        indentPop();

                    writeNewline();
                }
            }
        }
    }

    private void emitRestParameterCodeBlock(IFunctionNode node)
    {
        IParameterNode[] pnodes = node.getParameterNodes();

        IParameterNode rest = getRest(pnodes);
        if (rest != null)
        {
            final StringBuilder code = new StringBuilder();

            /* x = Array.prototype.slice.call(arguments, y);\n */
            code.append(rest.getName());
            code.append(SPACE);
            code.append(EQUALS);
            code.append(SPACE);
            code.append(IASLanguageConstants.Array);
            code.append(PERIOD);
            code.append(PROTOTYPE);
            code.append(PERIOD);
            code.append(SLICE);
            code.append(PERIOD);
            code.append(CALL);
            code.append(PARENTHESES_OPEN);
            code.append(IASLanguageConstants.arguments);
            code.append(COMMA);
            code.append(SPACE);
            code.append(String.valueOf(pnodes.length - 1));
            code.append(PARENTHESES_CLOSE);
            code.append(SEMICOLON);

            write(code.toString());

            writeNewline();
        }
    }

    @Override
    public void emitParameter(IParameterNode node)
    {
        getWalker().walk(node.getNameExpressionNode());
    }

    @Override
    protected void emitAssignedValue(IExpressionNode node)
    {
        if (node != null)
        {
            write(SPACE);
            write(EQUALS);
            write(SPACE);
            if (node.getNodeID() == ASTNodeID.ClassReferenceID)
            {
                IDefinition definition = node.resolve(getWalker().getProject());
                write(definition.getQualifiedName());
            }
            else
            {
                getWalker().walk(node);
            }
        }
    }

    @Override
    public void emitTypedExpression(ITypedExpressionNode node)
    {
        getWalker().walk(node.getCollectionNode());
    }

    @Override
    public void emitForEachLoop(IForLoopNode node)
    {
        IContainerNode xnode = (IContainerNode) node.getChild(1);
        IBinaryOperatorNode bnode = (IBinaryOperatorNode) node
                .getConditionalsContainerNode().getChild(0);
        IVariableNode vnode = (IVariableNode) bnode.getChild(0).getChild(0);

        write(GOOG_ARRAY_FOREACH);
        write(PARENTHESES_OPEN);
        getWalker().walk(bnode.getChild(1));
        write(COMMA);
        write(SPACE);
        write(IASKeywordConstants.FUNCTION);
        write(SPACE);
        write(PARENTHESES_OPEN);
        write(vnode.getName());
        write(PARENTHESES_CLOSE);
        write(SPACE);
        if (isImplicit(xnode))
            write(CURLYBRACE_OPEN);
        getWalker().walk(node.getStatementContentsNode());
        if (isImplicit(xnode))
        {
            writeNewline();
            write(CURLYBRACE_CLOSE);
        }
        write(PARENTHESES_CLOSE);
    }

    public JSGoogEmitter(FilterWriter out)
    {
        super(out);
    }

    private Map<Integer, IParameterNode> getDefaults(IParameterNode[] nodes)
    {
        Map<Integer, IParameterNode> result = new HashMap<Integer, IParameterNode>();
        int i = 0;
        boolean hasDefaults = false;
        for (IParameterNode node : nodes)
        {
            if (node.hasDefaultValue())
            {
                hasDefaults = true;
                result.put(i, node);
            }
            else
            {
                result.put(i, null);
            }
            i++;
        }

        if (!hasDefaults)
            return null;

        return result;
    }

    private IParameterNode getRest(IParameterNode[] nodes)
    {
        for (IParameterNode node : nodes)
        {
            if (node.isRest())
                return node;
        }

        return null;
    }

    private static ITypeDefinition getTypeDefinition(IDefinitionNode node)
    {
        ITypeNode tnode = (ITypeNode) node.getAncestorOfType(ITypeNode.class);
        return (ITypeDefinition) tnode.getDefinition();
    }

    private static IClassDefinition getClassDefinition(IDefinitionNode node)
    {
        IClassNode tnode = (IClassNode) node
                .getAncestorOfType(IClassNode.class);
        return tnode.getDefinition();
    }

    private static IClassDefinition getSuperClassDefinition(
            IDefinitionNode node, ICompilerProject project)
    {
        IClassDefinition parent = (IClassDefinition) node.getDefinition()
                .getParent();
        IClassDefinition superClass = parent.resolveBaseClass(project);
        return superClass;
    }

    private boolean hasSuperClass(IDefinitionNode node)
    {
        ICompilerProject project = getWalker().getProject();
        IClassDefinition superClassDefinition = getSuperClassDefinition(node,
                project);
        // XXX (mschmalle) this is nulling for MXML super class, figure out why
        if (superClassDefinition == null)
            return false;
        String qname = superClassDefinition.getQualifiedName();
        return superClassDefinition != null
                && !qname.equals(IASLanguageConstants.Object);
    }

    private boolean hasSuperCall(IScopedNode node)
    {
        for (int i = node.getChildCount() - 1; i > -1; i--)
        {
            IASNode cnode = node.getChild(i);
            if (cnode.getNodeID() == ASTNodeID.FunctionCallID
                    && cnode.getChild(0).getNodeID() == ASTNodeID.SuperID)
                return true;
        }

        return false;
    }

    private static boolean hasBody(IFunctionNode node)
    {
        IScopedNode scope = node.getScopedNode();
        return scope.getChildCount() > 0;
    }

    private void emitObjectDefineProperty(IAccessorNode node)
    {
        /*
        Object.defineProperty(
            A.prototype, 
            'foo', 
            {get: function() {return -1;}, 
            configurable: true}
         );
        */

        FunctionNode fn = (FunctionNode) node;
        fn.parseFunctionBody(problems);

        // head
        write(IASLanguageConstants.Object);
        write(PERIOD);
        write(DEFINE_PROPERTY);
        write(PARENTHESES_OPEN);
        indentPush();
        writeNewline();

        // Type
        IFunctionDefinition definition = node.getDefinition();
        ITypeDefinition type = (ITypeDefinition) definition.getParent();
        write(type.getQualifiedName());
        if (!node.hasModifier(ASModifier.STATIC))
        {
            write(PERIOD);
            write(PROTOTYPE);
        }
        write(COMMA);
        write(SPACE);
        writeNewline();

        // name
        write(SINGLE_QUOTE);
        write(definition.getBaseName());
        write(SINGLE_QUOTE);
        write(COMMA);
        write(SPACE);
        writeNewline();

        // info object
        // declaration
        write(CURLYBRACE_OPEN);
        write(node.getNodeID() == ASTNodeID.GetterID ? IASKeywordConstants.GET
                : IASKeywordConstants.SET);
        write(COLON);
        write(FUNCTION);
        emitParamters(node.getParameterNodes());

        emitMethodScope(node.getScopedNode());

        write(COMMA);
        write(SPACE);
        write(CONFIGURABLE);
        write(COLON);
        write(IASKeywordConstants.TRUE);
        write(CURLYBRACE_CLOSE);
        indentPop();
        writeNewline();

        // tail, no colon; parent container will add it
        write(PARENTHESES_CLOSE);
    }

    //--------------------------------------------------------------------------
    // Operators
    //--------------------------------------------------------------------------

    @Override
    public void emitNamespaceAccessExpression(NamespaceAccessExpressionNode node)
    {
        getWalker().walk(node.getLeftOperandNode());
        write(PERIOD);
        getWalker().walk(node.getRightOperandNode());
    }

    @Override
    public void emitBinaryOperator(IBinaryOperatorNode node)
    {
        ASTNodeID id = node.getNodeID();

        if (id == ASTNodeID.Op_IsID)
        {
            write(ASTNodeID.Op_IsID.getParaphrase());
            write(PARENTHESES_OPEN);
            getWalker().walk(node.getLeftOperandNode());
            write(COMMA);
            write(SPACE);
            getWalker().walk(node.getRightOperandNode());
            write(PARENTHESES_CLOSE);
        }
        else if (id == ASTNodeID.Op_AsID)
        {
            // (is(a, b) ? a : null)
            write(PARENTHESES_OPEN);
            write(ASTNodeID.Op_IsID.getParaphrase());
            write(PARENTHESES_OPEN);
            getWalker().walk(node.getLeftOperandNode());
            write(COMMA);
            write(SPACE);
            getWalker().walk(node.getRightOperandNode());
            write(PARENTHESES_CLOSE);
            write(SPACE);
            write(QUESTIONMARK);
            write(SPACE);
            getWalker().walk(node.getLeftOperandNode());
            write(SPACE);
            write(COLON);
            write(SPACE);
            write(IASLanguageConstants.NULL);
            write(PARENTHESES_CLOSE);
        }
        else
        {
            getWalker().walk(node.getLeftOperandNode());

            if (id != ASTNodeID.Op_CommaID)
                write(SPACE);

            // (erikdebruin) rewrite 'a &&= b' to 'a = a && b'
            if (id == ASTNodeID.Op_LogicalAndAssignID
                    || id == ASTNodeID.Op_LogicalOrAssignID)
            {
                IIdentifierNode lnode = (IIdentifierNode) node
                        .getLeftOperandNode();

                write(EQUALS);
                write(SPACE);
                write(lnode.getName());
                write(SPACE);
                write((id == ASTNodeID.Op_LogicalAndAssignID) ? ASTNodeID.Op_LogicalAndID
                        .getParaphrase() : ASTNodeID.Op_LogicalOrID
                        .getParaphrase());
            }
            else
            {
                write(node.getOperator().getOperatorText());
            }

            write(SPACE);

            getWalker().walk(node.getRightOperandNode());
        }
    }

    //--------------------------------------------------------------------------
    // 
    //--------------------------------------------------------------------------

    private List<String> resolveImports(ITypeDefinition type)
    {
        ClassDefinition cdefinition = (ClassDefinition) type;
        ArrayList<String> list = new ArrayList<String>();
        IScopedNode scopeNode = type.getContainedScope().getScopeNode();
        if (scopeNode != null)
        {
            scopeNode.getAllImports(list);
        }
        else
        {
            // MXML
            String[] implicitImports = cdefinition.getImplicitImports();
            for (String imp : implicitImports)
            {
                list.add(imp);
            }
        }
        return list;
    }
}