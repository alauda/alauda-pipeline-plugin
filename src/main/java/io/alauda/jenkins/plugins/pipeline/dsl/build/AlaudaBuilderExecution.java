package io.alauda.jenkins.plugins.pipeline.dsl.build;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.alauda.jenkins.plugins.pipeline.dsl.build.AlaudaBuilder;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import javax.inject.Inject;

public class AlaudaBuilderExecution extends AbstractSynchronousNonBlockingStepExecution<Object> {

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
    private transient AlaudaBuilder step;

    @Override
    protected Object run() throws Exception{
        listener.getLogger().println("Running alauda build");
        return step.doIt(build, launcher, listener);

    }

}
