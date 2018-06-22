package io.alauda.jenkins.plugins.pipeline.dsl;

import com.google.common.base.Strings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepMonitor;
import io.alauda.jenkins.plugins.pipeline.AlaudaConfiguration;
import io.alauda.jenkins.plugins.pipeline.alauda.IAlaudaConfig;
import io.alauda.jenkins.plugins.pipeline.utils.MissingJenkinsConfigException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;

public abstract class AlaudaBaseStep extends AbstractStepImpl implements SimpleBuildStep, IAlaudaConfig {
    private static final Logger LOGGER = Logger.getLogger(AlaudaBaseStep.class.getName());

    private String consoleURL;
    private String apiEndpoint;
    private String apiToken;
    private String account;
    private boolean verbose;

    protected String spaceName;
    protected String clusterName;
    protected String namespace;
    protected String projectName;

    public AlaudaBaseStep() throws MissingJenkinsConfigException {
        this(null, null, null, null, false);
    }

    public AlaudaBaseStep(boolean verbose) throws MissingJenkinsConfigException {
        this(null, null, null, null, verbose);
    }

    public AlaudaBaseStep(String spaceName, String clusterName, String namespace, String projectName, boolean verbose) throws MissingJenkinsConfigException {
        AlaudaConfiguration config = AlaudaConfiguration.get();
        this.consoleURL = config.getConsoleURL();
        this.apiEndpoint = config.getApiEndpoint();
        this.account = config.getAccount();
        this.apiToken = config.getApiToken();

        this.spaceName = Strings.isNullOrEmpty(spaceName) ? config.getSpaceName() : spaceName;
        this.clusterName = Strings.isNullOrEmpty(clusterName) ? config.getClusterName() : clusterName;
        this.namespace = Strings.isNullOrEmpty(namespace) ? config.getNamespace() : namespace;
        this.projectName = Strings.isNullOrEmpty(projectName) ? config.getProjectName() : projectName;
        this.verbose = verbose;

        this.validArgs();
    }

    private void validArgs() throws MissingJenkinsConfigException {
        if (
                Strings.isNullOrEmpty(this.consoleURL) ||
                        Strings.isNullOrEmpty(this.apiEndpoint) ||
                        Strings.isNullOrEmpty(this.account) ||
                        Strings.isNullOrEmpty(this.apiToken) ||
                        Strings.isNullOrEmpty(this.clusterName)) {
            throw MissingJenkinsConfigException.newMissingAlaudaConfig();
        }
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        return true;
    }

    public abstract Object doIt(@Nonnull Run<?, ?> run, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException;

    @Override
    // this is the workflow plugin path
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {
        this.doIt(run, launcher, listener);
    }

    @Override
    // this is the classic jenkins build step path
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        this.doIt(build, launcher, listener);
        return true;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return null;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        return project.getActions();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return null;
    }

    private String trimString(String str) {
        return str == null ? null : str.trim();
    }

    // region Setter and Getter
    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    @Override
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    @Override
    public String getConsoleURL() {
        return consoleURL;
    }

    public void setConsoleURL(String consoleURL) {
        this.consoleURL = consoleURL;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    // endregion


    public static class AlaudaStepExecution extends AbstractSynchronousNonBlockingStepExecution<Object> {

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
        private transient AlaudaBaseStep step;

        protected void println(String info) throws Exception{
            listener.getLogger().println(info);
        }

        @Override
        protected Object run() throws Exception {
            return step.doIt(build, launcher, listener);
        }

    }
}
