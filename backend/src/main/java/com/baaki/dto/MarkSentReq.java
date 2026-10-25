package com.baaki.dto;

import java.util.List;

public record MarkSentReq(List<Long> invoiceIds, String stage, String channel, String message) {}
