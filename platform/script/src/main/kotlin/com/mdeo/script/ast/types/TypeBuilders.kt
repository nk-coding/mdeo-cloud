package com.mdeo.script.ast.types

// Re-export builder functions from expression module
import com.mdeo.expression.ast.types.voidType as expressionVoidType
import com.mdeo.expression.ast.types.classType as expressionClassType
import com.mdeo.expression.ast.types.nullableClassType as expressionNullableClassType
import com.mdeo.expression.ast.types.genericClassType as expressionGenericClassType
import com.mdeo.expression.ast.types.lambdaType as expressionLambdaType
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.Parameter

// Create local aliases
fun voidType() = expressionVoidType()
fun classType(typeName: String) = expressionClassType(typeName)
fun nullableClassType(typeName: String) = expressionNullableClassType(typeName)
fun genericClassType(typeName: String, isNullable: Boolean = false, typeArgs: Map<String, ValueType>) = 
    expressionGenericClassType(typeName, isNullable, typeArgs)
fun lambdaType(returnType: ReturnType, parameters: List<Parameter>, isNullable: Boolean = false) = 
    expressionLambdaType(returnType, parameters, isNullable)
