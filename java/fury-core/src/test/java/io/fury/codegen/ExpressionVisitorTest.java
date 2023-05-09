/*
 * Copyright 2023 The Fury authors
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fury.codegen;

import static org.testng.Assert.assertEquals;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import io.fury.util.ReflectionUtils;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;

public class ExpressionVisitorTest {

  @Test
  public void testTraverseExpression() throws InvocationTargetException, IllegalAccessException {
    Expression.Reference ref =
        new Expression.Reference("a", TypeToken.of(ExpressionVisitorTest.class));
    Expression e1 = new Expression.Invoke(ref, "testTraverseExpression");
    Expression.Literal start = new Expression.Literal("0");
    Expression.Literal end = new Expression.Literal("10");
    Expression.Literal step = new Expression.Literal("1");
    ExpressionVisitor.ExprHolder holder =
        ExpressionVisitor.ExprHolder.of("e1", e1, "e2", new Expression.ListExpression());
    // FIXME ListExpression#add in lambda don't get executed, so ListExpression is the last expr.
    Expression.ForLoop forLoop =
        new Expression.ForLoop(
            start,
            end,
            step,
            expr -> ((Expression.ListExpression) (holder.get("e2"))).add(holder.get("e1")));
    List<Expression> expressions = new ArrayList<>();
    new ExpressionVisitor()
        .traverseExpression(forLoop, exprSite -> expressions.add(exprSite.current));
    Preconditions.checkArgument(forLoop.action != null);
    Method writeReplace =
        ReflectionUtils.findMethods(forLoop.action.getClass(), "writeReplace").get(0);
    writeReplace.setAccessible(true);
    SerializedLambda serializedLambda = (SerializedLambda) writeReplace.invoke(forLoop.action);
    assertEquals(serializedLambda.getCapturedArgCount(), 1);
    ExpressionVisitor.ExprHolder exprHolder =
        (ExpressionVisitor.ExprHolder) (serializedLambda.getCapturedArg(0));
    assertEquals(
        expressions, Arrays.asList(forLoop, start, end, step, e1, ref, exprHolder.get("e2")));
  }
}