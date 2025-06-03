package com.redcat.tutorials.summariser.dto.gemini;

import java.util.List;

public class GeminiResponse {
    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;
    private UsageMetadata usageMetadata;
    private String modelVersion;
    private String responseId;

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public PromptFeedback getPromptFeedback() {
        return promptFeedback;
    }

    public void setPromptFeedback(PromptFeedback promptFeedback) {
        this.promptFeedback = promptFeedback;
    }

    public UsageMetadata getUsageMetadata() {
        return usageMetadata;
    }

    public void setUsageMetadata(UsageMetadata usageMetadata) {
        this.usageMetadata = usageMetadata;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public static class Candidate {
        private Content content;
        private String finishReason;
        private int index;
        private SafetyRating[] safetyRatings;

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public SafetyRating[] getSafetyRatings() {
            return safetyRatings;
        }

        public void setSafetyRatings(SafetyRating[] safetyRatings) {
            this.safetyRatings = safetyRatings;
        }
    }

    public static class Content {
        private List<Part> parts;
        private String role;

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class Part {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class SafetyRating {
        private String category;
        private String probability;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getProbability() {
            return probability;
        }

        public void setProbability(String probability) {
            this.probability = probability;
        }
    }

    public static class PromptFeedback {
        private BlockedReason blockedReason;
        private SafetyRating[] safetyRatings;

        public BlockedReason getBlockedReason() {
            return blockedReason;
        }

        public void setBlockedReason(BlockedReason blockedReason) {
            this.blockedReason = blockedReason;
        }

        public SafetyRating[] getSafetyRatings() {
            return safetyRatings;
        }

        public void setSafetyRatings(SafetyRating[] safetyRatings) {
            this.safetyRatings = safetyRatings;
        }
    }

    public static class BlockedReason {
        // Add fields as needed
    }

    public static class UsageMetadata {
        private Integer promptTokenCount;
        private Integer candidatesTokenCount;
        private Integer totalTokenCount;
        private List<PromptTokensDetail> promptTokensDetails;
        private Integer thoughtsTokenCount;

        public Integer getPromptTokenCount() {
            return promptTokenCount;
        }

        public void setPromptTokenCount(Integer promptTokenCount) {
            this.promptTokenCount = promptTokenCount;
        }

        public Integer getCandidatesTokenCount() {
            return candidatesTokenCount;
        }

        public void setCandidatesTokenCount(Integer candidatesTokenCount) {
            this.candidatesTokenCount = candidatesTokenCount;
        }

        public Integer getTotalTokenCount() {
            return totalTokenCount;
        }

        public void setTotalTokenCount(Integer totalTokenCount) {
            this.totalTokenCount = totalTokenCount;
        }

        public List<PromptTokensDetail> getPromptTokensDetails() {
            return promptTokensDetails;
        }

        public void setPromptTokensDetails(List<PromptTokensDetail> promptTokensDetails) {
            this.promptTokensDetails = promptTokensDetails;
        }

        public Integer getThoughtsTokenCount() {
            return thoughtsTokenCount;
        }

        public void setThoughtsTokenCount(Integer thoughtsTokenCount) {
            this.thoughtsTokenCount = thoughtsTokenCount;
        }
    }

    public static class PromptTokensDetail {
        private String modality;
        private Integer tokenCount;

        public String getModality() {
            return modality;
        }

        public void setModality(String modality) {
            this.modality = modality;
        }

        public Integer getTokenCount() {
            return tokenCount;
        }

        public void setTokenCount(Integer tokenCount) {
            this.tokenCount = tokenCount;
        }
    }
}
