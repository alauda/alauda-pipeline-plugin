package io.alauda.jenkins.plugins.pipeline.dsl.notification.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.alauda.jenkins.plugins.pipeline.utils.DateSerializer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NotificationPayloadByParams {

    @JsonProperty("template_type")
    private String templateType = "jenkins";
    @JsonProperty("data")
    public NotificationPayloadByParams.NotificationPayloadData data;

    public NotificationPayloadByParams(Map<String, Object> params) {
        this.data = new NotificationPayloadByParams.NotificationPayloadData();
        this.data.setTime(new Date());
        this.data.initialParams(params);
    }

    public void setParam(String key, Object value) {
        this.data.params.put(key, value);
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public NotificationPayloadByParams.NotificationPayloadData getData() {
        return data;
    }

    public void setData(NotificationPayloadByParams.NotificationPayloadData data) {
        this.data = data;
    }

    public NotificationPayloadByParams setSubject(String subject) {
        this.data.subject = subject;
        return this;
    }

    public NotificationPayloadByParams setContent(String content) {
        this.data.content = content;
        return this;
    }

    public static class NotificationPayloadData {

        private String subject;
        @JsonProperty("content")
        private String content = "";

        @JsonSerialize(using = DateSerializer.class)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Date time;

        @JsonProperty("payload")
        private Map<String, Object> params;

        @JsonIgnore
        private Map<String, String> detailKeys = new HashMap<String, String>(){{
            put("codescan.sonarprojectname.cn", "代码扫描项目名称");
            put("codescan.sonarurl.cn", "Sonar地址");
        }};


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

        public Map<String, Object> getParams() {
            return params;
        }

        public void setParams(Map<String, Object> params) {
            this.params = params;
        }

        public void initialParams(Map<String,Object> params) {
            this.params = new HashMap<String, Object>();
            StringBuffer details = new StringBuffer();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String title = entry.getKey();
                if (detailKeys.containsKey(entry.getKey())) {
                    title = detailKeys.get(title);
                }
                details.append(String.format("  %s: %s%n", title, entry.getValue().toString()));
            }
            this.params.put("details", details.toString());
        }
    }
}
