# Alauda Pipeline Plugin for Jenkins

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


<!-- END doctoc generated TOC please keep comment here to allow auto update -->


## 预览
Jenkins Pipeline Plugin 插件提供了一套可读易用的语法， 你可以用该插件和你的Alauda 平台进行功能集成。

## 安装和开发
1. 安装 maven
2. Clone 该代码仓
3. 执行命令

```
    cd alauda-pipeline-plugin
    mvn package
```
4. Maven 将会build 生成 target/alauda-pipeline.hpi （jenkins 插件）
5. 在浏览器中打开你的Jenkins 服务，使用管理员账号登录
    1. 打开 系统管理>管理插件 页面
    2. 点击高级选项
    3. 点击 “上传插件”， 上次 alauda-pipeline.hpi 文件
    4. 上传完成后，等待Jenkins 重启

接下来可以对该插件进行配置

## 插件配置

1. 打开 系统管理>系统设置 页面
2. 找到Alauda 节点，各个参数意义如下
    - `API Console URL`: 你的Alauda 平台的地址 例如：`https://enterprise.alauda.cn`
    - `API Endpoint`: Alauda 平台的API地址,例如：`https://api.alauda.cn`
    - `API Token`: Alauda 平台的API Token,例如： `123efc65d34f3e927dfhr8076fd7b1ddr5dc923d` 你可以在Alauda平台的 `用户中心/查看Token`获取。
    - `Default Namespace`: Alauda平台根账号名称, 所有资源操作，都将会默认操作改根账号下的资源。
    - `Default Spacename`: 可选值。默认的资源空间，所有资源操作，都将会在该spacename下。

## 功能

### alauda.withSpace
提供一个命令执行的上下文，上下文中执行的命令都会默认在该space种执行。如果不指定，则会使用配置的 `Default Spacename`

- 参数:
    - spaceName: 必选，资源空间

示例
```
script{
    alauda.withSpace("global"){
        
    }
}
```

### alauda.startBuild
触发 Alauda平台的构建

- 参数:
    - buildConfigName: `必选`, Alauda平台构建功能中，构建项目的名称
    - commitID: `可选`, 想要构建的commitID, 如果为空，则使用最新版本。
    - branch: 想要构建的分支，如果你的Alauda构建项目的分支指定的是通配符，则必须传递该参数。
    - async: `可选`, 默认为 false, 将会一直等待，一直到 Alauda构建完成。 true表示触发构建则立即返回。
    - ignoreBuildResult: `可选`, 默认值为 false， 表示Jenkins job 的成功与否取决于Alauda的构建的结果。 如果设置为true，jenkins job 不会关心Alauda的构建结果，无论成功还是失败，Jenkins Job都会正常结束。
- 返回: 构建ID

```
script{
    // 示例1
    alauda.startBuild "your-build-config-name"
    
    // 示例 2
    alauda.startBuild buildConfigName:"", commitID:"", branch:"", async:false, ignoreBuildResult:false
    
    // 示例 3
    alauda.startBuild [buildConfigName:"", commitID:"", branch:"", async:false, ignoreBuildResult:false]
}

```

### alauda.deleteBuild
根据构建ID 删除构建 

- 参数:
    - buildID: `必选`
    
```
script{
    def buildID = alauda.startBuild "your-build-config-name"
    alauda.deleteBuild "${buildID}"
}
```

### alauda.notify
发送Alauda通知

- 参数:
    - name: `必选`, Alauda平台的通知配置名称
    - body: `可选`, 想要发送的自定义内容
    
```
script{
    alauda.notify "jenkins-job-notify"
}
```

### alauda.verbose
调试使用，输出更多信息

- 参数: 
    - isVerbose, 默认值为false

```
script{
    alauda.verbose true
    alauda.withSpace("global"){
        alauda.startBuild "your-build-config-name"
    }
    alauda.verbose false
}
```

## 样例

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
