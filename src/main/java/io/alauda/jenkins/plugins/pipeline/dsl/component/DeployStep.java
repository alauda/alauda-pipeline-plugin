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
import io.alauda.model.Kubernete;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class DeployStep extends AlaudaBaseStep{

    private static final Logger LOGGER = Logger.getLogger(DeployStep.class.getName());

    private boolean rollback;

    public static Logger getLOGGER() {
        return LOGGER;
    }

    private boolean async;
    private String applicationName;
    private String resourceType;
    private String componentName;
    private Kubernete payload;
    private int timeout;


    // region Setter and Getter
    public Kubernete getPayload() {
        return payload;
    }

    @DataBoundSetter
    public void setPayload(Kubernete payload) {
        this.payload = payload;
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

    public boolean getAsync() {
        return async;
    }

    @DataBoundSetter
    public void setAsync(boolean async) {
        this.async = async;
    }

    public boolean getRollback() {
        return rollback;
    }

    @DataBoundSetter
    public void setRollback(boolean rollback) {
        this.rollback = rollback;
    }

    public int getTimeout() {
        return timeout;
    }

    @DataBoundSetter
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    // endregion


    @DataBoundConstructor
    public DeployStep() throws MissingJenkinsConfigException {
        super();
    }

    public String toString() {
        return String.format("alaudaService: clusterName:%s, namespace:%s, componentName:%s, resourceType:%s",
                this.getClusterName(),
                this.getNamespace(),
                this.getComponentName(),
                this.getResourceType()
        );
    }

    @Override
    public Object doIt(@Nonnull Run<?, ?> run, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        Alauda alauda = new Alauda((IAlaudaConfig) this).setJenkinsContext(run, launcher, listener);

        return alauda.updateComponent(clusterName, resourceType, namespace, applicationName, componentName, payload, getAsync(), rollback, getTimeout());
    }

    @Symbol("component")
    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(AlaudaBaseStep.AlaudaStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "alaudaDeployComponent";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Alauda deploy component";
        }

        @Override
        public Step newInstance(Map<String, Object> arguments) throws Exception {
            if (arguments == null)
                throw new IllegalArgumentException("arguments missed");

            if (!arguments.containsKey("payload")) {
                throw new IllegalArgumentException("argument 'payload' missed");
            }

            DeployStep step = new DeployStep();
            Object payload = arguments.get("payload");
            if (payload == null) {
                throw new IllegalArgumentException("argument 'payload' is not allowed to be null");
            }
            step.setPayload((Kubernete)payload);

            String applicationName = Converter.getDataAsString(arguments, "applicationName");
            step.setApplicationName(applicationName);

            String resourceType = Converter.getDataAsString(arguments, "resourceType");
            step.setResourceType(resourceType);

            String componentName = Converter.getDataAsString(arguments, "componentName");
            step.setComponentName(componentName);

            String clusterName = Converter.getDataAsString(arguments, "clusterName");
            step.setClusterName(clusterName);

            String namespace = Converter.getDataAsString(arguments, "namespace");
            step.setNamespace(namespace);

            boolean async = Converter.getDataAsBool(arguments, "async");
            step.setAsync(async);

            boolean rollback = Converter.getDataAsBool(arguments, "rollback");
            step.setRollback(rollback);

            int timeout = Converter.getDataAsInt(arguments, "timeout", 600);
            step.setTimeout(timeout);

            return step;
        }
    }

}
