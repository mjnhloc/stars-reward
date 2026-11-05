package com.sr.scoreimport.dto.response;

/*
 * severity: ERROR (data is skipped) or WARNING (only for information)
 */
public record ImportIssueResponse(
    String severity,
    String location,
    String message
) {

    public static ImportIssueResponse error(String location, String message) {
        return new ImportIssueResponse("ERROR", location, message);
    }

    public static ImportIssueResponse warning(String location, String message) {
        return new ImportIssueResponse("WARNING", location, message);
    }
}
