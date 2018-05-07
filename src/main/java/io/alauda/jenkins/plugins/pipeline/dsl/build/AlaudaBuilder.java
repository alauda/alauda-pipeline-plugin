package io.alauda.jenkins.plugins.pipeline.dsl.build;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.util.FormValidation;
import io.alauda.jenkins.plugins.pipeline.AlaudaConfiguration;
import io.alauda.jenkins.plugins.pipeline.alauda.Alauda;
import io.alauda.jenkins.plugins.pipeline.alauda.IAlaudaConfig;
import io.alauda.jenkins.plugins.pipeline.dsl.AlaudaBaseStep;
import io.alauda.jenkins.plugins.pipeline.dsl.build.AlaudaBuilderExecution;
import io.alauda.jenkins.plugins.pipeline.utils.MissingJenkinsConfigException;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.jenkinsci.plugins.workflow.steps.Step;

public class AlaudaBuilder extends AlaudaBaseStep {
    private static final Logger LOGGER = Logger.getLogger(AlaudaBuilder.class.getName());

    @CheckForNull
    protected String buildConfigName;
    protected Boolean async;
    protected String branch;
    protected String commitID;
    protected Boolean ignoreBuildResult;


    @DataBoundConstructor
    public AlaudaBuilder(String buildConfigName) throws MissingJenkinsConfigException {
        super();
        this.buildConfigName = buildConfigName;
    }

    public String toString() {
        return String.format("alaudaBuild apiEndpoint:%s, namespace:%s, branch:%s, commitID:%s, async:%b",
                this.getApiEndpoint(), this.getNamespace(), this.getBranch(), this.getCommitID(), this.getAsync());
    }

    public Object doIt(@Nonnull Run<?, ?> run, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        String buildID = new Alauda((IAlaudaConfig) this)
                .setJenkinsContext(run, launcher, listener)
                .startBuild(
                        this.getSpaceName(), this.getBuildConfigName(),
                        this.getCommitID(), this.getBranch(), this.getAsync(),
                        this.getIgnoreBuildResult());
        return buildID;
    }

    @Symbol("alaudaStartBuild")
    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public FormValidation doCheckBuildConfigName(@QueryParameter String value, @QueryParameter boolean useFrench)
                throws IOException, ServletException {
            return FormValidation.validateRequired(value);
        }

        public DescriptorImpl() {
            super(AlaudaBuilderExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "alaudaStartBuild";
        }

        @Override
        public String getDisplayName() {
            return "alaudaStartBuild";
        }

        public String defaultSpaceName() {
            return AlaudaConfiguration.get().getSpaceName();
        }

        @Override
        public Step newInstance(Map<String, Object> arguments) throws Exception {
            if (!arguments.containsKey("buildConfigName") || Strings.isNullOrEmpty(arguments.get("buildConfigName").toString()))
                throw new IllegalArgumentException(
                        "need to specify buildConfigName");
            String buildConfigName = arguments.get("buildConfigName").toString();

            AlaudaBuilder step = new AlaudaBuilder(buildConfigName);

            if (arguments.containsKey("commitID")) {
                Object commitID = arguments.get("commitID");
                if (commitID != null) {
                    step.setCommitID(commitID.toString());
                }
            }

            if (arguments.containsKey("branch")) {
                Object branch = arguments.get("branch");
                if (branch != null) {
                    step.setBranch(branch.toString());
                }
            }

            if (arguments.containsKey("async")) {
                Object async = arguments.get("async");
                if (async != null && async.toString().toLowerCase().equals("false")) {
                    step.setAsync(false);
                } else {
                    step.setAsync(true);
                }
            }

            if (arguments.containsKey("spaceName")) {
                Object spaceName = arguments.get("spaceName");
                step.setSpaceName(spaceName.toString());
            }

            if (arguments.containsKey("verbose")) {
                boolean verbose = (boolean) arguments.getOrDefault("verbose", false);
                step.setVerbose(verbose);
            }

            if (arguments.containsKey("ignoreBuildResult")) {
                Object ignoreBuildResult = arguments.get("ignoreBuildResult");
                if (ignoreBuildResult != null && ignoreBuildResult.toString().toLowerCase().equals("true")) {
                    step.setIgnoreBuildResult(true);
                } else {
                    step.setIgnoreBuildResult(false);
                }
            }

            return step;
        }

//        @Override
//        public Builder newInstance(@Nullable StaplerRequest req, @Nonnull JSONObject formData) throws FormException {
//            if(!formData.has("buildConfigName") || Strings.isNullOrEmpty(formData.getString("buildConfigName"))){
//                throw new IllegalArgumentException(
//                        "need to specify buildConfigName");
//            }
//            return super.newInstance(req, formData);
//        }
    }

    // region Setter and Getter
    public Boolean getIgnoreBuildResult() {
        if (ignoreBuildResult == null) {
            return false;
        }
        return ignoreBuildResult;
    }

    public void setIgnoreBuildResult(Boolean ignoreBuildResult) {
        if (ignoreBuildResult == null) {
            this.ignoreBuildResult = false;
            return;
        }
        this.ignoreBuildResult = ignoreBuildResult;
    }

    @CheckForNull
    public String getBuildConfigName() {
        return buildConfigName;
    }

    @DataBoundSetter
    public void setBuildConfigName(@CheckForNull String buildConfigName) {
        this.buildConfigName = buildConfigName;
    }

    public String getCommitID() {
        return commitID;
    }

    @DataBoundSetter
    public void setCommitID(String commitID) {
        this.commitID = commitID;
    }

    @DataBoundSetter
    public void setSpaceName(String name) {
        if (name != null) {
            this.spaceName = name;
        }
    }

    public String getBranch() {
        return branch;
    }

    @DataBoundSetter
    public void setBranch(String branch) {
        this.branch = branch;
    }

    public boolean getAsync() {
        if (async == null) {
            return false;
        }
        return async;
    }

    @DataBoundSetter
    public void setAsync(Boolean async) {
        if (async != null) {
            this.async = async;
        } else {
            this.async = false;
        }
    }
    // endregion

}
