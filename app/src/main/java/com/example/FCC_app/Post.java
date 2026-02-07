package com.example.FCC_app;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;
import java.util.List;

public class Post {

    @DocumentId
    private String documentId;
    private String subject;
    private String message;
    private Date timestamp;
    private List<String> targetTeams;
    private List<String> readBy;

    // Firestore requires a public no-arg constructor
    public Post() {}

    public Post(String subject, String message, Date timestamp, List<String> targetTeams, List<String> readBy) {
        this.subject = subject;
        this.message = message;
        this.timestamp = timestamp;
        this.targetTeams = targetTeams;
        this.readBy = readBy;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public List<String> getTargetTeams() {
        return targetTeams;
    }

    public List<String> getReadBy() {
        return readBy;
    }
}
