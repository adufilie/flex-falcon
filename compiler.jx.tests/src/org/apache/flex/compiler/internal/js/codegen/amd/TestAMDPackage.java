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

package org.apache.flex.compiler.internal.js.codegen.amd;

import java.io.IOException;

import org.apache.flex.compiler.clients.IBackend;
import org.apache.flex.compiler.internal.as.codegen.TestPackage;
import org.apache.flex.compiler.internal.js.driver.amd.AMDBackend;
import org.apache.flex.compiler.tree.as.IFileNode;
import org.apache.flex.compiler.tree.as.IFunctionNode;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This class tests the production of AMD JavaScript for AS package.
 * 
 * @author Michael Schmalle
 */
public class TestAMDPackage extends TestPackage
{

    @Override
    @Test
    public void testPackage_Simple()
    {
        IFileNode node = getFileNode("package{}");
        visitor.visitFile(node);
        assertOut("");
    }

    @Override
    @Test
    public void testPackage_SimpleName()
    {
        IFileNode node = getFileNode("package foo {}");
        visitor.visitFile(node);
        assertOut("");
    }

    @Override
    @Test
    public void testPackage_Name()
    {
        IFileNode node = getFileNode("package foo.bar.baz {}");
        visitor.visitFile(node);
        assertOut("");
    }

    @Override
    @Test
    public void testPackageSimple_Class()
    {
        IFileNode node = getFileNode("package {public class A{}}");
        visitor.visitFile(node);
        //assertOut("");
    }

    @Ignore
    @Test
    public void testPackageSimple_TestA() throws IOException
    {
    }

    @Override
    @Test
    public void testPackageQualified_Class()
    {
        IFileNode node = getFileNode("package foo.bar.baz {public class A{}}");
        visitor.visitFile(node);
        //assertOut("");
    }

    @Override
    @Test
    public void testPackageQualified_ClassBody()
    {
        IFileNode node = getFileNode("package foo.bar.baz {public class A{public function A(){}}}");
        visitor.visitFile(node);
        //assertOut("");
    }

    @Override
    @Test
    public void testPackageQualified_ClassBodyMethodContents()
    {
        IFileNode node = getFileNode("package foo.bar.baz {public class A{public function A(){if (a){for (var i:Object in obj){doit();}}}}}");
        visitor.visitFile(node);
        //assertOut("");
    }

    //@Test
    public void testMethod()
    {
        IFunctionNode node = getMethod("function foo(){}");
        visitor.visitFunction(node);
        assertOut("A.prototype.foo = function() {\n}");
    }

    @Override
    protected IBackend createBackend()
    {
        return new AMDBackend();
    }

    protected IFileNode getFile(String code)
    {
        IFileNode node = getFileNode(code);
        return node;
    }
}