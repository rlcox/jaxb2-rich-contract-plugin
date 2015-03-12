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
package com.kscs.util.plugins.xjc;

import com.kscs.util.jaxb.PropertyTree;
import com.kscs.util.jaxb.PropertyTreeUse;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JVar;

import static com.kscs.util.plugins.xjc.base.PluginUtil.nullSafe;

/**
 * @author mirko 2014-06-04
 */
class PartialCopyGenerator {
	private final ApiConstructs apiConstructs;
	private final JVar propertyTreeUseParam;
	private final JVar propertyTreeParam;
	private final JMethod copyMethod;

	public PartialCopyGenerator(final ApiConstructs apiConstructs, final JMethod copyMethod, final String propertyTreeParamName, final String propertyTreeUseParamName) {
		this.apiConstructs = apiConstructs;
		this.copyMethod = copyMethod;
		this.propertyTreeParam = copyMethod.param(JMod.FINAL, PropertyTree.class, propertyTreeParamName);
		this.propertyTreeUseParam = copyMethod.param(JMod.FINAL, PropertyTreeUse.class, propertyTreeUseParamName);
	}

	public ApiConstructs getApiConstructs() {
		return this.apiConstructs;
	}

	public JVar getPropertyTreeUseParam() {
		return this.propertyTreeUseParam;
	}

	public JVar getPropertyTreeParam() {
		return this.propertyTreeParam;
	}

	public JMethod getCopyMethod() {
		return this.copyMethod;
	}


	public void generateFieldAssignment(final JBlock body, final JFieldVar field, final JExpression targetInstanceVar, final JExpression sourceInstanceVar) {
		final JFieldRef newField = targetInstanceVar.ref(field);
		final JFieldRef fieldRef = sourceInstanceVar.ref(field);

		final JVar fieldPathVar = generatePropertyTreeVarDeclaration(body, field);
		final JExpression includeCondition = getIncludeCondition(fieldPathVar);
		final JConditional ifHasClonePath = body._if(includeCondition);
		final JBlock currentBlock = ifHasClonePath._then();
		if (field.type().isReference()) {
			final JClass fieldType = (JClass) field.type();
			if (this.apiConstructs.collectionClass.isAssignableFrom(fieldType)) {
				final JClass elementType = fieldType.getTypeParameters().get(0);
				if (this.apiConstructs.partialCopyableInterface.isAssignableFrom(elementType)) {
					final JForEach forLoop = this.apiConstructs.loop(currentBlock, fieldRef, elementType, newField, elementType);
					forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), this.apiConstructs.castOnDemand(elementType, forLoop.var().invoke(this.apiConstructs.copyMethodName).arg(fieldPathVar).arg(this.propertyTreeUseParam))));
				} else if (this.apiConstructs.copyableInterface.isAssignableFrom(elementType)) {
					final JForEach forLoop = this.apiConstructs.loop(currentBlock, fieldRef, elementType, newField, elementType);
					forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), this.apiConstructs.castOnDemand(elementType, forLoop.var().invoke(this.apiConstructs.copyMethodName))));
				} else if (this.apiConstructs.cloneableInterface.isAssignableFrom(elementType)) {
					final JBlock maybeTryBlock = this.apiConstructs.catchCloneNotSupported(currentBlock, elementType);
					final JForEach forLoop = this.apiConstructs.loop(maybeTryBlock, fieldRef, elementType, newField, elementType);
					forLoop.body().invoke(newField, "add").arg(nullSafe(forLoop.var(), this.apiConstructs.castOnDemand(elementType, forLoop.var().invoke(this.apiConstructs.cloneMethodName))));
				} else {
					currentBlock.assign(newField, nullSafe(fieldRef, this.apiConstructs.newArrayList(elementType).arg(fieldRef)));
				}

				final ImmutablePlugin immutablePlugin = this.apiConstructs.findPlugin(ImmutablePlugin.class);
				if (immutablePlugin != null) {
					immutablePlugin.immutableInit(this.apiConstructs, body, targetInstanceVar, field);
				}

			} else if (this.apiConstructs.partialCopyableInterface.isAssignableFrom(fieldType)) {
				currentBlock.assign(newField, nullSafe(fieldRef, this.apiConstructs.castOnDemand(fieldType, fieldRef.invoke(this.apiConstructs.copyMethodName).arg(fieldPathVar).arg(this.propertyTreeUseParam))));
			} else if (this.apiConstructs.copyableInterface.isAssignableFrom(fieldType)) {
				currentBlock.assign(newField, nullSafe(fieldRef, this.apiConstructs.castOnDemand(fieldType, fieldRef.invoke(this.apiConstructs.copyMethodName))));
			} else if (this.apiConstructs.cloneableInterface.isAssignableFrom(fieldType)) {
				final JBlock maybeTryBlock = this.apiConstructs.catchCloneNotSupported(currentBlock, fieldType);
				maybeTryBlock.assign(newField, nullSafe(fieldRef, this.apiConstructs.castOnDemand(fieldType, fieldRef.invoke(this.apiConstructs.cloneMethodName))));
			} else {
				currentBlock.assign(newField, fieldRef);
			}
		} else {
			currentBlock.assign(newField, fieldRef);
		}
	}


	JExpression getIncludeCondition(final JVar fieldPathVar) {
		return JOp.cond(
				this.propertyTreeUseParam.eq(this.apiConstructs.includeConst),
				fieldPathVar.ne(JExpr._null()),
				fieldPathVar.eq(JExpr._null()).cor(fieldPathVar.invoke("isLeaf").not())
		);
	}



	JVar generatePropertyTreeVarDeclaration(final JBlock body, final JFieldVar field) {
		return body.decl(JMod.FINAL, this.apiConstructs.codeModel._ref(PropertyTree.class), field.name() + "PropertyTree", JOp.cond(this.propertyTreeParam.eq(JExpr._null()), JExpr._null(), this.propertyTreeParam.invoke("get").arg(JExpr.lit(field.name()))));
	}
}
