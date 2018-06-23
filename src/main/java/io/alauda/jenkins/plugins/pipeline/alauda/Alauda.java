package io.alauda.jenkins.plugins.pipeline.alauda;

import com.cloudbees.groovy.cps.NonCPS;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.AbortException;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;
import io.alauda.client.AlaudaClient;
import io.alauda.client.IBuildClient;
import io.alauda.client.INotifactionClient;
import io.alauda.client.IServiceClient;
import io.alauda.jenkins.plugins.pipeline.dsl.notification.models.NotificationPayload;
import io.alauda.model.ServiceCreatePayload;
import io.alauda.model.ServiceDetails;
import io.alauda.model.ServiceUpdatePayload;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

public class Alauda {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Alauda.class.getName());
    private AlaudaClient client;

    protected String apiToken;
    protected String account;
    protected String apiEndpoint;
    protected String spaceName;
    protected String clusterName;
    protected String namespace;
    protected String projectName;

    protected boolean verbose;
    protected Logger logger;
    private String consoleURL;

    private IServiceClient serviceClient;
    private IBuildClient buildClient;
    private INotifactionClient notifactionClient;

    public Run<?, ?> run;
    public Launcher launcher;
    public TaskListener listener;

    public Alauda(IAlaudaConfig config) {
        this(
                config.getConsoleURL(),
                config.getApiEndpoint(),
                config.getApiToken(),
                config.getAccount(),
                config.getSpaceName(),
                config.getClusterName(),
                config.getNamespace(),
                config.getProjectName(),
                config.isVerbose()
        );
    }

    public Alauda(String consoleURL, String endpoint, String token, String account, String spaceName, String clusterName, String namespace, String projectName, boolean isVerbose) {
        this.consoleURL = consoleURL;
        this.apiToken = token;
        this.apiEndpoint = endpoint;
        this.account = account;
        this.spaceName = spaceName;
        this.verbose = isVerbose;
        this.clusterName = clusterName;
        this.namespace = namespace;
        this.projectName = projectName;

        this.client = new AlaudaClient(endpoint, namespace, apiToken, spaceName);
        this.serviceClient = client.getServiceClient();
        this.buildClient = client.getBuildClient();
        this.notifactionClient = client.getNotifactionClient();
    }

    public Alauda setJenkinsContext(Run<?, ?> run, Launcher launcher, TaskListener listener) {
        this.run = run;
        this.launcher = launcher;
        this.listener = listener;

        this.logger = new Logger(this.listener.getLogger());
        this.logger.setVerbose(this.verbose);

        return this;
    }

    // region Build operation
    public void deleteBuild(String buildID) throws IOException {
        logger.printf("Deleting build: %s", buildID);
        this.buildClient.deleteBuild(buildID);
        logger.printf("Deleted build: %s", buildID);
    }

    public void retrieveBuild(String buildID) throws IOException {
        logger.printf("Retrieve build: %s", buildID);
        this.buildClient.retrieveBuild(buildID);
    }

    @NonCPS
    public String startBuild(
            String spaceName,
            String buildConfigName,
            String commitID,
            String branch,
            boolean async,
            boolean ignoreBuildResult)
            throws IOException, InterruptedException {
        if (spaceName == null) {
            spaceName = this.spaceName;
        }

        logger.printf("Build:[%s/%s] Starting", spaceName, buildConfigName);

        String buildID = buildClient.startBuild(buildConfigName, commitID, branch);

        if (async) {
            logger.printf("Build:[%s/%s] Started, detail -> %s", spaceName, buildConfigName, getAlaudaBuildURL(buildID));
            return buildID;
        }
        this.listener.getLogger().flush();

        boolean isSucceed;
        try {
            logger.printf("Build:[%s/%s] Waiting build completed...", spaceName, buildConfigName);
            isSucceed = monitorBuild(buildID);
        } catch (TimeoutException ex) {
            logger.printf("Build:[%s/%s] TIMEOUT, detail -> %s", spaceName, buildConfigName, getAlaudaBuildURL(buildID));
            throw new AbortException("Build Timeout");
        } catch (InvalidDataException ex) {
            logger.printf("Build:[%s/%s] response data unexpected, detail -> %s", spaceName, buildConfigName, getAlaudaBuildURL(buildID));
            throw new AbortException("Build response data unexpected: " + ex);
        }

        if (ignoreBuildResult) {
            logger.printf("Build:[%s/%s] %s, detail -> %s", spaceName, buildConfigName, isSucceed ? "SUCCEED" : "FAIL", getAlaudaBuildURL(buildID));
            return buildID;
        }

        if (!isSucceed) {
            throw new AbortException(String.format("Build:[%s/%s] FAIL, detail -> %s", spaceName, buildConfigName, getAlaudaBuildURL(buildID)));
        }

        logger.printf("Build:[%s/%s] SUCCEED, detail -> %s", spaceName, buildConfigName, getAlaudaBuildURL(buildID));
        return buildID;
    }

    String getAlaudaBuildURL(String buildID) throws IOException {
        return AlaudaPath.getBuildUrl(this.getConsoleURL(), buildID);
    }

    boolean monitorUpdateService(String serviceID, int timeout) throws InterruptedException, IOException {
        DateTime deadline = new DateTime().plusMillis(timeout * 1000);
        int sleepMillis = 10000; // 10s
        int timer = 0;
        while (true) {
            if (deadline.isBeforeNow()) {
                throw new InterruptedException(String.format("Timeout, more than %d minutes!", timeout));
            }

            ServiceDetails details;
            try {
                timer++;
                details = serviceClient.retrieveService(serviceID);
                logger.printf("%d. Get service status: %s \n", timer, details.getResource().getStatus());
                if (details.getResource().isFinalStatus()) {
                    return details.getResource().isSucc();
                }
            } catch (IOException ex) {
                if (!ex.getMessage().contains("Unexpected code")) {
                    logger.printf("%d. Monitor status error %s , will try again \n", timer, ex.getMessage());
                }
            } finally {
                this.listener.getLogger().flush();
            }

            Thread.sleep(sleepMillis);
        }
    }

    boolean monitorBuild(String buildID) throws TimeoutException, InterruptedException, IOException, InvalidDataException {
        int timeoutHours = 2;
        int timeoutMillis = timeoutHours * 60 * 60 * 1000;

        int interval = 5000;
        int timeoutTimes = timeoutMillis / interval;

        int timer = 0;
        while (true) {
            if (timer >= timeoutTimes) {
                throw new TimeoutException(String.format("Building timeout, more than %d hours!", timeoutHours));
            }

            this.logger.print(".");
            JSONObject build = null;
            try {
                build = this.buildClient.retrieveBuild(buildID);
            } catch (IOException ex) {
                //TODO this is trick for alauda java client
                if (!ex.getMessage().contains("Unexpected code")) {
                    logger.printf("Watch build status network error %s , will try again", ex.getMessage());
                    build = this.buildClient.retrieveBuild(buildID);
                }
            }

            if (build == null) {
                throw new AbortException("Not retrieve build");
            }

            String status = getBuildStatus(build);
            logger.verbose("Build status: " + status, false);
            if (isBuildCompleted(status)) {
                this.listener.getLogger().flush();
                logger.println();
                return isBuildSucceed(status);
            }
            this.listener.getLogger().flush();
            Thread.sleep(interval);
            timer++;
        }

    }

    String getBuildStatus(JSONObject build) throws InvalidDataException {
        if (build.has("status")) {
            return build.getString("status");
        }
        throw new InvalidDataException("Got unexpected build data", build.toString());
    }

    boolean isBuildCompleted(String buildStatus) {
        return buildStatus.equals("F") || buildStatus.equals("D") || buildStatus.equals("S");
    }

    boolean isBuildSucceed(String buildStatus) {
        return buildStatus.equals("S");
    }

    // endregion

    // region Notification operation
    public void sendNotification(String spaceName, String name, String body) throws IOException {
        if (spaceName == null) {
            spaceName = this.spaceName;
        }

        String payload = toJson(prepareNotificationPayload(body));
        LOGGER.info("payload is -> %s " + payload);
        logger.printf("Notification:[%s/%s] Sending ", spaceName, name);
        this.notifactionClient.sendNotification(name, payload, spaceName);
        logger.printf("Notification:[%s/%s] Sended SUCCEED", spaceName, name);
    }

    NotificationPayload prepareNotificationPayload(String body) {

        NotificationPayload payload = new NotificationPayload();

        Result result = this.run.getResult();
        String resultStr = result == null ? getState(run) : result.toString();

        payload.setStatus(resultStr);

        String jenkinsUrl = "";
        JenkinsLocationConfiguration config = JenkinsLocationConfiguration.get();
        if (config != null) {
            jenkinsUrl = config.getUrl();
            if (jenkinsUrl != null && !jenkinsUrl.endsWith("/")) {
                jenkinsUrl = jenkinsUrl + "/";
            }
        }

        payload.setJobURL(jenkinsUrl + this.run.getUrl());

        String cause = getCause(this.run.getCauses());
        payload.setCauseText(cause);

        Date startDate = new Date(this.run.getStartTimeInMillis());
        payload.setStartedAt(startDate);

        if (result == Result.ABORTED
                || result == Result.FAILURE
                || result == Result.SUCCESS) {
            Date endDate = new Date(this.run.getStartTimeInMillis() + this.run.getDuration());
            payload.setEndedAt(endDate);
        }

        payload.setDuration(this.run.getDurationString());

        String content = String.format("The Jenkins Job %s execute notification",
                this.run.getFullDisplayName());
        payload.setContent(content);

        List<BuildData> actions = this.run.getActions(BuildData.class);
        if (actions.size() >= 1) {
            if (actions.size() > 1) {
                LOGGER.warning(String.format("%s has multi build data action, will use first one.", this.run.getFullDisplayName()));
            }
            BuildData data = actions.get(0);
            if (data.buildsByBranchName.size() >= 1) {
                String branch = data.buildsByBranchName.keySet().toArray()[0].toString();
                payload.setRepoBranch(branch);
                Revision revision = data.buildsByBranchName.get(branch).revision;
                if (revision != null) {
                    String commitID = revision.getSha1().name();
                    payload.setRepoVersion(commitID);
                }
            }

            if (data.remoteUrls.size() >= 1) {
                payload.setRepo(data.remoteUrls.toArray()[0].toString());
            }
        }

        String subject = String.format("Jenkins job %s", this.run.getFullDisplayName());
        payload.setSubject(subject);

        payload.setBody(body);
        return payload;
    }

    // endregion

    // region Service operation
    public ServiceDetails retrieveService(String serviceID) throws IOException {
        return serviceClient.retrieveService(serviceID);
    }

    public ServiceDetails retrieveService(String serviceName, String clusterName, String namespace, String projectName) throws IOException {
        return serviceClient.retrieveService(serviceName, clusterName, namespace, projectName);
    }

    public void deleteService(String serviceName, String clusterName, String namespace, String projectName) throws IOException {
        serviceClient.deleteService(serviceName, clusterName, namespace, projectName);
    }

    public String createService(ServiceCreatePayload payload,
                                Boolean async, int timeout) throws IOException, InterruptedException {
        String serviceID = serviceClient.createService(payload);
        logger.printf("createService: %s has been started, Show the details -> %s\n", serviceID, getAlaudaServiceURL(serviceID));
        if (async) {
            return serviceID;
        }
        LOGGER.info("----> monitor the create ---->");
        boolean isSucceed = monitorUpdateService(serviceID, timeout);

        if (isSucceed) {
            return serviceID;
        }

        //not success
        throw new AbortException(String.format("createService: %s has been failed.  Show the details -> %s %n",
                serviceID, getAlaudaServiceURL(serviceID)));

    }

    public String updateService(String serviceID, ServiceUpdatePayload payload,
                                Boolean async, boolean rollbackOnFail, int timeout) throws IOException, InterruptedException {
        serviceID = serviceClient.updateService(serviceID, payload);
        logger.printf("updateService: %s has been started. Show the details -> %s %n", serviceID,
                getAlaudaServiceURL(serviceID));
        if (async) {
            return serviceID;
        }

        logger.println("waiting for the service to update is complete...");
        boolean isSucceed = monitorUpdateService(serviceID, timeout);
        if (isSucceed) {
            return serviceID;
        }

        // not success
        if (rollbackOnFail) {
            logger.println("updated service failure, will try to rollback");
            serviceClient.rollbackService(serviceID);
            boolean rollbackSucceed = monitorUpdateService(serviceID, timeout);
            if (rollbackSucceed) {
                logger.println("rollbacked service finished");
            } else {
                logger.println("rollbacked service failure");
            }
            throw new AbortException(String.format("updateService: %s has been failed, already rollback.  Show the details -> %s %n",
                    serviceID, getAlaudaServiceURL(serviceID)));
        } else {
            throw new AbortException(String.format("updateService: %s has been failed.  Show the details -> %s %n",
                    serviceID, getAlaudaServiceURL(serviceID)));
        }
    }

    private String getAlaudaServiceURL(String serviceID) throws IOException {
        return AlaudaPath.getServiceUrl(this.getConsoleURL(), serviceID);
    }
    // endregion

    // region Setter and Getter
    public String getConsoleURL() {
        if (!consoleURL.endsWith("/")) {
            consoleURL = consoleURL + "/";
        }
        return consoleURL;
    }

    public void setConsoleURL(String consoleURL) {
        this.consoleURL = consoleURL;
    }

    public Alauda setSpaceName(String spaceName) {
        this.spaceName = spaceName;
        return this;
    }

    public Alauda setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
    // endregion

    static String getState(Run run) {
        if (!run.hasntStartedYet() && run.isLogUpdated()) {
            return "RUNNING";
        } else if (!run.isLogUpdated()) {
            return "FINISHED";
        } else {
            return "RUNNING";
        }
    }

    String toJson(Object object) throws IOException {
        ObjectMapper jsonMapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        jsonMapper.writeValue(sw, object);
        return sw.toString();
    }

    String getCause(List<hudson.model.Cause> causes) {
        if (causes == null) {
            LOGGER.log(Level.WARNING, "causes list is empty, will return UNKNOWN");
            return "UNKNOWN";
        }

        for (hudson.model.Cause cau : causes) {
            if (cau instanceof TimerTrigger.TimerTriggerCause) {
                return cau.getShortDescription();
            } else if (cau instanceof hudson.model.Cause.UserIdCause) {
                return cau.getShortDescription();
            } else if (cau instanceof SCMTrigger.SCMTriggerCause) {
                return cau.getShortDescription();
            } else {
                return cau.getShortDescription();
            }
        }

        LOGGER.log(Level.WARNING, "causes list is empty, will return UNKNOWN");

        return "UNKNOWN";
    }

    public static enum CauseType {
        ByUser,
        ByCRON,
        ByCodeChange,
    }

    public static class InvalidDataException extends Exception {
        private Object data;
        private String message;

        public InvalidDataException(String message, Object data) {
            super(message);
            this.message = message;
            this.data = data;
        }

        public String toString() {
            return String.format("%s, data is %s", this.getMessage(), data);
        }
    }

    protected static class Logger {
        public boolean isVerbose() {
            return verbose;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        protected boolean verbose;

        protected PrintStream stream;

        public Logger(PrintStream stream) {
            this.stream = stream;
        }

        public void verbose(String msg) {
            this.verbose(msg, true);
        }

        public void verbose(String msg, boolean newline) {
            if (verbose) {
                if (newline) {
                    if (msg != null) {
                        stream.println(msg);
                    } else {
                        stream.println();
                    }

                } else {
                    stream.print(msg);
                }
            }
        }

        public void println() {
            stream.println();
        }

        public void println(String msg) {
            stream.println(msg);
        }

        public void printf(String format, Object... args) {
            stream.printf(format, args);
        }

        public void print(String msg) {
            stream.print(msg);
        }
    }

    public static class AlaudaPath {
        public static String getBuildUrl(String endpoint, String buildID) throws IOException {
            return AlaudaPath.combine(endpoint, String.format("/console/build/history/detail/%s", buildID));
        }

        public static String getServiceUrl(String endpoint, String serviceID) throws IOException {
            return AlaudaPath.combine(endpoint, String.format("/console/service/history/detail/%s", serviceID));
        }

        public static String combine(String baseUrl, String relativeUrl) throws IOException {
            URL mergedURL = new URL(new URL(baseUrl), relativeUrl);
            return mergedURL.toString();
        }
    }
}
