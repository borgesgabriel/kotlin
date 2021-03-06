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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.intentions.InfixCallToOrdinaryIntention
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression

class InfixCallFix(expression: KtBinaryExpression) : KotlinQuickFixAction<KtBinaryExpression>(expression), CleanupFix {
    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        InfixCallToOrdinaryIntention.convert(element)
    }

    override fun getFamilyName(): String = text
    override fun getText(): String = "Replace infix call with ordinary call"

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val functionDescriptor = (diagnostic as? DiagnosticWithParameters2<*, *, *>)?.a as? FunctionDescriptor ?: return null
            val target = DescriptorToSourceUtilsIde.getAnyDeclaration(diagnostic.psiFile.project, functionDescriptor)
                                 as? KtModifierListOwner
            if (target == null || QuickFixUtil.canModifyElement(target)) {
                // we'll fix the problem by adding the 'infix' modifier to the target
                return null
            }

            val infixCall = (diagnostic.psiElement as? KtOperationReferenceExpression)?.parent as? KtBinaryExpression ?: return null
            return InfixCallFix(infixCall)
        }
    }
}