/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.forge.ipaas;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import io.fabric8.forge.ipaas.dto.ComponentDto;
import io.fabric8.forge.ipaas.dto.ConnectionCatalogDto;
import io.fabric8.forge.ipaas.model.InputOptionByGroup;
import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizard;

import static io.fabric8.forge.ipaas.helper.CamelCatalogHelper.createComponentDto;
import static io.fabric8.forge.ipaas.helper.CamelCommandsHelper.createUIInputsForCamelEndpoint;

@FacetConstraint({ResourcesFacet.class})
public class ChooseConnectorOptionsCommand extends AbstractIPaaSProjectCommand implements UIWizard {

    private static final int MAX_OPTIONS = 20;

    @Inject
    private InputComponentFactory componentFactory;

    @Inject
    protected CamelCatalog camelCatalog;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ChooseConnectorOptionsCommand.class)
                .name("iPaaS: Choose Connector Options").category(Categories.create(CATEGORY))
                .description("Choose which options to enable in existing Connector");
    }

    @Override
    public boolean isEnabled(UIContext context) {
        // TODO: https://github.com/fabric8io/django/issues/79
        return true;
        /*boolean answer = super.isEnabled(context);
        if (answer) {
            FileResource<?> fileResource = getCamelConnectorFile(context);
            answer = fileResource != null && fileResource.exists();
        }
        return answer;*/
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Map<Object, Object> attributeMap = builder.getUIContext().getAttributeMap();
        attributeMap.remove("navigationResult");
    }

    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
        return null;
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        Map<Object, Object> attributeMap = context.getUIContext().getAttributeMap();

        // avoid building this NavigationResult multiple times by forge as that causes problems
        NavigationResult navigationResult = (NavigationResult) attributeMap.get("navigationResult");
        if (navigationResult != null) {
            return navigationResult;
        }

        UIContext ui = context.getUIContext();

        ConnectionCatalogDto dto = loadCamelConnectionDto(getSelectedProject(context));
        if (dto == null) {
            return null;
        }

        // there may be custom components so load them from classpath
        Project project = getSelectedProjectOrNull(context.getUIContext());
        if (project != null) {
            discoverCustomCamelComponentsOnClasspathAndAddToCatalog(camelCatalog, project);
        }

        ComponentDto component = createComponentDto(camelCatalog, dto.getBaseScheme());
        if (component == null) {
            return null;
        }

        boolean isConsumerOnly = "From".equals(dto.getSource());
        boolean isProducerOnly = "To".equals(dto.getSource());
        String camelComponentName = dto.getBaseScheme();
        String name = dto.getName();

        // what are the current chosen options
        Set<String> chosenOptions = new LinkedHashSet<>();
        if (dto.getEndpointOptions() != null) {
            for (String option : dto.getEndpointOptions()) {
                chosenOptions.add(option);
            }
        }

        List<InputOptionByGroup> groups = createUIInputsForCamelEndpoint(camelComponentName, null, null, MAX_OPTIONS,
                isConsumerOnly, isProducerOnly, true, chosenOptions, false,
                camelCatalog, componentFactory, converterFactory, ui);

        // need all inputs in a list as well
        List<InputComponent> allInputs = new ArrayList<>();
        for (InputOptionByGroup group : groups) {
            allInputs.addAll(group.getInputs());
        }

        NavigationResultBuilder builder = Results.navigationBuilder();
        int pages = groups.size();
        for (int i = 0; i < pages; i++) {
            boolean last = i == pages - 1;
            InputOptionByGroup current = groups.get(i);
            ChooseConnectorOptionsStep step = new ChooseConnectorOptionsStep(projectFactory, name,
                    current.getGroup(), allInputs, current.getInputs(), last, i, pages);
            builder.add(step);
        }

        navigationResult = builder.build();
        attributeMap.put("navigationResult", navigationResult);
        return navigationResult;
    }
}
