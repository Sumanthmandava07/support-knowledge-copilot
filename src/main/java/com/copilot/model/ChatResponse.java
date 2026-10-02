package com.copilot.model;

import java.util.List;

public class ChatResponse {

    private String answer;
    private List<Citation> citations;
    private boolean verified;
    private String sessionId;

    public ChatResponse() {
    }

    public ChatResponse(
            String answer,
            List<Citation> citations,
            boolean verified,
            String sessionId) {

        this.answer = answer;
        this.citations = citations;
        this.verified = verified;
        this.sessionId = sessionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<Citation> getCitations() {
        return citations;
    }

    public void setCitations(List<Citation> citations) {
        this.citations = citations;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public static class Citation {

        private String source;
        private String title;
        private String section;
        private String page;
        private String accessLevel;
        private String text;
        private double supportScore;
        private boolean verified;

        public Citation() {
        }

        public Citation(
                String source,
                String title,
                String section,
                String page,
                String accessLevel,
                String text,
                double supportScore,
                boolean verified) {

            this.source = source;
            this.title = title;
            this.section = section;
            this.page = page;
            this.accessLevel = accessLevel;
            this.text = text;
            this.supportScore = supportScore;
            this.verified = verified;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }

        public String getPage() {
            return page;
        }

        public void setPage(String page) {
            this.page = page;
        }

        public String getAccessLevel() {
            return accessLevel;
        }

        public void setAccessLevel(String accessLevel) {
            this.accessLevel = accessLevel;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public double getSupportScore() {
            return supportScore;
        }

        public void setSupportScore(double supportScore) {
            this.supportScore = supportScore;
        }

        public boolean isVerified() {
            return verified;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }
    }
}