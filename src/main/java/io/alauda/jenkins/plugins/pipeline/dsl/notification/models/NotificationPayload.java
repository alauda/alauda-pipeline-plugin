package io.alauda.jenkins.plugins.pipeline.dsl.notification.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.alauda.jenkins.plugins.pipeline.utils.DateSerializer;

import java.util.Date;

public class NotificationPayload {
    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public NotificationPayloadData getData() {
        return data;
    }

    public void setData(NotificationPayloadData data) {
        this.data = data;
    }

    @JsonProperty("template_type")
    private String templateType = "jenkins";
    @JsonProperty("data")
    public NotificationPayloadData data;


    public NotificationPayload() {
        this.data = new NotificationPayloadData();
        this.data.time = new Date();
    }

    public NotificationPayload setSubject(String subject) {
        this.data.subject = subject;
        return this;
    }

    public NotificationPayload setContent(String content) {
        this.data.content = content;
        return this;
    }

    public NotificationPayload setCauseText(String cause) {
        this.data.payload.causeText = cause;
        return this;
    }

    public NotificationPayload setRepo(String repo) {
        this.data.payload.repo = repo;
        return this;
    }

    public NotificationPayload setRepoBranch(String branch) {
        this.data.payload.repoBranch = branch;
        return this;
    }

    public NotificationPayload setRepoVersion(String version) {
        this.data.payload.repoVersion = version;
        return this;
    }

    public NotificationPayload setJobURL(String jobURL) {
        this.data.payload.jobURL = jobURL;
        return this;
    }

    public NotificationPayload setStartedAt(Date startedAt) {
        this.data.payload.startedAt = startedAt;
        return this;
    }

    public NotificationPayload setDuration(String duration) {
        this.data.payload.duration = duration;
        return this;
    }

    public NotificationPayload setEndedAt(Date endedAt) {
        this.data.payload.endAt = endedAt;
        return this;
    }

    public NotificationPayload setStatus(String status) {
        this.data.payload.status = status;
        return this;
    }

    public NotificationPayload setBody(String body) {
        this.data.payload.body = body;
        return this;
    }

    public static class NotificationPayloadData {
        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Date getTime() {
            if (this.time != null) {
                return new Date(this.time.getTime());
            }
            return null;
        }

        public void setTime(Date time) {
            if (time != null){
                this.time = (Date)time.clone();
            }else{
                this.time = null;
            }

        }

        public Payload getPayload() {
            return payload;
        }

        public void setPayload(Payload payload) {
            this.payload = payload;
        }

        private String subject;
        @JsonProperty("content")
        private String content = "";

        @JsonSerialize(using = DateSerializer.class)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Date time;

        @JsonProperty("payload")
        private Payload payload;

        public NotificationPayloadData() {
            this.payload = new Payload();
        }
    }

    public static class Payload {
        public String getCauseText() {
            return causeText;
        }

        public void setCauseText(String causeText) {
            this.causeText = causeText;
        }

        public String getRepo() {
            return repo;
        }

        public void setRepo(String repo) {
            this.repo = repo;
        }

        public String getRepoBranch() {
            return repoBranch;
        }

        public void setRepoBranch(String repoBranch) {
            this.repoBranch = repoBranch;
        }

        public String getRepoVersion() {
            return repoVersion;
        }

        public void setRepoVersion(String repoVersion) {
            this.repoVersion = repoVersion;
        }

        public String getJobURL() {
            return jobURL;
        }

        public void setJobURL(String jobURL) {
            this.jobURL = jobURL;
        }

        public Date getStartedAt() {
            if(this.startedAt!=null){
                return new Date(this.startedAt.getTime());
            }
            return null;
        }

        public void setStartedAt(Date startedAt) {
            if(startedAt!=null){
                this.startedAt = (Date) startedAt.clone();
            }else{
                this.startedAt = null;
            }
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public Date getEndAt() {
            if (endAt != null) {
                return new Date(this.endAt.getTime());
            }
            return null;
        }

        public void setEndAt(Date endAt) {
            if (endAt != null){
                this.endAt = (Date) endAt.clone();
            }else{
                this.endAt = null;
            }
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        @JsonProperty("cause_text")
        private String causeText;
        public String repo;
        @JsonProperty("repo_branch")
        private String repoBranch;
        @JsonProperty("repo_version")
        private String repoVersion;
        @JsonProperty("job_url")
        private String jobURL;

        @JsonProperty("started_at")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonSerialize(using = DateSerializer.class)
        private Date startedAt;

        @JsonProperty("duration")
        private String duration;

        @JsonProperty("ended_at")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonSerialize(using = DateSerializer.class)
        private Date endAt;

        private String status;
        private String body;
    }
}
