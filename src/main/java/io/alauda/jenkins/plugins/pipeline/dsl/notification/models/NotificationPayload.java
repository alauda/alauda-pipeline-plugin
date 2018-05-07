package io.alauda.jenkins.plugins.pipeline.dsl.notification.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.alauda.jenkins.plugins.pipeline.utils.DateSerializer;

import java.util.Date;

public class NotificationPayload {
    @JsonProperty("template_type")
    public String TemplateType="jenkins";
    @JsonProperty("data")
    public data data;


    public NotificationPayload(){
        this.data = new data();
        this.data.time = new Date();
    }

    public NotificationPayload setSubject(String subject){
        this.data.subject = subject;
        return this;
    }

    public NotificationPayload setContent(String content){
        this.data.content = content;
        return this;
    }

    public NotificationPayload setCauseText(String cause){
        this.data.payload.causeText = cause;
        return this;
    }

    public NotificationPayload setRepo(String repo){
        this.data.payload.repo = repo;
        return this;
    }

    public NotificationPayload setRepoBranch(String branch){
        this.data.payload.repoBranch = branch;
        return this;
    }

    public NotificationPayload setRepoVersion(String version){
        this.data.payload.repoVersion = version;
        return this;
    }

    public NotificationPayload setJobURL(String jobURL){
        this.data.payload.jobURL = jobURL;
        return this;
    }

    public  NotificationPayload setStartedAt(Date startedAt){
        this.data.payload.startedAt = startedAt;
        return this;
    }

    public NotificationPayload setDuration(String duration){
        this.data.payload.duration = duration;
        return this;
    }

    public NotificationPayload setEndedAt(Date endedAt){
        this.data.payload.endAt = endedAt;
        return  this;
    }

    public NotificationPayload setStatus(String status){
        this.data.payload.status = status;
        return this;
    }

    public NotificationPayload setBody(String body){
        this.data.payload.body = body;
        return this;
    }

    public static class data {
        public String subject;
        @JsonProperty("content")
        public String content="";

        @JsonSerialize(using = DateSerializer.class)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Date time;

        @JsonProperty("payload")
        public payload payload;
        public data(){
            this.payload = new payload();
        }
    }

    public static class payload{
        @JsonProperty("cause_text")
        public String causeText;
        public String repo;
        @JsonProperty("repo_branch")
        public String repoBranch;
        @JsonProperty("repo_version")
        public String repoVersion;
        @JsonProperty("job_url")
        public String jobURL;

        @JsonProperty("started_at")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonSerialize(using = DateSerializer.class)
        public Date startedAt;

        @JsonProperty("duration")
        public String duration;

        @JsonProperty("ended_at")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonSerialize(using = DateSerializer.class)
        public Date endAt;

        public String status;
        public String body;
    }
}
