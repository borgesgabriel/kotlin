/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger.filter

import com.intellij.debugger.engine.SyntheticTypeComponentProvider
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.ClassType
import com.sun.jdi.Method
import com.sun.jdi.TypeComponent
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.org.objectweb.asm.Opcodes
import sun.tools.java.RuntimeConstants

public class KotlinSyntheticTypeComponentProvider: SyntheticTypeComponentProvider {
    override fun isSynthetic(typeComponent: TypeComponent?): Boolean {
        if (typeComponent !is Method) return false

        val typeName = typeComponent.declaringType().name()
        if (!FqNameUnsafe.isValid(typeName)) return false

        if (typeComponent.isInterfaceForwarder()) return true

        try {
            if (typeComponent.location().lineNumber() != 1) return false

            if (typeComponent.allLineLocations().any { it.lineNumber() != 1 }) {
                return false
            }

            return !typeComponent.declaringType().allLineLocations().any { it.lineNumber() != 1 }
        }
        catch(e: AbsentInformationException) {
            return false
        }
    }

    private fun Method.isInterfaceForwarder(): Boolean {
        return onlyInvokesStatic(this)
    }

    private val LOAD_INSTRUCTIONS = arrayOf(
            RuntimeConstants.opc_aload, RuntimeConstants.opc_aload_0, RuntimeConstants.opc_aload_1, RuntimeConstants.opc_aload_2, RuntimeConstants.opc_aload_3,
            RuntimeConstants.opc_iload, RuntimeConstants.opc_iload_0, RuntimeConstants.opc_iload_1, RuntimeConstants.opc_iload_2, RuntimeConstants.opc_iload_3,
            RuntimeConstants.opc_lload, RuntimeConstants.opc_lload_0, RuntimeConstants.opc_lload_1, RuntimeConstants.opc_lload_2, RuntimeConstants.opc_lload_3,
            RuntimeConstants.opc_dload, RuntimeConstants.opc_dload_0, RuntimeConstants.opc_dload_1, RuntimeConstants.opc_dload_2, RuntimeConstants.opc_dload_3,
            RuntimeConstants.opc_fload, RuntimeConstants.opc_fload_0, RuntimeConstants.opc_fload_1, RuntimeConstants.opc_fload_2, RuntimeConstants.opc_fload_3
    ).map { it.toByte() }

    private val RETURN_INSTRUCTIONS = arrayOf(
            RuntimeConstants.opc_return, RuntimeConstants.opc_areturn, RuntimeConstants.opc_ireturn, RuntimeConstants.opc_lreturn, RuntimeConstants.opc_dreturn, RuntimeConstants.opc_freturn
    ).map { it.toByte() }

    private fun onlyInvokesStatic(m: Method): Boolean {
        if (m.allLineLocations().size != 1) return false

        if (!hasOnlyInvokeStatic(m)) return false

        val declaringType = m.declaringType() as? ClassType ?: return false
        if (hasInterfaceWithImplementation(m.name(), declaringType)) return true

        val thisVar = m.variablesByName("this").singleOrNull() ?: return false
        val interfaces = declaringType.allInterfaces()
        return interfaces.any { it == thisVar.type() }
    }

    private fun hasOnlyInvokeStatic(m: Method): Boolean {

        var i = 0
        val bytecodes = m.bytecodes()
        while (i < bytecodes.size) {
            val instr = bytecodes[i]
            when (instr) {
                in LOAD_INSTRUCTIONS -> {
                    val nextIdx = ++i
                    val nextInstr = bytecodes[nextIdx]
                    if (nextInstr == Opcodes.LDC.toByte()) {
                        i += 5
                    }
                }
                Opcodes.INVOKESTATIC.toByte() -> {
                    val nextIdx = i + 3
                    val nextInstr = bytecodes[nextIdx]
                    return nextIdx == (bytecodes.size - 1) && nextInstr in RETURN_INSTRUCTIONS
                }
                else -> return false
            }

        }
        return false
    }

    // class for DefaultImpl can be not loaded
    private fun hasInterfaceWithImplementation(methodName: String, declaringType: ClassType): Boolean {
        val interfaces = declaringType.allInterfaces()
        val vm = declaringType.virtualMachine()
        val traitImpls = interfaces.flatMap { vm.classesByName(it.name() + JvmAbi.DEFAULT_IMPLS_SUFFIX) }
        return traitImpls.any { !it.methodsByName(methodName).isEmpty() }
    }
}