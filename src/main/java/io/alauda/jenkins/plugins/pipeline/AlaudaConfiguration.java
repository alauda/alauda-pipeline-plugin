package io.alauda.jenkins.plugins.pipeline;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;
import io.alauda.jenkins.plugins.pipeline.alauda.IAlaudaConfig;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalConfigurationCategory;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class AlaudaConfiguration extends GlobalConfiguration implements IAlaudaConfig {

    private String consoleURL;
    private String apiEndpoint;
    private String apiToken;
    private String account;
    private String spaceName;
    private String clusterName;
    private String namespace;
    private String projectName;


    public AlaudaConfiguration() {
        load();  // When Jenkins is restarted, load any saved configuration from disk.
    }

    public static AlaudaConfiguration get() {
        return GlobalConfiguration.all().get(AlaudaConfiguration.class);
    }

    public String toString() {
        return String.format("Endpoint:%s, account: %s, clusterName: %s; namespace:%s; projectName:%s;",
                this.apiEndpoint, this.account, this.clusterName, this.namespace, this.projectName);
    }

    @Override
    public GlobalConfigurationCategory getCategory() {
        return super.getCategory();
    }

    // region Getter and Setter and Checker
    public String getApiEndpoint() {
        return apiEndpoint;
    }

    @DataBoundSetter
    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = Util.fixEmptyAndTrim(apiEndpoint);
        save();
    }

    public FormValidation doCheckApiEndpoint(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }

    public String getConsoleURL() {
        return consoleURL;
    }

    @DataBoundSetter
    public void setConsoleURL(String consoleURL) {
        this.consoleURL = Util.fixEmptyAndTrim(consoleURL);
        save();
    }

    public FormValidation doCheckConsoleURL(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }

    public String getApiToken() {
        return apiToken;
    }

    @DataBoundSetter
    public void setApiToken(String apiToken) {
        this.apiToken = Util.fixEmptyAndTrim(apiToken);
        save();
    }

    public FormValidation doCheckApiToken(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }

    public String getAccount() {
        return account;
    }

    @DataBoundSetter
    public void setAccount(String account) {
        this.account = Util.fixEmptyAndTrim(account);
        save();
    }

    public FormValidation doCheckAccountName(@QueryParameter String value) {
        return FormValidation.ok();
    }

    public String getSpaceName() {
        return spaceName;
    }

    @DataBoundSetter
    public void setSpaceName(String spaceName) {
        this.spaceName = Util.fixEmptyAndTrim(spaceName);
        save();
    }

    public FormValidation doCheckSpaceName(@QueryParameter String value) {
        return FormValidation.ok();
    }

    public String getClusterName() {
        return clusterName;
    }

    @DataBoundSetter
    public void setClusterName(String clusterName) {
        this.clusterName = Util.fixEmptyAndTrim(clusterName);
        save();
    }

    public FormValidation doCheckClusterName(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }

    public String getNamespace()  {
        if (Strings.isNullOrEmpty(namespace)){
            return "default";
        }
        return namespace;
    }

    @DataBoundSetter
    public void setNamespace(String namespace) {
        this.namespace = Util.fixEmptyAndTrim(namespace);
        save();
    }

    public FormValidation doCheckNamespace(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }

    public String getProjectName()  {
        if (Strings.isNullOrEmpty(projectName)){
            return "";
        }
        return projectName;
    }

    @DataBoundSetter
    public void setProjectName(String projectName) {
        this.projectName = Util.fixEmptyAndTrim(projectName);
        save();
    }

    public FormValidation doCheckProjectName(@QueryParameter String value) {
        return FormValidation.validateRequired(value);
    }
    // endregion

}
