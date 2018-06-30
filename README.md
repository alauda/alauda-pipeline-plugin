# Alauda Pipeline Plugin for Jenkins

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Overview](#overview)
- [Installing and developing](#installing-and-developing)
- [Alauda Configuration](#alauda-configuration)
- [Functions](#functions)
  - [alauda.withSpace](#alaudawithspace)
  - [alauda.startBuild](#alaudastartbuild)
  - [alauda.deleteBuild](#alaudadeletebuild)
  - [alauda.notify](#alaudanotify)
  - [alauda.verbose](#alaudaverbose)
- [Examples](#examples)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


## Overview
DSL Plugin is a Jenkins plugin which aims to provide a readable, concise, comprehensive, and fluent
[Jenkins Pipeline](https://jenkins.io/doc/book/pipeline/) syntax for rich interactions with an Alauda API Server. 

The DSL provided by this plugin can coexist with both the declarative and scripted syntaxes of Jenkins Pipeline.
Keep in mind that per the requirements of declarative pipelines, the

```groovy
pipeline {

...

}
```

## Installing and developing

Otherwise, if you are interested in building this plugin locally, follow these steps:

1. Install maven (platform specific)
2. Clone this git repository:

3. In the root of the local repository, run maven
    ```
    cd alauda-pipeline-plugin
    mvn package
    ```
4. Maven will build target/alauda-pipeline.hpi  (the Jenkins plugin binary)
5. Open Jenkins in your browser, and navigate (as an administrator):
  1. Manage Jenkins > Manage Plugins.
  2. Select the "Advanced" Tab.
  3. Find the "Upload Plugin" HTML form and click "Browse".
  4. Find the alauda-pipeline.hpi built in the previous steps.
  5. Submit the file.
  6. Check that Jenkins should be restarted.

You should now be able to [configure an Alauda](#configuring).


## Alauda Configuration
You should config Alauda information on `http://{you-jenkins-url}/configure` firstly

## Functions

### alauda.withSpace
- params:
    - spaceName: `required`, space name
    
```$xslt
script{
    alauda.withSpace("global"){
    
    }
}
```
### alauda.startBuild
- params:
    - buildConfigName: `required`, your build config name on Alauda
    - commitID: `optional`, commitID that you want build
    - branch: `optional`, branch that you want build
    - async: `optional`, default value is false, will block build job until completed.
    - ignoreBuildResult: `optional`, default value is false. if it set to `true`, jenkins job will success , ingore build result is failure or succeed.
- return: buildID

```
script{
    // case 1
    alauda.startBuild "your-build-config-name"
    
    // case 2
    alauda.startBuild buildConfigName:"", commitID:"", branch:"", async:false, ignoreBuildResult:false
    
    // case 3
    alauda.startBuild [buildConfigName:"", commitID:"", branch:"", async:false, ignoreBuildResult:false]
}

```

### alauda.deleteBuild
- params:
    - buildID: `required`
    
```$xslt
script{
    alauda.deleteBuild "xxxxxx"
}
```

### alauda.notify
- params:
    - name: `required`, notification name on alauda
    - body: `optional`, the message you want to send
    
```
script{
    alauda.notify "jenkins-job-notify"
}
```

### alauda.verbose
- params: isVerbose, default value is false. 

```
script{
    alauda.verbose true
    alauda.verbose false
}
```

### alauda.build


## alauda.service
Get a service object

### withYaml
```
script{
    alauda.withCluster("cluster_name", "k8s_namespace"){
        alauda..withProject("dev"){
            alauda.service("ServiceNotExists").
                withYaml("./service.yaml").
                deploy()
        }
    }
}
```
### autoReplaceImageTag
when use service yaml to deploy service, you can invoke autoReplaceImageTag to update image tag in yaml
, you cannot use it , when specify one container in service.
```
script{
    alauda.withCluster("cluster_name", "k8s_namespace"){
        alauda.withProject("dev"){
            alauda.service("ServiceNotExists").
                withYaml("./service.yaml").
                autoReplaceImageTag("index.alauda.cn/alauda/demo", "v1").
                deploy()
        }
    }
}
```
### autoRollbackOnFail
Auto rollback service when deploy failure

```
script{
    alauda.withCluster("cluster_name", "k8s_namespace"){
        alauda.withProject("dev"){
            alauda.service("ServiceNotExists").
                withContainer("container-0").
                withImage("index.alauda/alauda/demo").
                withTag("v1").
                autoRollbackOnFail()
        }
    }
}
```

### withEnvVarFrom
```
script{
    alauda.withCluster("cluster_name", "k8s_namespace"){
        alauda.withProject("dev"){
            alauda.service("ServiceNotExists").
                withContainer("container-0").
                withImage("index.alauda/alauda/demo").
                withTag("v1").
                withEnvVarFrom("envname", "envkey", "configmapname").
                autoRollbackOnFail()
        }
    }
}
```

### withEnvFrom
```
script{
    alauda.withCluster("cluster_name", "k8s_namespace"){
        alauda.withProject("dev"){
            alauda.service("ServiceNotExists").
                withContainer("container-0").
                withImage("index.alauda/alauda/demo").
                withTag("v1").
                withEnvFrom("configmapname").
                autoRollbackOnFail()
        }
    }
}
```


## Examples

```
pipeline {
    agent any
    
    stages{
        stage("Build"){
            steps{
                script{
                    timeout(10){
                     
                        alauda.withSpace("global"){
                            def buildID = alauda.startBuild buildConfigName:"alauda-ci"
                            echo "build id is ${buildID}"
                            echo "will delete this build"
                            alauda.deleteBuild(buildID)
                        }
                        
                    }
                }
            }
        }
    }
    post{
        always {
            script{
                alauda.withSpace("jenkins_robot"){
                    alauda.notify("jenkins-notify") 
                }
                
            }
        }
    }
}
```

### use build dsl
```
pipeline{
    agent any
    stages{
        stage("Clone"){
            steps{
               git "https://github.com/foo/bar.git" 
            }
        }
        stage("BuildWithYaml"){
            steps{
                script{
                    def image = alauda.build().
                        setDockerfileLocation("./").
                        setContextPath("./").
                        setUseImageCache(true).
                        setImage("xxxx/xxxx/xxxxx").
                        withYaml("./alaudaci.yml").
                        mountHostPathVolume("/data/.m2", "/root/.m2").
                        mountPersistentVolumeClaim("testdata", "/root/.m2"). //选其一
                        startBuild().
                        startBuildImage()

                    image.withRegistry([credentialsId: 'replace-with-your-credentialsid', url: 'xxx/xxx/xxx']).push("replace-with-your-tag")
                }
            }
            
        }
        stage("Build"){
            steps{
                script{
                    def image = alauda.build().
                        setDockerfileLocation("./").
                        setContextPath("./").
                        setUseImageCache(true).
                        withCIImage("xxxx/xxxx/xxxx:xx", "replace-with-your-registry-credentialsid"){
                            sh "replace-with-your-command"
                        }.
                        mountHostPathVolume("/data/.m2", "/root/.m2").
                        mountPersistentVolumeClaim("testdata", "/root/.m2"). //选其一
                        setImage("xxxxx/xxxx/xx:xx").
                        startBuild().
                        startBuildImage()
                        
                    image.withRegistry([credentialsId: 'replace-with-your-credentialsid', url: 'replace-with-your-registry-url']).push("replace-with-your-tag")

                }
            }
            
        }
    }
}
```
                    
