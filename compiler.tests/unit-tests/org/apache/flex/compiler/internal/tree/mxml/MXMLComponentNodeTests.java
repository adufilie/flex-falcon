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

package org.apache.flex.compiler.internal.tree.mxml;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.is;

import org.apache.flex.compiler.definitions.IClassDefinition;
import org.apache.flex.compiler.tree.ASTNodeID;
import org.apache.flex.compiler.tree.mxml.IMXMLClassDefinitionNode;
import org.apache.flex.compiler.tree.mxml.IMXMLClassNode;
import org.apache.flex.compiler.tree.mxml.IMXMLComponentNode;
import org.apache.flex.compiler.tree.mxml.IMXMLFileNode;
import org.junit.Test;

/**
 * JUnit tests for {@link MXMLComponentNode}.
 * 
 * @author Gordon Smith
 */
public class MXMLComponentNodeTests extends MXMLInstanceNodeTests
{	
	private IMXMLComponentNode getMXMLComponentNode(String[] code)
	{
		IMXMLFileNode fileNode = getMXMLFileNodeWithFlex(code);
		IMXMLComponentNode node = (IMXMLComponentNode)findFirstDescendantOfType(fileNode, IMXMLComponentNode.class);
		assertThat("getNodeID", node.getNodeID(), is(ASTNodeID.MXMLComponentID));
		//assertThat("getName", node.getName(), is("Component"));
		return node;
	}
	
	// Note: getClassName() always returns null for an IMXMLComponentNode.
	// It is non-null in the case of an IFactoryNode that gets created
	// for the value of a property of type IFactory.
	
	@Test
	public void MXMLComponentNode_empty1()
	{
		String[] code = new String[]
		{
			"<fx:Declarations><fx:Component/></fx:Declarations>"
		};
		IMXMLComponentNode node = getMXMLComponentNode(code);
		assertThat("getChildCount", node.getChildCount(), is(0));
		assertThat("getID", node.getID(), is((String)null));
		assertThat("getClassNode", node.getClassNode(), is((IMXMLClassNode)null));
		assertThat("getClassName", node.getClassName(), is((String)null));
		assertThat("getContainedClassDefinitionNode", node.getContainedClassDefinitionNode(), is((IMXMLClassDefinitionNode)null));
		assertThat("getContainedClassDefinition", node.getContainedClassDefinition(), is((IClassDefinition)null));
	}
	
	@Test
	public void MXMLComponentNode_empty2()
	{
		String[] code = new String[]
		{
			"<fx:Declarations><fx:Component>",
			"</fx:Component></fx:Declarations>"
		};
		IMXMLComponentNode node = getMXMLComponentNode(code);
		assertThat("getChildCount", node.getChildCount(), is(0));
		assertThat("getID", node.getID(), is((String)null));
		assertThat("getClassNode", node.getClassNode(), is((IMXMLClassNode)null));
		assertThat("getClassName", node.getClassName(), is((String)null));
		assertThat("getContainedClassDefinitionNode", node.getContainedClassDefinitionNode(), is((IMXMLClassDefinitionNode)null));
		assertThat("getContainedClassDefinition", node.getContainedClassDefinition(), is((IClassDefinition)null));
	}
	
	@Test
	public void MXMLComponentNode_Sprite()
	{
		String[] code = new String[]
		{
		    "<fx:Declarations><fx:Component>",
		    "    <d:Sprite/>",
		    "</fx:Component></fx:Declarations>"
		};
		IMXMLComponentNode node = getMXMLComponentNode(code);
		assertThat("getChildCount", node.getChildCount(), is(1));
		assertThat("getID", node.getID(), is((String)null));
		assertThat("getClassNode", node.getClassNode(), is((IMXMLClassNode)null));
		assertThat("getClassName", node.getClassName(), is((String)null));
		assertThat("getContainedClassDefinitionNode", node.getContainedClassDefinitionNode(), is(node.getChild(0)));
		assertThat("getContainedClassDefinition", node.getContainedClassDefinition().isInstanceOf("flash.display.Sprite", project), is(true));
	}
	
	@Test
	public void MXMLComponentNode_className_Sprite()
	{
		String[] code = new String[]
		{
		    "<fx:Declarations><fx:Component className='MySprite'>",
		    "    <d:Sprite/>",
		    "</fx:Component></fx:Declarations>"
		};
		IMXMLComponentNode node = getMXMLComponentNode(code);
		assertThat("getChildCount", node.getChildCount(), is(1));
		assertThat("getID", node.getID(), is((String)null));
		assertThat("getClassNode", node.getClassNode(), is((IMXMLClassNode)null));
		assertThat("getClassName", node.getClassName(), is("MySprite"));
		assertThat("getContainedClassDefinitionNode", node.getContainedClassDefinitionNode(), is(node.getChild(0)));
		assertThat("getContainedClassDefinition", node.getContainedClassDefinition().isInstanceOf("flash.display.Sprite", project), is(true));
	}
	
	@Test
	public void MXMLComponentNode_id_Sprite()
	{
		String[] code = new String[]
		{
		    "<fx:Declarations><fx:Component id='c1'>",
		    "    <d:Sprite/>",
		    "</fx:Component></fx:Declarations>"
		};
		IMXMLComponentNode node = getMXMLComponentNode(code);
		assertThat("getChildCount", node.getChildCount(), is(1));
		assertThat("getID", node.getID(), is("c1"));
		assertThat("getClassNode", node.getClassNode(), is((IMXMLClassNode)null));
		assertThat("getClassName", node.getClassName(), is((String)null));
		assertThat("getContainedClassDefinitionNode", node.getContainedClassDefinitionNode(), is(node.getChild(0)));
		assertThat("getContainedClassDefinition", node.getContainedClassDefinition().isInstanceOf("flash.display.Sprite", project), is(true));
	}
	
	@Test
	public void MXMLComponentNode_className_id_Sprite_width_height()
	{
		String[] code = new String[]
		{
		    "<fx:Declarations><fx:Component id='c1' className='MySprite'>",
		    "    <d:Sprite width='100'>",
		    "        <d:height>100</d:height>",
		    "    </d:Sprite>",
		    "</fx:Component></fx:Declarations>"
		};
		IMXMLComponentNode node = getMXMLComponentNode(code);
		assertThat("getChildCount", node.getChildCount(), is(1));
		assertThat("getID", node.getID(), is("c1"));
		assertThat("getClassNode", node.getClassNode(), is((IMXMLClassNode)null));
		assertThat("getClassName", node.getClassName(), is("MySprite"));
		assertThat("getContainedClassDefinitionNode", node.getContainedClassDefinitionNode(), is(node.getChild(0)));
		assertThat("getContainedClassDefinition", node.getContainedClassDefinition().isInstanceOf("flash.display.Sprite", project), is(true));
		assertThat("getContainedClassDefinitionNode.getChildCount", node.getContainedClassDefinitionNode().getChildCount(), is(2));
	}
}
