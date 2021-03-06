/*
 * MIT License
 *
 * Copyright (c) 2014 Klemm Software Consulting, Mirko Klemm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.kscs.util.plugins.xjc.outline;

import java.util.List;
import com.sun.codemodel.JClass;

/**
 * @author Mirko Klemm 2015-02-03
 */
public class ReferencedInterfaceOutline implements InterfaceOutline {
	private final JClass implClass;
	private final JClass supportInterface;

	public ReferencedInterfaceOutline(final JClass implClass, final String supportInterfaceNameSuffix) {
		this.implClass = implClass;
		this.supportInterface = supportInterfaceNameSuffix == null ? null : implClass.owner().ref(implClass._package().name() + "." + implClass.name() + supportInterfaceNameSuffix);
	}

	@Override
	public List<PropertyOutline> getDeclaredFields() {
		return null;
	}

	@Override
	public TypeOutline getSuperClass() {
		return null;
	}

	@Override
	public JClass getImplClass() {
		return this.implClass;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public boolean isInterface() {
		return true;
	}

	@Override
	public JClass getSupportInterface() {
		return this.supportInterface;
	}
}
