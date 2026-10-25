package com.baaki.dto;

import java.util.List;

public record LogLetterReq(Long customerId, List<Long> invoiceIds, String channel) {}
