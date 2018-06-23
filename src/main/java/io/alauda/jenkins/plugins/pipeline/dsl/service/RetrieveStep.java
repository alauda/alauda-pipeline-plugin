package io.alauda.jenkins.plugins.pipeline.dsl.service;

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
    private static final Logger LOGGER = Logger.getLogger(RetrieveStep.class.getName());
    private String serviceID;
    private String serviceName;

    @DataBoundConstructor
    public RetrieveStep() throws MissingJenkinsConfigException {
        super();
    }

    @Override
    public Object doIt(@Nonnull Run<?, ?> run, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException {
        Alauda alauda = new Alauda((IAlaudaConfig) this)
                .setJenkinsContext(run, launcher, listener);
        if (!Strings.isNullOrEmpty(serviceID)) {
            return alauda.retrieveService(serviceID);
        }else{
            return alauda.retrieveService(serviceName, clusterName, namespace, projectName);
        }
    }

    public String getServiceID() {
        return serviceID;
    }

    @DataBoundSetter
    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }

    public String getServiceName() {
        return serviceName;
    }

    @DataBoundSetter
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

            String serviceID = Converter.getDataAsString(arguments, "serviceID");
            String serviceName = Converter.getDataAsString(arguments, "serviceName");
            String clusterName = Converter.getDataAsString(arguments, "clusterName");
            String namespace = Converter.getDataAsString(arguments, "namespace");
            String projectName = Converter.getDataAsString(arguments, "projectName");

            if (Strings.isNullOrEmpty(serviceID) && Strings.isNullOrEmpty(serviceName)) {
                throw new IllegalArgumentException("Arguments missed: either serviceID or serviceName is needed");
            }

            LOGGER.info(("Begin to retrieve the serivce"));
            RetrieveStep step = new RetrieveStep();
            if (!Strings.isNullOrEmpty(serviceID)) {
                step.setServiceID(serviceID);
            } else {
                step.setServiceName(serviceName);
                step.setClusterName(clusterName);
                step.setNamespace(namespace);
                step.setProjectName(projectName);
            }
            return step;
        }

        @Override
        public String getFunctionName() {
            return "alaudaRetrieveService";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Retrieve the service";
        }

        @Override
        public boolean isAdvanced() {
            return true;
        }

    }
}
