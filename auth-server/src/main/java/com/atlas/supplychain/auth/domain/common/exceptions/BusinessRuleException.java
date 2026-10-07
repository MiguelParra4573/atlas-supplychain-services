package com.atlas.supplychain.auth.domain.common.exceptions;

public class BusinessRuleException extends RuntimeException {

    private final BusinessRuleCode code;

    public BusinessRuleException(BusinessRuleCode code) {
        super(code.getPublicMessage());
        this.code = code;
    }

    public BusinessRuleException(BusinessRuleCode code, String internalMessage) {
        super(internalMessage);
        this.code = code;
    }

    public BusinessRuleCode getCode() {
        return code;
    }
}
