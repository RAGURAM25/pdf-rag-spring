package com.example.pdfrag;

import java.util.List;

public record AnswerResponse(String answer, List<Source> sources) {

    public record Source(int number, String file, Object page, Double score, String snippet) {}
}
