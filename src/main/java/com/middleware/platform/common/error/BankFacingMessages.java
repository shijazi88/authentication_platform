package com.middleware.platform.common.error;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites internal error text into plain language for bank-facing surfaces
 * (the verification API and the client portal). Internal messages — which name
 * the upstream provider, HTTP statuses, circuit breakers and so on — stay on the
 * transaction record and in the logs for our own support staff.
 */
public final class BankFacingMessages {

    private static final Pattern PROVIDER_VALIDATION =
            Pattern.compile("^MOI (?:HTTP )?(\\d+)(?: · (.*))?$");

    private BankFacingMessages() {}

    public static String forBank(ErrorCode ec, String internal) {
        if (ec == null) return internal;
        switch (ec) {
            case CONNECTOR_UNAVAILABLE:
                return "The verification service is temporarily unavailable. Please try again in a moment.";
            case CONNECTOR_TIMEOUT:
                return "The verification service did not respond in time. Please try again.";
            case CONNECTOR_ERROR:
                return "The verification service could not process the request. Please try again in a few minutes; "
                        + "if the problem persists, contact support and quote the request ID.";
            case INTERNAL_ERROR:
                return "An unexpected error occurred. Please try again; if it persists, contact support and quote the request ID.";
            default:
                break;
        }
        if (internal == null) return ec.defaultMessage();
        if (!internal.contains("MOI")) return internal;

        // Provider-side auth/permission problems are ours to fix, not the bank's.
        if (ec == ErrorCode.INVALID_CREDENTIALS || ec == ErrorCode.FORBIDDEN) {
            return "The verification service refused the request. Please contact support and quote the request ID.";
        }
        // Provider input validation, e.g. "MOI 220 · Invalid biometrics": keep the code, drop the name.
        Matcher m = PROVIDER_VALIDATION.matcher(internal);
        if (m.matches()) {
            String detail = m.group(2) != null ? ": " + m.group(2) : "";
            return "The verification service rejected the request (code " + m.group(1) + ")" + detail;
        }
        return internal.replace("MOI", "the verification service");
    }
}
