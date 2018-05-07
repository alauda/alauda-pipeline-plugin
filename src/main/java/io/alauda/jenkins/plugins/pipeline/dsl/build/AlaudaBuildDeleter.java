package io.alauda.jenkins.plugins.pipeline.dsl.build;

import com.google.common.base.Strings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.util.FormValidation;
import io.alauda.jenkins.plugins.pipeline.alauda.Alauda;
import io.alauda.jenkins.plugins.pipeline.alauda.IAlaudaConfig;
import io.alauda.jenkins.plugins.pipeline.dsl.AlaudaBaseStep;
import io.alauda.jenkins.plugins.pipeline.utils.MissingJenkinsConfigException;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;


public class AlaudaBuildDeleter extends AlaudaBaseStep {
    private static final Logger LOGGER = Logger.getLogger(AlaudaBuildDeleter.class.getName());

    @CheckForNull
    protected String buildID;

    @CheckForNull
    public String getBuildID() {
        return buildID;
    }

    @DataBoundSetter
    public void setBuildID(@CheckForNull String buildID) {
        this.buildID = buildID;
    }


    @DataBoundConstructor
    public AlaudaBuildDeleter(String buildID) throws MissingJenkinsConfigException {
        super();
        this.buildID = buildID;
    }

    public String toString(){
        return String.format("alaudaBuildDelete buildID:%s",
                this.getBuildID());
    }

    public Object doIt(@Nonnull Run<?, ?> run, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        new Alauda((IAlaudaConfig) this)
                .setJenkinsContext(run, launcher, listener)
                .deleteBuild(this.getBuildID());
        return  true;
    }

    @Symbol("alaudaDeleteBuild")
    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public FormValidation doCheckBuildID(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("BuildID should be required!");

            return FormValidation.ok();
        }


        public DescriptorImpl(){
            super(AlaudaBuildDeleterExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "AlaudaDeleteBuild";
        }

        @Override
        public String getDisplayName() {
            return "AlaudaDeleteBuild";
        }

        @Override
        public Step newInstance(Map<String, Object> arguments) throws Exception {
            if (!arguments.containsKey("buildID") || Strings.isNullOrEmpty(arguments.get("buildID").toString()) )
                throw new IllegalArgumentException(
                        "need to specify buildID");
            String buildID = arguments.get("buildID").toString();

            AlaudaBuildDeleter step = new AlaudaBuildDeleter(buildID);

            return step;
        }

    }

    //TODO refactor Execution
    public static class AlaudaBuildDeleterExecution extends AbstractSynchronousNonBlockingStepExecution<Object> {

        private static final long serialVersionUID = 1L;

        @StepContextParameter
        private transient TaskListener listener;
        @StepContextParameter
        private transient Launcher launcher;
        @StepContextParameter
        private transient EnvVars envVars;
        @StepContextParameter
        private transient Run<?, ?> runObj;
        @StepContextParameter
        private transient FilePath filePath;
        // included for future use
        @StepContextParameter
        private transient Executor executor;
        // included for future use
        @StepContextParameter
        private transient Computer computer;

        @StepContextParameter
        private transient Run build;

        @Inject
        private transient AlaudaBuildDeleter step;

        @Override
        protected Object run() throws Exception{
            listener.getLogger().println("Running alauda delete build");
            return step.doIt(build, launcher, listener);
        }

    }

}
