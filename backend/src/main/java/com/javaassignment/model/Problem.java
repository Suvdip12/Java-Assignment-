package com.javaassignment.model;

public class Problem {
    private int id;
    private String part;
    private String questionNumber;
    private String title;
    private String description;
    private String code;
    private String originalCode;
    private String inputType; // "NONE" or "STDIN"
    private String defaultInput;

    public Problem() {}

    public Problem(int id, String part, String questionNumber, String title,
                   String description, String code, String inputType, String defaultInput) {
        this.id = id;
        this.part = part;
        this.questionNumber = questionNumber;
        this.title = title;
        this.description = description;
        this.code = code;
        this.originalCode = code;
        this.inputType = inputType;
        this.defaultInput = defaultInput;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPart() { return part; }
    public void setPart(String part) { this.part = part; }

    public String getQuestionNumber() { return questionNumber; }
    public void setQuestionNumber(String questionNumber) { this.questionNumber = questionNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getOriginalCode() { return originalCode; }
    public void setOriginalCode(String originalCode) { this.originalCode = originalCode; }

    public String getInputType() { return inputType; }
    public void setInputType(String inputType) { this.inputType = inputType; }

    public String getDefaultInput() { return defaultInput; }
    public void setDefaultInput(String defaultInput) { this.defaultInput = defaultInput; }
}
