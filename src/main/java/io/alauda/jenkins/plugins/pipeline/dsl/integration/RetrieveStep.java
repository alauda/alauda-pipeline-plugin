package io.alauda.jenkins.plugins.pipeline.dsl.integration;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.Run;
import io.alauda.jenkins.plugins.pipeline.alauda.Alauda;
import io.alauda.jenkins.plugins.pipeline.alauda.IAlaudaConfig;
import io.alauda.jenkins.plugins.pipeline.dsl.AlaudaBaseStep;
import io.alauda.jenkins.plugins.pipeline.utils.Converter;
import io.alauda.jenkins.plugins.pipeline.utils.MissingJenkinsConfigException;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.google.common.base.Strings;

public class RetrieveStep extends AlaudaBaseStep {
    private static final Logger LOGGER = Logger.getLogger(RetrieveStep.class.getName());
    private String instanceUUID;

    @DataBoundConstructor
    public RetrieveStep() throws MissingJenkinsConfigException {
        super();
    }

    @Override
    public Object doIt(@Nonnull Run<?, ?> run, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException {
        Alauda alauda = new Alauda((IAlaudaConfig) this)
                .setJenkinsContext(run, launcher, listener);
        if (!Strings.isNullOrEmpty(instanceUUID)) {
            return alauda.retrieveIntegration(instanceUUID, projectName);
        }else{
            throw new IllegalArgumentException("Argument instanceUUID missed");
        }
    }

    public String getInstanceUUID() {
        return instanceUUID;
    }

    @DataBoundSetter
    public void setInstanceUUID(String instanceUUID) {
        this.instanceUUID = instanceUUID;
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

            String instanceUUID = Converter.getDataAsString(arguments, "instanceUUID");
            String projectName = Converter.getDataAsString(arguments, "projectName");

            if (Strings.isNullOrEmpty(instanceUUID)) {
                throw new IllegalArgumentException("Arguments missed: instanceUUID is needed");
            }

            LOGGER.info(("Begin to retrieve the Instance"));
            RetrieveStep step = new RetrieveStep();
            step.setInstanceUUID(instanceUUID);
            step.setProjectName(projectName);
            return step;
        }

        @Override
        public String getFunctionName() {
            return "alaudaRetrieveIntegration";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Retrieve the integration";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }

    }
}
