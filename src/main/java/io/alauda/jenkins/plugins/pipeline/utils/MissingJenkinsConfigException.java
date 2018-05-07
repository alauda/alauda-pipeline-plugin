package io.alauda.jenkins.plugins.pipeline.utils;

import com.google.common.base.Strings;
import jenkins.model.JenkinsLocationConfiguration;

public class MissingJenkinsConfigException extends Exception {
    private String scope;

    private MissingJenkinsConfigException(String scope) {
        this.scope = scope;
    }

    public String toString() {
        return String.format("Missing %s configuration in jenkins, You should config it firstly on %s.", this.scope, jenkinsConfigURL());
    }

    private String jenkinsConfigURL() {
        JenkinsLocationConfiguration config = JenkinsLocationConfiguration.get();
        String jenkinsUrl = config.getUrl();
        // todo here is null
        if (!Strings.isNullOrEmpty(jenkinsUrl) && !jenkinsUrl.endsWith("/")) {
            jenkinsUrl = jenkinsUrl + "/";
        }
        return jenkinsUrl + "configure";

    }

    public static MissingJenkinsConfigException NewMissingAlaudaConfig() {
        return new MissingJenkinsConfigException("Alauda");
    }

    // region Setter and Getter
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
    // endregion
}