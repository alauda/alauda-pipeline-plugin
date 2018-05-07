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

public class AlaudaNotifier extends AlaudaBaseStep {

    @DataBoundConstructor
    public AlaudaNotifier(String name) throws MissingJenkinsConfigException {
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

    public String getBody() {
        return body;
    }

    @DataBoundSetter
    public void setBody(String body) {
        this.body = body;
    }

    protected String name;
    protected String body;

    @Override
    public Object doIt(@Nonnull Run<?, ?> run, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException {
        new Alauda(this)
                .setJenkinsContext(run, launcher, listener)
                .sendNotification(this.getSpaceName(), name, body);
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl{
        public DescriptorImpl(){
            super(AlaudaNotifierExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "alaudaNotify";
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

            AlaudaNotifier step = new AlaudaNotifier(name);

            if (arguments.containsKey("body")) {
                Object msgBody = arguments.get("body");
                if (msgBody != null) {
                    step.setBody(msgBody.toString());
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
