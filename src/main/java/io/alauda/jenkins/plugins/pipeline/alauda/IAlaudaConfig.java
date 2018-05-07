package io.alauda.jenkins.plugins.pipeline.alauda;

public interface IAlaudaConfig {
    String getConsoleURL();

    String getApiEndpoint();

    String getApiToken();

    String getAccount();

    String getSpaceName();

    String getClusterName();

    String getNamespace();

    default boolean isVerbose() {
        return false;
    }
}
