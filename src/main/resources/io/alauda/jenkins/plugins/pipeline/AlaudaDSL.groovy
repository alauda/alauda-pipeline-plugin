package io.alauda.jenkins.plugins.pipeline

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.databind.DeserializationFeature
import com.google.common.base.Strings
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurperClassic
import groovy.json.JsonOutput
import hudson.AbortException
import io.alauda.model.ComponentDetails
import org.jenkinsci.plugins.workflow.cps.CpsScript
import io.alauda.model.Kubernete
import io.alauda.model.ServiceCreatePayload
import io.alauda.model.ServiceDetails
import io.alauda.model.ServiceUpdatePayload
import io.alauda.model.IntegrationDetails

import org.yaml.snakeyaml.Yaml

class AlaudaDSL implements Serializable {

    private CpsScript script;
    private Context currentContext = null;
    private transient AlaudaConfiguration alaudaConfiguration = AlaudaConfiguration.get()
    private boolean isVerbose;

    AlaudaDSL(CpsScript script) {
        this.script = script
    }

    @NonCPS
    def jsonParse(def json) {
        // https://stackoverflow.com/questions/37864542/jenkins-pipeline-notserializableexception-groovy-json-internal-lazymap
        new JsonSlurperClassic().parseText(json)
    }

    @NonCPS
    def parseArgs(argsDefine, args) {
        Map map = [:]

        if (args == null || args.length == 0) {
            return map;
        }

        if (args.length == 1) {
            if (args[0] instanceof Map) {
                return args[0]
            }

            if (args[0] instanceof String) {
                map[argsDefine[0]] = args[0].toString();
                return map;
            }
        }


        if (args instanceof Object[]) {
            for (def i = 0; i < args.length; i++) {
                def name = argsDefine[i]
                map[name] = args[i].toString();
            }

            return map;
        }
        return map;
    }

    void verbose(boolean verbose) {
        this.isVerbose = verbose;
    }

    private class Context implements Serializable {
        protected final Context parent;
        private String spaceName;
        private String clusterName;
        private String namespace;
        private String projectName;

        protected Context(Context parent) {
            this.parent = parent;
        }

        String getSpaceName() {
            if (this.spaceName != null) {
                return this.spaceName;
            }
            if (parent != null) {
                return parent.getSpaceName();
            }
            return null
        }

        Context setSpaceName(String spaceName) {
            this.spaceName = spaceName;
            return this;
        }

        String getClusterName() {
            if (this.clusterName != null) {
                return this.clusterName;
            }
            if (parent != null) {
                return parent.getClusterName();
            }
            return null
        }

        Context setClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        String getNamespace() {
            if (this.namespace != null) {
                return this.namespace;
            }
            if (parent != null) {
                return parent.getNamespace();
            }
            return null
        }

        Context setNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        String getProjectName() {
            if (this.projectName != null) {
                return this.projectName;
            }
            if (parent != null) {
                return parent.getProjectName();
            }
            return null
        }

        Context setProjectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        def <V> V run(Closure<V> body) {
            AlaudaDSL.Context last = currentContext;
            currentContext = this;
            try {
                return body()
            } finally {
                currentContext = last
            }
        }

    }

    // region with...
    def <V> V withSpace(String spaceName, Closure<V> body) {
        Context context = new Context(currentContext).setSpaceName(spaceName);
        return context.run {
            return body()
        }
    }

    def <V> V withCluster(String clusterName, Closure<V> body) {
        Context context = new Context(currentContext);
        return this.withCluster(clusterName, context.getNamespace(), body);
    }

    def <V> V withCluster(String clusterName, String namespace, Closure<V> body) {
        Context context = new Context(currentContext).setClusterName(clusterName);
        if (!Strings.isNullOrEmpty(namespace)) {
            context.setNamespace(namespace);
        }
        return context.run {
            return body()
        }
    }

    def <V> V withNamespace(String namespace, Closure<V> body) {
        Context context = new Context(currentContext).setNamespace(namespace);
        return context.run {
            return body()
        }
    }

    def <V> V withProject(String projectName, Closure<V> body) {
        Context context = new Context(currentContext).setProjectName(projectName);
        return context.run {
            return body()
        }
    }

    String consoleURL() {
        return alaudaConfiguration.getConsoleURL()
    }

    String account() {
        return alaudaConfiguration.getAccount()
    }

    String spaceName() {
        if (currentContext == null) {
            return alaudaConfiguration.getSpaceName()
        }
        return currentContext.getSpaceName();
    }

    String cluster() {
        if (currentContext == null) {
            return alaudaConfiguration.getClusterName()
        }
        return currentContext.getClusterName()
    }

    String namespace() {
        if (currentContext == null) {
            return alaudaConfiguration.getNamespace()
        }
        return currentContext.getNamespace()
    }

    String project() {
        if (currentContext == null) {
            return alaudaConfiguration.getProjectName()
        }
        return currentContext.getProjectName()
    }
    // endregion

    // region build operation
    String startBuild(Object... args) {
        def argsDefine = ["buildConfigName", "commitID", "branch", "async", "ignoreBuildResult"]
        Map map = parseArgs(argsDefine, args)

        String buildConfigName = map.get("buildConfigName", "");
        if (buildConfigName == "") {
            throw new AbortException("Missing argument buildConfigName")
        }

        String commitID = map.get("commitID", null);
        String branch = map.get("branch", null);
        boolean async = map.get("async", false);
        boolean ignoreBuildResult = map.get("ignoreBuildResult", false);

        return script.alaudaStartBuild(
                spaceName: spaceName(),
                buildConfigName: buildConfigName,
                commitID: commitID,
                branch: branch,
                async: async,
                ignoreBuildResult: ignoreBuildResult,
                verbose: this.isVerbose)

    }

    String deleteBuild(String buildID) {
        script.alaudaDeleteBuild(buildID: buildID)
    }
    // endregion

    // region notification operation
    void notify(Object... args) {
        def argsDefine = ["name", "body"]
        Map map = parseArgs(argsDefine, args)
        if (!map.containsKey("name")) {
            throw new AbortException("Missing argument name")
        }

        script.alaudaNotify(
                spaceName: spaceName(),
                name: map.getOrDefault("name", ""),
                body: map.getOrDefault("body", null),
                verbose: this.isVerbose
        )
    }
    // endregion

     @NonCPS
     def IntegrationDetails retrieveIntegration(String instanceUUID, String projectName) {
         IntegrationDetails details = script.alaudaRetrieveIntegration instanceUUID: instanceUUID, projectName: projectName
         script.println(details)
         return details
     }

    def integration(String instanceUUID) {
        script.printf("alauda.integration('%s')", instanceUUID)
        return new Integration(alauda: this, name: instanceUUID)
    }

    def integration(String instanceUUID, String projectName) {
        script.printf("alauda.integration('%s %s')", instanceUUID, projectName)
        return new Integration(alauda: this, name: instanceUUID, projectName: projectName)
    }

    def static class Integration implements Serializable {

        private AlaudaDSL alauda
        private String name
        private IntegrationDetails integration
        private String projectName

        void println(String info) {
            alauda.script.println(info)
        }

        void printf(String fm, Object[] objs) {
            alauda.script.printf(fm, objs)
        }

        Integration withProject(String projectName) {
            this.projectName = projectName
            return this
        }

        Integration retrieve() {
            if (this.projectName == null) {
                this.integration = alauda.script.alaudaRetrieveIntegration instanceUUID: this.name, projectName: alauda.project()
            } else {
                this.integration = alauda.script.alaudaRetrieveIntegration instanceUUID: this.name, projectName: this.projectName
            }
            return this
        }

        String getToken() {
            return this.integration.getFields().getToken()
        }

        String getEndpoint() {
            return this.integration.getFields().getEndpoint()
        }

        String getProject() {
            return this.projectName == null ? alauda.project() : this.projectName
        }

        @Override
        public String toString() {
            String projectname = this.projectName == null ? alauda.project() : this.projectName
            return "Integration{" +
                    "name='" + name + '\'' +
                    ", projectName=" + projectname +
                    '}'
        }
    }

    @NonCPS
    def ServiceDetails retrieveServiceDetails(String serviceID) {
        ServiceDetails details = script.alaudaRetrieveService serviceID: serviceID
        script.println(details)
        return details;
    }

//    @NonCPS
    def ServiceDetails retrieveServiceDetails(String serviceName, String clusterName, String namespace, String projectName) {
        ServiceDetails details = script.alaudaRetrieveService serviceName: serviceName, clusterName: clusterName, namespace: namespace, projectName: projectName
        script.println(details)
        return details;
    }

    @NonCPS
    def retrieveService(String serviceID) {
        script.println("alauda.retrieveService serviceID: '${serviceID}')")

        ServiceDetails details = retrieveServiceDetails(serviceID)
        def service = new Service(alauda: this)
        service.exec(["print 1", "print 2"])
    }

    @NonCPS
    def retrieveService(String serviceName, String clusterName, String namespace) {
        script.println("alauda.retrieveService serviceName: '${serviceName}', clusterName: '${clusterName}', namespace: '${namespace}'")
        new Service(alauda: this, name: serviceName);
//        ServiceDetails details = retrieveServiceDetails(serviceName, clusterName, namespace)
//        return new Service(this, details)
    }

    def service(String serviceName) {
        script.printf("alauda.service('%s')", serviceName)
        new Service(alauda: this, name: serviceName)
    }

    def static class Service implements Serializable, ContainerParent {

        private AlaudaDSL alauda;
        private String name;
        private boolean createIfNotExists;
        private int timeout;

        //using yaml to deploy service
        private String yamlFile;
        // when using yaml to update service, user should set image and image tag
        private String imageWillUpdate;
        private String imageTagUpdateTo;

        private boolean rollback;

        @Override
        void setRollback(boolean rollback) {
            this.rollback = rollback;
        }

        private Map<String, Container> containers = new HashMap<>();
        private String currentContainerName = "0"; // currentContainerName=0 when method withContainer has not argument

        Container getCurrentContainer() {
            if (!containers.containsKey(currentContainerName)) {
                containers.put(currentContainerName, new Container(parent: this, name: currentContainerName));
            }
            return containers.get(currentContainerName);
        }

        @Override
        void println(String info) {
            alauda.script.println(info);
        }

        void printf(String fm, Object[] objs) {
            alauda.script.printf(fm, objs);
        }

        void delete() {
            println("delete the service ${name}");
        }

        def retrieve() {
            return this;
        }

        Service withYaml(String yaml="app.yaml"){
            this.yamlFile = yaml
            return this
        }

        Service autoReplaceImageTag(String image, String tag){
            this.imageWillUpdate = image
            this.imageTagUpdateTo = tag
            return this
        }

        Service autoRollbackOnFail(){
            this.rollback = true
            return this
        }

        @Override
        void withTimeout(int timeout) {
            println("withTimeout('$timeout')");
            this.timeout = timeout;
        }

        // select the first container defined in service
        @Override
        Container withContainer() {
            return withContainer("0");
        }

        @Override
        Container withContainer(String name) {
            println("withContainer('$name')");
            this.currentContainerName = name;
            return this.getCurrentContainer();
        }

        Container withImage(String imageName) {
            return this.getCurrentContainer().withImage(imageName);
        }

        Container withImageTag(String tagName) {
            return this.getCurrentContainer().withImageTag(tagName);
        }

        Container withEnv(String key, String value) {
            return this.getCurrentContainer().withEnv(key, value);
        }

        Container withEnvVarFrom(String name, String key, String from) {
            return this.getCurrentContainer().withEnvVarFrom(name, key, from);
        }

        Container withEnvFrom(String name) {
            return this.getCurrentContainer().withEnvFrom(name);
        }

        Container withCommand(String command) {
            return this.getCurrentContainer().withCommand(command);
        }

        Container withArgs(String args) {
            return this.getCurrentContainer().withArgs(args);
        }

        Service exec(List<String> commands) {
            println("exec('$commands')");
            commands.each {
                println(it);
            }
            return this;
        }

        def updateContainer(Kubernete.Container originContainer, Container newContainer) {
            if (!Strings.isNullOrEmpty(newContainer.image)) {
                originContainer.setImage(newContainer.image)
            }
            if (!Strings.isNullOrEmpty(newContainer.imageTag)) {
                originContainer.setImageTag(newContainer.imageTag)
            }
            if (newContainer.envVars != null) {
                originContainer.addEnvVars(newContainer.envVars)
            }
            if (newContainer.envFroms != null) {
                originContainer.addEnvFroms(newContainer.envFroms)
            }
            if (!Strings.isNullOrEmpty(newContainer.command)) {
                originContainer.setCommand([newContainer.command])
            }
            if (!Strings.isNullOrEmpty(newContainer.args)) {
                originContainer.setArgs([newContainer.args])
            }
        }

        @NonCPS
        def autoReplaceImageTagInYamlMap(yamlMap){
            for(def res : yamlMap){
                if(res.kind.equals('Deployment')){
                    def containers = res.spec.template.spec.containers
                    for(def container : containers){
                        if(container.image.startsWith(this.imageWillUpdate+":")){
                            container.image = this.imageWillUpdate + ":" + this.imageTagUpdateTo
                        }
                    }
                }
            }
            return yamlMap
        }

        @NonCPS
        def ServiceCreatePayload convertCreatePaylod(payload){
            String jsonStr = new JsonOutput().toJson(payload)
            def objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false)
            return objectMapper.readValue(jsonStr, ServiceCreatePayload.class)
        }

        @NonCPS
        def ServiceUpdatePayload convertUpdatePaylod(payload){
            String jsonStr = new JsonOutput().toJson(payload)
            def objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false)
            return objectMapper.readValue(jsonStr, ServiceUpdatePayload.class)
        }

        @Override
        String deploy(Object... args) {
            println("deploy()")

            def argsDefine = ["async"]
            Map map = alauda.parseArgs(argsDefine, args)
            boolean async = map.get("async", false);
            ServiceDetails service = alauda.retrieveServiceDetails(this.name, alauda.cluster(), alauda.namespace(), alauda.project());

            // no yaml , must update service
            if(this.yamlFile == null || this.yamlFile==""){
                if(service == null){
                    throw new Exception(String.format("service %s is not exists, cannot update it", this.name) )
                }

                List<Kubernete.Container> originContainers = service.retrieveContainers();

                def firstContainer = originContainers.get(0);
                Container zeroContainer;
                if (containers.containsKey("0")) {
                    zeroContainer = containers.get("0")
                    updateContainer(firstContainer, zeroContainer)
                    containers.remove(zeroContainer)
                }
                if (containers.containsKey(firstContainer.name)) {
                    zeroContainer = containers.get(firstContainer.name)
                    updateContainer(firstContainer, zeroContainer)
                    containers.remove(firstContainer.name)
                }

                containers.each {
                    for (int i = 0; i < originContainers.size(); i++) {
                        if (it.getKey() == originContainers.get(i).name) {
                            updateContainer(originContainers.get(i), it.getValue())
                        }
                    }
                }

                service.updateContainers(originContainers)
                ServiceUpdatePayload payload = service.convertToServiceUpdatePayload()
                println("${payload}")
                alauda.script.alaudaDeployService serviceID: service.getResource().getUuid(), payload: payload, async: async, rollback:rollback

                return service.getResource().getUuid();
            }

            // use yaml , create service or update service

            def yamlMap = alauda.script.readYaml(file:this.yamlFile)
            def kubes = this.autoReplaceImageTagInYamlMap(yamlMap)
            def payload = [
                    "resource":["name": name],
                    "cluster":["name": alauda.cluster()],
                    "namespace":["name": alauda.namespace()],
                    "kubernetes": kubes
            ]
            println("${payload}")

            if(service == null){
                // will create it
                def serviceID = alauda.script.alaudaDeployService serviceName: this.name, payload: convertCreatePaylod(payload), async: async, project: alauda.project()
                return serviceID
            }else{
                // will update it
                alauda.script.alaudaDeployService serviceID: service.getResource().getUuid(), payload: convertUpdatePaylod(payload), async: async, rollback:rollback
                return service.getResource().getUuid()
            }
        }



        @Override
        public String toString() {
            return "Service{" +
                    "name='" + name + '\'' +
                    ", timeout=" + timeout +
                    ", currentContainerName='" + currentContainerName + '\'' +
                    '}';
        }
    }

    @NonCPS
    def retrieveComponentDetails(String applicationName, String resourceType, String componentName, String clusterName, String namespace) {
        ComponentDetails details = script.alaudaRetrieveComponent applicationName: applicationName, resourceType: resourceType,
                componentName: componentName, clusterName: clusterName, namespace: namespace
        script.println(details)
        return details;
    }

    def component(String applicationName, String resourceType, String componentName) {
        script.printf("alauda.component('%s, %s, %s')", applicationName, resourceType, componentName)
        new Component(alauda: this, applicationName: applicationName, resourceType: resourceType, name: componentName)
    }

    def static class Component implements Serializable, ContainerParent {

        private AlaudaDSL alauda;
        private String applicationName;
        private String resourceType;
        private String name;
        private boolean createIfNotExists;
        private int timeout;

        //using yaml to deploy Component
        private String yamlFile;
        // when using yaml to update Component, user should set image and image tag
        private String imageWillUpdate;
        private String imageTagUpdateTo;

        private boolean rollback;

        @Override
        void setRollback(boolean rollback) {
            this.rollback = rollback;
        }

        private Map<String, Container> containers = new HashMap<>();
        private String currentContainerName = "0"; // currentContainerName=0 when method withContainer has not argument

        Container getCurrentContainer() {
            if (!containers.containsKey(currentContainerName)) {
                containers.put(currentContainerName, new Container(parent: this, name: currentContainerName));
            }
            return containers.get(currentContainerName);
        }

        void println(String info) {
            alauda.script.println(info);
        }

        void printf(String fm, Object[] objs) {
            alauda.script.printf(fm, objs);
        }

        void delete() {
            println("delete the Component ${name}");
        }

        def retrieve() {
            return this;
        }

        Component withYaml(String yaml="app.yaml"){
            this.yamlFile = yaml
            return this
        }

        Component autoReplaceImageTag(String image, String tag){
            this.imageWillUpdate = image
            this.imageTagUpdateTo = tag
            return this
        }

        Component autoRollbackOnFail(){
            this.rollback = true
            return this
        }

        @Override
        void withTimeout(int timeout) {
            println("withTimeout('$timeout')");
            this.timeout = timeout;
        }

// select the first container defined in Component
        @Override
        Container withContainer() {
            return withContainer("0");
        }

        @Override
        Container withContainer(String name) {
            println("withContainer('$name')");
            this.currentContainerName = name;
            return this.getCurrentContainer();
        }

        Container withImage(String imageName) {
            return this.getCurrentContainer().withImage(imageName);
        }

        Container withImageTag(String tagName) {
            return this.getCurrentContainer().withImageTag(tagName);
        }

        Container withEnv(String key, String value) {
            return this.getCurrentContainer().withEnv(key, value);
        }

        Container withEnvVarFrom(String name, String key, String from) {
            return this.getCurrentContainer().withEnvVarFrom(name, key, from);
        }

        Container withEnvFrom(String name) {
            return this.getCurrentContainer().withEnvFrom(name);
        }

        Container withCommand(String command) {
            return this.getCurrentContainer().withCommand(command);
        }

        Container withArgs(String args) {
            return this.getCurrentContainer().withArgs(args);
        }

        Component exec(List<String> commands) {
            println("exec('$commands')");
            commands.each {
                println(it);
            }
            return this;
        }

        def updateContainer(Kubernete.Container originContainer, Container newContainer) {
            if (!Strings.isNullOrEmpty(newContainer.image)) {
                originContainer.setImage(newContainer.image)
            }
            if (!Strings.isNullOrEmpty(newContainer.imageTag)) {
                originContainer.setImageTag(newContainer.imageTag)
            }
            if (newContainer.envVars != null) {
                originContainer.addEnvVars(newContainer.envVars)
            }
            if (newContainer.envFroms != null) {
                originContainer.addEnvFroms(newContainer.envFroms)
            }
            if (!Strings.isNullOrEmpty(newContainer.command)) {
                originContainer.setCommand([newContainer.command])
            }
            if (!Strings.isNullOrEmpty(newContainer.args)) {
                originContainer.setArgs([newContainer.args])
            }
        }

        @NonCPS
        def autoReplaceImageTagInYamlMap(yamlMap, componentName){
            for(def res : yamlMap){
                if(res.metadata.name.equals(componentName)){
                    def containers = res.spec.template.spec.containers
                    for(def container : containers){
                        if(container.image.startsWith(this.imageWillUpdate+":")){
                            container.image = this.imageWillUpdate + ":" + this.imageTagUpdateTo
                        }
                    }
                }
            }
            return yamlMap
        }

        @NonCPS
        def Kubernete convertKubernetePaylod(payload){
            String jsonStr = new JsonOutput().toJson(payload)
            def objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false)
            return objectMapper.readValue(jsonStr, Kubernete.class)
        }

        @Override
        String deploy(Object... args) {
            println("deploy()")

            def argsDefine = ["async"]
            Map map = alauda.parseArgs(argsDefine, args)
            boolean async = map.get("async", false);
            ComponentDetails component = alauda.retrieveComponentDetails(this.applicationName, this.resourceType, this.name, alauda.cluster(), alauda.namespace());

            // no yaml , must update service
            if(this.yamlFile == null || this.yamlFile==""){
                if(component == null){
                    throw new Exception(String.format("service %s is not exists, cannot update it", this.name) )
                }

                List<Kubernete.Container> originContainers = component.retrieveContainers();

                def firstContainer = originContainers.get(0);
                Container zeroContainer;
                if (containers.containsKey("0")) {
                    zeroContainer = containers.get("0")
                    updateContainer(firstContainer, zeroContainer)
                    containers.remove(zeroContainer)
                }
                if (containers.containsKey(firstContainer.name)) {
                    zeroContainer = containers.get(firstContainer.name)
                    updateContainer(firstContainer, zeroContainer)
                    containers.remove(firstContainer.name)
                }

                containers.each {
                    for (int i = 0; i < originContainers.size(); i++) {
                        if (it.getKey() == originContainers.get(i).name) {
                            updateContainer(originContainers.get(i), it.getValue())
                        }
                    }
                }

                component.updateContainers(originContainers)
                component.updateAnnotation()
                Kubernete kube = component.getKubernetes()
                println("${kube}")
                alauda.script.alaudaDeployComponent clusterName: alauda.cluster(), resourceType: resourceType, componentName: name,
                        namespace: alauda.namespace(), payload: kube, async: async, rollback:rollback

                return kube.getMetadata().getName();
            }

            // use yaml , create service or update service

            def yamlMap = alauda.script.readYaml(file:this.yamlFile)
            def kubes = this.autoReplaceImageTagInYamlMap(yamlMap, name)
            def payload = convertKubernetePaylod(kubes)
            payload.updateAnnotation()
            println("${payload}")

            alauda.script.alaudaDeployComponent clusterName: alauda.cluster(), resourceType: resourceType, componentName: name,
                    namespace: alauda.namespace(), payload:payload, async: async, rollback:rollback

            return payload.getMetadata().getName();
        }



        @Override
        public String toString() {
            return "Service{" +
                    "name='" + name + '\'' +
                    ", timeout=" + timeout +
                    ", currentContainerName='" + currentContainerName + '\'' +
                    '}';
        }

    }

    def interface ContainerParent {
        void println(String info)
        String deploy(Object... args)
        void setRollback(boolean rollback)
        Container withContainer()
        Container withContainer(String name)
        void withTimeout(int timeout)
    }

    def static class Container implements Serializable {

        private ContainerParent parent;
        private String name;
        private String image;
        private String imageTag;
        private List<Kubernete.Container.EnvVar> envVars = new ArrayList<>()
        private List<Kubernete.Container.EnvFrom> envFroms = new ArrayList<>()
        private String command;
        private String args;

        void println(String info) {
            parent.println(info);
        }

        String deploy(Object... args) {
            return parent.deploy(args)
        }

        Container autoRollbackOnFail(){
            parent.setRollback(true)
            return this
        }

        Container withContainer() {
            return this.parent.withContainer()
        }

        Container withContainer(String name) {
            return this.parent.withContainer(name)
        }

        Container withAppcation(String appcationName) {
            return this.parent.withAppcation(appcationName)
        }

        Container withImage(String imageName) {
            println("withImage('$imageName')")

            this.image = imageName;
            return this;
        }

        Container withImageTag(String imageTag) {
            println("withImageTag('$imageTag')");

            this.imageTag = imageTag;
            return this;
        }

        Container withEnv(String key, String value) {
            println("withEnv('$key', '$value')");

            this.envVars.add(new Kubernete.Container.EnvVar(key, value));
            return this;
        }

        Container withEnvVarFrom(String name, String key, String from) {
            println("withEnvVarFrom('$name', '$key', '$from')")

            this.envVars.add(new Kubernete.Container.EnvVar(name, key, from));
            return this
        }

        Container withEnvFrom(String name) {
            println("withEnvFrom('$name')")

            this.envFroms.add(new Kubernete.Container.EnvFrom(name))
            return this
        }

        Container withCommand(String command) {
            println("withCommand('$command')");

            this.command = command;
            return this;
        }

        Container withArgs(String args) {
            println("withArgs('$args')");

            this.args = args;
            return this;
        }

        Container withTimeout(int timeout) {
            this.parent.withTimeout(timeout);
            return this;
        }

        @Override
        public String toString() {
            return "Container{" +
                    ", name='" + name + '\'' +
                    ", image='" + image + '\'' +
                    ", imageTag='" + imageTag + '\'' +
                    ", envVars=" + envVars +
                    ", envFroms=" + envFroms +
                    ", command='" + command + '\'' +
                    ", args='" + args + '\'' +
                    '}';
        }
    }

    def static class Build implements  Serializable{
        private AlaudaDSL alauda;
        private String imageFullName
        private String ciCredentialsId
        private String ciImage
        private Closure ciBody
        private String contextPath
        private String dockerfileLocation
        private boolean useImageCache
        private boolean ciEnabled
        private boolean isNewVersion



        private HostPathVolume hostPathVolume
        private PVC pvc

        Build setDockerfileLocation(String dockerfileLocation){
            this.dockerfileLocation = safePath(dockerfileLocation)
            return this
        }

        String safePath(String path){
            if(path.startsWith("/")){
                return "." + path
            }
            return path
        }

        Build setContextPath(String contextPath){
            this.contextPath = safePath(contextPath)
            return this
        }

        Build setUseImageCache(boolean useImageCache){
            this.useImageCache = useImageCache
            return this
        }

        Build setImage(String imageFullName){
            this.imageFullName = imageFullName
            return this
        }


        Build withCIImage(String ciImage, String credentialsId, Closure body){
            this.ciCredentialsId = credentialsId
            this.ciImage = ciImage
            this.ciBody = body
            return this
        }

        Build mountHostPathVolume(String hostPath, String mountPath){
            if (this.ciImage == null || this.ciImage == "") {
                throw  new Exception("CI is not enabled, cannot set host path volume")
            }
            this.hostPathVolume = new HostPathVolume(hostPath, mountPath)
            return this
        }

        Build mountPersistentVolumeClaim(String claimName, String mountPath){
            if (this.ciImage == null || this.ciImage == "") {
                throw  new Exception("CI is not enabled, cannot set persistent volume claim")
            }
            this.pvc = new PVC(mountPath, claimName)
            return this
        }

        static class HostPathVolume implements Serializable {
            private String mountPath;
            private String hostPath;

            HostPathVolume(String hostPath, String mountPath){
                this.mountPath = mountPath;
                this.hostPath = hostPath
            }
        }

        static class  PVC implements  Serializable {
            private String mountPath;
            private String claimName;

            PVC(String mountPath, String claimName){
                this.mountPath = mountPath;
                this.claimName = claimName
            }
        }

        Build withYaml(String ymlFile){
            ymlFile = safePath(ymlFile)

            String content = alauda.script.readFile(ymlFile)
            def result = parseAlaudaCIYamlContent(content)

            Map<String, Object> pre_ci_boot = (Map<String, Object>) result.get("pre_ci_boot")
            def ciImageName = pre_ci_boot.get("image").toString()
            def ciImageTag = pre_ci_boot.get("tag").toString()
            def ciSteps = (List) result.get("ci")

            this.ciImage = "${ciImageName}:${ciImageTag}"

            def ciCommands = ciSteps.join("\n")

            this.ciBody = {
                    alauda.script.sh ciCommands
            }

            return this
        }

        @NonCPS
        Map<String, Object> parseAlaudaCIYamlContent(String content){
            Yaml yaml = new Yaml()
            def result = (Map<String, Object>) yaml.load(content)
            return result
        }

        Image image(String imageName){
            def img = new AlaudaDSL.Image(this.alauda, imageName)
            return img

        }

        // start build image and return imageObject
        Image startBuildImage(){
            def dockerfile = this.dockerfileLocation;
            if (!this.dockerfileLocation.endsWith("/"))
                dockerfile += "/";
            dockerfile += "Dockerfile";
            def context = this.contextPath
            def useImageCache = this.useImageCache

            if (this.ciEnabled){
                alauda.script.dir("__dest__"){
                    // unstash dest if with ci step
                    if (!this.isNewVersion){
                        alauda.script.unstash "alaudaciDest"
                    }
                    alauda.script.sh "docker build --no-cache=${useImageCache} -t ${imageFullName} -f ${dockerfile} ${context}"
                    alauda.script.printf("prepare to push image")
                    return this.image(imageFullName)
                }
            }else{
                alauda.script.sh "docker build --no-cache=${useImageCache} -t ${imageFullName} -f ${dockerfile} ${context}"
                alauda.script.printf("prepare to push image")
                return this.image(imageFullName)
            }
        }

        ArrayList getVolumes(){
            def volumes = [alauda.script.hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')]
            if (this.hostPathVolume != null) {
                volumes += alauda.script.hostPathVolume(hostPath: this.hostPathVolume.hostPath, mountPath: this.hostPathVolume.mountPath)
            }
            if (this.pvc != null){
                volumes += alauda.script.persistentVolumeClaim(claimName: this.pvc.claimName, mountPath: this.pvc.mountPath)
            }
            return volumes
        }


        // exe command
        Build startBuild(){
            this.ciEnabled = true
            String registry = AlaudaDSL.parseRegistry(ciImage)

            if(this.ciCredentialsId != null && this.ciCredentialsId != "") {
                alauda.script.withEnv(["registry=${registry}"]) {
                    alauda.script.withCredentials([alauda.script.usernamePassword(credentialsId: this.ciCredentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        alauda.script.sh 'docker login ${registry} -u $USERNAME -p $PASSWORD'
                        alauda.script.sh "docker pull ${ciImage}"
                    }
                }
            }

            alauda.script.stash "alaudaciSource"
            def label = "ci-node-${UUID.randomUUID().toString()}"

            alauda.script.podTemplate(
                    label: label,
                    containers:[
                            alauda.script.containerTemplate(name: 'ci-container', image: "${ciImage}", ttyEnabled: true,
                                envVars: [alauda.script.envVar(key: "LANG", value: "C.UTF-8")])
                    ],
                    volumes: this.getVolumes(),
                    nodeUsageMode: "EXCLUSIVE"
            ){
                alauda.script.node(label) {
                    alauda.script.container('ci-container') {

                        def dest = "${alauda.script.env.WORKSPACE}/__dest__"
                        def source = "${alauda.script.env.WORKSPACE}/__source__"
                        def bin = "${alauda.script.env.WORKSPACE}/__bin__"
                        def upload = "${alauda.script.env.WORKSPACE}/__upload__"
                        alauda.script.sh "mkdir -p ${dest}"

                        alauda.script.dir("./__source__"){
                            alauda.script.unstash "alaudaciSource"

                            alauda.script.withEnv([
                                    "ALAUDACI_DEST_DIR=${dest}","ALAUDACI_SOURCE_DIR=${source}", "ALAUDACI_BIN_DIR=${bin}", "ALAUDACI_UPLOAD_DIR=${upload}"
                            ]){
                                alauda.script.dir("${contextPath}"){
                                    this.ciBody()
                                }
                            }

                            def count = alauda.script.sh(script: "ls -A ${dest} |wc -w", returnStdout:true).trim()
                            if(count == "0"){
                                alauda.script.echo "dest directory is empty, will copy source directory to dest"
                                alauda.script.sh "cp -r ./ ${dest}"
                            }
                        }

                        alauda.script.dir("./__dest__"){
                            alauda.script.stash "alaudaciDest"
                        }
                    }
                }
            }
            return this
        }
    }

    Build build(Object... args){
        def argsDefine = ["contextPath", "dockerfileLocation", "useImageCache"]
        Map map = parseArgs(argsDefine, args)
        def build = new AlaudaDSL.Build(alauda: this)

        build.setContextPath(map.get("contextPath", './'))
        build.setDockerfileLocation(map.get("dockerfileLocation", './'))
        String imageCache = map.get("useImageCache", "true")
        build.useImageCache = imageCache.toLowerCase().equals("true")
        build.ciEnabled = false
        build.isNewVersion = false
        return build
    }

     static class Image implements Serializable{

        private AlaudaDSL alauda

        private String originImageFullName

        private String imageRegistry
        private String imageRepository
        private String tag

        private String credentialsId

        private Image(AlaudaDSL alauda, String imageFullName){
            this.alauda = alauda
            initByImageFullName(imageFullName)
        }

        @NonCPS
        void initByImageFullName(String imageFullName){
            this.originImageFullName = imageFullName
            String[] segments = imageFullName.split("/")
            this.imageRegistry = segments[0]
            if(segments.length < 2 ){
                throw new AbortException("${imageFullName} is invalid, format should be imageRegistry/imageRepository:imageTag")
            }

            def lastSegment = segments[segments.length-1]
            if(!lastSegment.contains(":")){
                this.tag = "latest"
                this.imageRepository = imageFullName.substring(this.imageRegistry.length()+1)
            }else{
                this.tag = lastSegment.substring(lastSegment.indexOf(":")+1)
                this.imageRepository = imageFullName.substring(this.imageRegistry.length()+1, imageFullName.lastIndexOf(":"))
            }
            this.alauda.script.print("registry is ${imageRegistry} repository is ${imageRepository} tag is ${tag}")
            return
        }

        Image setTag(String tag){
            alauda.script.printf("set image tag is: %s", tag)
            this.tag = tag
            return this
        }

        Image withRegistry(Object... args){
            def argsDefine = ["url", "credentialsId"]
            Map map = this.alauda.parseArgs(argsDefine, args)

            if (!map.get("url", "").equals("")){
                this.imageRegistry = AlaudaDSL.parseRegistry(map.get("url", "").toString())
            }
            this.credentialsId = map.get("credentialsId", null)
            return this
        }

        String imageFullName(String overrideTag){
            if (overrideTag != null && overrideTag.equals("")) {
                return imageRegistry + "/" + imageRepository + ":" + overrideTag
            }
            return imageRegistry + "/" + imageRepository + ":" + tag
        }

        void push(String tag = null){
            if(tag != null && tag != ""){
                this.tag = tag
            }
            def selfTaggedImage = imageFullName(null)

            if(this.credentialsId != null && this.credentialsId != ""){
                alauda.script.withEnv(["registry=${imageRegistry}"]){
                    alauda.script.withCredentials([alauda.script.usernamePassword(credentialsId: this.credentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]){
                        alauda.script.sh 'docker login ${registry} -u ${USERNAME} -p ${PASSWORD}'
                    }
                }
            }

            alauda.script.sh "docker push ${originImageFullName}"

            if(!selfTaggedImage.equals(originImageFullName)){
                alauda.script.sh "docker tag ${originImageFullName} ${selfTaggedImage}"
                alauda.script.sh "docker push ${selfTaggedImage}"
            }
        }
    }

    @NonCPS
    def static parseRegistry(String url){
        String registry

        String[] urlList = url.split("//")
        if(urlList.length == 2){
            registry = urlList[1]
        }else {
            registry = url
        }

        String[] registryList = registry.split("/")
        return registryList[0]
    }


    @NonCPS
    def static String safePath(String path){
        if(path.startsWith("/")){
            return "." + path
        }
        return path
    }
}
