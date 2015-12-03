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

package org.jetbrains.kotlin.idea.run;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.configurations.ConfigurationUtil;
import com.intellij.execution.ui.*;
import com.intellij.ide.util.ClassFilter;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiMethodUtil;
import com.intellij.ui.EditorTextFieldWithBrowseButton;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KtLightClass;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JetRunConfigurationEditor extends SettingsEditor<JetRunConfiguration> implements PanelWithAnchor {
    private JPanel mainPanel;
    private LabeledComponent<EditorTextFieldWithBrowseButton> mainClass;

    private CommonJavaParametersPanel commonProgramParameters;
    private LabeledComponent<ModulesComboBox> moduleChooser;
    private JrePathEditor jrePathEditor;

    private final Project project;
    private final ConfigurationModuleSelector moduleSelector;
    private JComponent anchor;

    public JetRunConfigurationEditor(Project project) {
        this.project = project;
        moduleSelector = new ConfigurationModuleSelector(project, moduleChooser.getComponent());
        jrePathEditor.setDefaultJreSelector(DefaultJreSelector.fromSourceRootsDependencies(moduleChooser.getComponent(), mainClass.getComponent()));
        commonProgramParameters.setModuleContext(moduleSelector.getModule());
        moduleChooser.getComponent().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commonProgramParameters.setModuleContext(moduleSelector.getModule());
            }
        });

        //noinspection unchecked
        createKotlinClassWithMainBrowser(project, moduleSelector).setField(mainClass.getComponent());

        anchor = UIUtil.mergeComponentsWithAnchor(mainClass, commonProgramParameters, jrePathEditor, jrePathEditor, moduleChooser);
    }

    @Override
    protected void applyEditorTo(JetRunConfiguration configuration) throws ConfigurationException {
        commonProgramParameters.applyTo(configuration);
        moduleSelector.applyTo(configuration);

        String className = mainClass.getComponent().getText();
        PsiClass aClass = moduleSelector.findClass(className);

        configuration.setRunClass(aClass != null ? JavaExecutionUtil.getRuntimeQualifiedName(aClass) : className);
        configuration.setAlternativeJrePath(jrePathEditor.getJrePathOrName());
        configuration.setAlternativeJrePathEnabled(jrePathEditor.isAlternativeJreSelected());
    }

    @Override
    protected void resetEditorFrom(JetRunConfiguration configuration) {
        commonProgramParameters.reset(configuration);
        moduleSelector.reset(configuration);
        mainClass.getComponent().setText(configuration.MAIN_CLASS_NAME != null ? configuration.MAIN_CLASS_NAME.replaceAll("\\$", "\\.") : "");
        jrePathEditor.setPathOrName(configuration.ALTERNATIVE_JRE_PATH, configuration.ALTERNATIVE_JRE_PATH_ENABLED);
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return mainPanel;
    }

    private void createUIComponents() {
        mainClass = new LabeledComponent<EditorTextFieldWithBrowseButton>();
        mainClass.setComponent(new EditorTextFieldWithBrowseButton(project, true, new JavaCodeFragment.VisibilityChecker() {
            @Override
            public Visibility isDeclarationVisible(PsiElement declaration, PsiElement place) {
                if (declaration instanceof PsiClass) {
                    PsiClass aClass = (PsiClass)declaration;
                    if (ConfigurationUtil.MAIN_CLASS.value(aClass) &&
                        PsiMethodUtil.findMainMethod(aClass) != null || place.getParent() != null && moduleSelector.findClass(((PsiClass)declaration).getQualifiedName()) != null) {
                        return Visibility.VISIBLE;
                    }
                }
                return Visibility.NOT_VISIBLE;
            }
        }));
    }

    @Override
    public JComponent getAnchor() {
        return anchor;
    }

    @Override
    public void setAnchor(JComponent anchor) {
        this.anchor = anchor;
        mainClass.setAnchor(anchor);
        commonProgramParameters.setAnchor(anchor);
        jrePathEditor.setAnchor(anchor);
        moduleChooser.setAnchor(anchor);
    }

    public static ClassBrowser createKotlinClassWithMainBrowser(@NotNull Project project, @NotNull ConfigurationModuleSelector moduleSelector) {
        final ClassFilter applicationClass = new ClassFilter() {
            @Override
            public boolean isAccepted(PsiClass aClass) {
                return aClass instanceof KtLightClass && ConfigurationUtil.MAIN_CLASS.value(aClass) && findMainMethod(aClass) != null;
            }

            @Nullable
            private PsiMethod findMainMethod(final PsiClass aClass) {
                return new ReadAction<PsiMethod>() {
                    @Override
                    protected void run(@NotNull Result<PsiMethod> result) throws Throwable {
                        result.setResult(PsiMethodUtil.findMainMethod(aClass));
                    }
                }.execute().getResultObject();
            }
        };

        return new MainClassBrowser(project, moduleSelector, ExecutionBundle.message("choose.main.class.dialog.title")) {
            @Override
            protected ClassFilter createFilter(Module module) {
                return applicationClass;
            }
        };
    }

    private abstract static class MainClassBrowser extends ClassBrowser {
        protected final Project myProject;
        private final ConfigurationModuleSelector myModuleSelector;

        public MainClassBrowser(Project project, ConfigurationModuleSelector moduleSelector, String title) {
            super(project, title);
            myProject = project;
            myModuleSelector = moduleSelector;
        }

        @Override
        protected PsiClass findClass(String className) {
            return myModuleSelector.findClass(className);
        }

        @Override
        protected ClassFilter.ClassFilterWithScope getFilter() throws NoFilterException {
            Module module = myModuleSelector.getModule();
            final GlobalSearchScope scope;
            if (module == null) {
                scope = GlobalSearchScope.allScope(myProject);
            }
            else {
                scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
            }
            final ClassFilter filter = createFilter(module);
            return new ClassFilter.ClassFilterWithScope() {
                @Override
                public GlobalSearchScope getScope() {
                    return scope;
                }

                @Override
                public boolean isAccepted(PsiClass aClass) {
                    return filter == null || filter.isAccepted(aClass);
                }
            };
        }

        protected ClassFilter createFilter(Module module) {
            return null;
        }
    }
}
