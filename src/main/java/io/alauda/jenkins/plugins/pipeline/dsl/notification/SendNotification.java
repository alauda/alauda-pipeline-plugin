package io.alauda.jenkins.plugins.pipeline.dsl.notification;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import io.alauda.jenkins.plugins.pipeline.alauda.Alauda;
import io.alauda.jenkins.plugins.pipeline.dsl.AlaudaBaseStep;
import io.alauda.jenkins.plugins.pipeline.utils.MissingJenkinsConfigException;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;

public class SendNotification extends AlaudaBaseStep {

    protected String name;
    protected Map<String, Object> params;

    @DataBoundConstructor
    public SendNotification(String name) throws MissingJenkinsConfigException {
        super();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    @DataBoundSetter
    public void setSpaceName(String name){
        if(name != null){
            this.spaceName = name;
        }
    }

    public Map<String, Object> getParams() {
        return params;
    }

    @DataBoundSetter
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public Object doIt(@Nonnull Run<?, ?> run, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException {
        new Alauda(this)
                .setJenkinsContext(run, launcher, listener)
                .sendNotificationByParams(this.getSpaceName(), name, this.getProjectName(), params);
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl(){
            super(SendNotificationExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "alaudaSendNotification";
        }

        public FormValidation doCheckName(@QueryParameter String value, @QueryParameter String body){
            if(value.length()==0){
                return FormValidation.error("Name should be required");
            }
            return FormValidation.ok();
        }

        @Override
        public Step newInstance(Map<String, Object> arguments) throws Exception {
            if (!arguments.containsKey("name") || Strings.isNullOrEmpty(arguments.get("name").toString()) )
                throw new IllegalArgumentException(
                        "need to specify name");
            String name = arguments.get("name").toString();

            SendNotification step = new SendNotification(name);

            if (arguments.containsKey("params")) {
                Object params = arguments.get("params");
                if (params != null && params instanceof Map) {
                    step.setParams((Map<String, Object>)params);
                }
            }

            if (arguments.containsKey("projectName")) {
                Object projectName = arguments.get("projectName");
                if (projectName != null) {
                    step.setProjectName(projectName.toString());
                }
            }

            if (arguments.containsKey("spaceName")) {
                Object spaceName = arguments.get("spaceName");
                if (spaceName != null) {
                    step.setSpaceName(spaceName.toString());
                }
            }

            if (arguments.containsKey("verbose")) {
                boolean verbose = (boolean)arguments.getOrDefault("verbose", false);
                step.setVerbose(verbose);
            }

            return step;
        }

    }
}
