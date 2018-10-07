package io.alauda.jenkins.plugins.pipeline.dsl.component;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.alauda.jenkins.plugins.pipeline.alauda.Alauda;
import io.alauda.jenkins.plugins.pipeline.alauda.IAlaudaConfig;
import io.alauda.jenkins.plugins.pipeline.dsl.AlaudaBaseStep;
import io.alauda.jenkins.plugins.pipeline.utils.Converter;
import io.alauda.jenkins.plugins.pipeline.utils.MissingJenkinsConfigException;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class RetrieveStep extends AlaudaBaseStep {
    private static final Logger LOGGER = Logger.getLogger(io.alauda.jenkins.plugins.pipeline.dsl.service.RetrieveStep.class.getName());
    private String applicationName;
    private String resourceType;
    private String componentName;

    @DataBoundConstructor
    public RetrieveStep() throws MissingJenkinsConfigException {
        super();
    }

    @Override
    public Object doIt(@Nonnull Run<?, ?> run, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException {
        Alauda alauda = new Alauda((IAlaudaConfig) this)
                .setJenkinsContext(run, launcher, listener);
        return alauda.retrieveComponent(applicationName, resourceType, componentName, clusterName, namespace, projectName);
    }

    public String getApplicationName() {
        return applicationName;
    }

    @DataBoundSetter
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getResourceType() {
        return resourceType;
    }

    @DataBoundSetter
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getComponentName() {
        return componentName;
    }

    @DataBoundSetter
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(AlaudaStepExecution.class);
        }

        @Override
        public Step newInstance(Map<String, Object> arguments) throws Exception {
            if (arguments == null)
                throw new IllegalArgumentException("Arguments missed");

            String applicationName = Converter.getDataAsString(arguments, "applicationName");
            String resourceType = Converter.getDataAsString(arguments, "resourceType");
            String componentName = Converter.getDataAsString(arguments, "componentName");
            String clusterName = Converter.getDataAsString(arguments, "clusterName");
            String namespace = Converter.getDataAsString(arguments, "namespace");
            String projectName = Converter.getDataAsString(arguments, "projectName");

            if (Strings.isNullOrEmpty(applicationName) || Strings.isNullOrEmpty(resourceType) || Strings.isNullOrEmpty(componentName)) {
                throw new IllegalArgumentException("Arguments missed: applicationName, resourceType and componentName is needed");
            }

            LOGGER.info(("Begin to retrieve the component"));
            RetrieveStep step = new RetrieveStep();
            step.setApplicationName(applicationName);
            step.setResourceType(resourceType);
            step.setComponentName(componentName);
            step.setClusterName(clusterName);
            step.setNamespace(namespace);
            step.setProjectName(projectName);

            return step;
        }

        @Override
        public String getFunctionName() {
            return "alaudaRetrieveComponent";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Retrieve the Component";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }

    }
}
