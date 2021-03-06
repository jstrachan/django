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

import javax.inject.Inject;

import org.apache.camel.catalog.CamelCatalog;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.projects.facets.MavenDependencyFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

@FacetConstraint({MavenDependencyFacet.class})
public class ConnectorDetailsCommand extends AbstractIPaaSProjectCommand {

    @Inject
    protected CamelCatalog camelCatalog;

    @Inject
    @WithAttributes(label = "name", required = true, description = "Name of connector")
    private UIInput<String> name;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(ConnectorDetailsCommand.class)
                .name("iPaaS: Connector Details").category(Categories.create(CATEGORY))
                .description("Display Connector details as JSon");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(name);
    }

    @Override
    public boolean isEnabled(UIContext context) {
        // TODO: https://github.com/fabric8io/django/issues/79
        return true;
        /*boolean answer = super.isEnabled(context);
        if (answer) {
            // we should only be enabled in non gui
            boolean gui = isRunningInGui(context);
            answer = !gui;
        }
        return answer;*/
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String scheme = name.getValue();

        // maybe the connector has already been added to the catalog
        String json = camelCatalog.componentJSonSchema(scheme);
        if (json == null) {
            // discover classpath and find all the connectors and add them to the catalog
            Project project = getSelectedProjectOrNull(context.getUIContext());
            if (project != null) {
                discoverCustomCamelComponentsOnClasspathAndAddToCatalog(camelCatalog, project);
            }
            // load schema again
            json = camelCatalog.componentJSonSchema(scheme);
        }

        if (json != null) {
            return Results.success(json);
        } else {
            return Results.fail("Connector " + scheme + " not found");
        }
    }

}
