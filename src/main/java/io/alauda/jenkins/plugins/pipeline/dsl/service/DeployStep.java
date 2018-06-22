package io.alauda.jenkins.plugins.pipeline.dsl.service;

import com.google.common.base.Strings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import io.alauda.jenkins.plugins.pipeline.AlaudaConfiguration;
import io.alauda.jenkins.plugins.pipeline.alauda.IAlaudaConfig;
import io.alauda.jenkins.plugins.pipeline.dsl.AlaudaBaseStep;

import hudson.Launcher;
import hudson.model.*;
import hudson.util.FormValidation;
import io.alauda.jenkins.plugins.pipeline.dsl.AlaudaBaseStep;
import io.alauda.jenkins.plugins.pipeline.alauda.Alauda;
import io.alauda.jenkins.plugins.pipeline.dsl.build.AlaudaBuilder;
import io.alauda.jenkins.plugins.pipeline.dsl.build.AlaudaBuilderExecution;
import io.alauda.jenkins.plugins.pipeline.utils.Converter;
import io.alauda.jenkins.plugins.pipeline.utils.MissingJenkinsConfigException;
import io.alauda.model.Kubernete;
import io.alauda.model.ServiceCreatePayload;
import io.alauda.model.ServiceDetails;
import io.alauda.model.ServiceUpdatePayload;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static io.alauda.jenkins.plugins.pipeline.utils.Converter.getDataAsBool;

public class DeployStep extends AlaudaBaseStep {
    private static final Logger LOGGER = Logger.getLogger(DeployStep.class.getName());

    private boolean rollback;

    public static Logger getLOGGER() {
        return LOGGER;
    }

    private boolean async;
    private String serviceID;
    private ServiceUpdatePayload updatePayload;
    private ServiceCreatePayload createPayload;
    private int timeout;

    @DataBoundConstructor
    public DeployStep() throws MissingJenkinsConfigException {
        super();
    }

    public String toString() {
        return String.format("alaudaService: clusterName:%s, namespace:%s, serviceID:%s, projectName:%s",
                this.getClusterName(),
                this.getNamespace(),
                this.getServiceID(),
                this.getProjectName()
        );
    }

    @Override
    public Object doIt(@Nonnull Run<?, ?> run, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        Alauda alauda = new Alauda((IAlaudaConfig) this).setJenkinsContext(run, launcher, listener);
        if (!Strings.isNullOrEmpty(serviceID)) {
            getLOGGER().info("env from -->"+ updatePayload);
            return alauda.updateService(serviceID, updatePayload, getAsync(), rollback, getTimeout());
        } else {
            return alauda.createService(createPayload, getAsync(), getTimeout());
        }
    }

    @Symbol("service")
    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(AlaudaStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "alaudaDeployService";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Alauda deploy service";
        }

        @Override
        public Step newInstance(Map<String, Object> arguments) throws Exception {
            if (arguments == null)
                throw new IllegalArgumentException("arguments missed");

            if (!arguments.containsKey("payload")) {
                throw new IllegalArgumentException("argument 'payload' missed");
            }

            Object payload = arguments.get("payload");
            if (payload == null) {
                throw new IllegalArgumentException("argument 'payload' is not allowed to be null");
            }

            DeployStep step = new DeployStep();
            boolean async = Converter.getDataAsBool(arguments, "async");
            step.setAsync(async);

            boolean rollback = Converter.getDataAsBool(arguments, "rollback");
            step.setRollback(rollback);

            int timeout = Converter.getDataAsInt(arguments, "timeout", 600);
            step.setTimeout(timeout);


            String serviceID = Converter.getDataAsString(arguments, "serviceID");
            if (!Strings.isNullOrEmpty(serviceID)) {
                // update service
                ServiceUpdatePayload updatePayload = (ServiceUpdatePayload) payload;
                step.setServiceID(serviceID);
                step.setUpdatePayload(updatePayload);

            } else {
                // create service
                ServiceCreatePayload createPayload = (ServiceCreatePayload) payload;
                step.setCreatePayload(createPayload);
            }

            return step;
        }
    }

    // region Setter and Getter
    public ServiceUpdatePayload getUpdatePayload() {
        return updatePayload;
    }

    @DataBoundSetter
    public void setUpdatePayload(ServiceUpdatePayload updatePayload) {
        this.updatePayload = updatePayload;
    }

    public ServiceCreatePayload getCreatePayload() {
        return createPayload;
    }

    @DataBoundSetter
    public void setCreatePayload(ServiceCreatePayload createPayload) {
        this.createPayload = createPayload;
    }

    public String getServiceID() {
        return serviceID;
    }

    @DataBoundSetter
    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
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

}
