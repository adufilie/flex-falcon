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

package org.apache.flex.abc.visitors;


/**
 * this class just passes all calls through to the delegate - used as a base
 * class when you want to modify the behavior of some of the methods.
 */
public class DelegatingClassVisitor implements IClassVisitor
{
    public DelegatingClassVisitor(IClassVisitor delegate)
    {
        this.delegate = delegate;
    }

    private IClassVisitor delegate;

    @Override
    public void visit()
    {
        delegate.visit();
    }

    @Override
    public void visitEnd()
    {
        delegate.visitEnd();
    }

    @Override
    public ITraitsVisitor visitClassTraits()
    {
        return delegate.visitClassTraits();
    }

    @Override
    public ITraitsVisitor visitInstanceTraits()
    {
        return delegate.visitInstanceTraits();
    }

}
