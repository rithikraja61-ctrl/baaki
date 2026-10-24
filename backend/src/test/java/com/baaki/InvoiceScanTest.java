package com.baaki;

import com.baaki.scan.InvoiceScanService;
import com.baaki.scan.ScanException;
import com.baaki.scan.ScanResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceScanTest {
    static final ObjectMapper M = new ObjectMapper();
    static final InvoiceScanService.Context CTX = new InvoiceScanService.Context("27", "27AAKFS4521Q1ZL", "REGULAR",
            List.of(0, 300, 500, 1800, 4000), 4,
            List.of(new InvoiceScanService.CustomerRef(1L, "Deshmukh Traders", "27AAPFU0939F1ZV", "27", 30),
                    new InvoiceScanService.CustomerRef(6L, "Mehta Plastics", "24AAHFM8812E1ZF", "24", 45)),
            Set.of("INV/26-27/0001"));

    static JsonNode j(String s) throws Exception { return M.readTree(s.replace('\'', '"')); }

    static boolean has(ScanResult r, String field, String code) {
        return r.flags().stream().anyMatch(f -> f.field().equals(field) && f.code().equals(code));
    }

    @Test
    void cleanSameStateInvoiceHasNoMismatch() throws Exception {
        ScanResult r = InvoiceScanService.build(j("{'isInvoice':true,'invoiceNumber':'INV/26-27/0099','invoiceDate':'2026-10-01',"
                + "'customerName':'Deshmukh Traders','customerGstin':'27AAPFU0939F1ZV','taxableTotal':10000,'cgst':900,'sgst':900,'igst':0,'total':11800,"
                + "'items':[{'description':'Boxes','hsnSac':'4819','quantity':100,'unit':'pcs','rate':100,'gstRatePercent':18,'taxableValue':10000}],'uncertainFields':[]}"), CTX);
        assertEquals(1L, r.matchedCustomerId());
        assertEquals("GSTIN", r.matchedBy());
        assertEquals("27", r.placeOfSupply());
        assertEquals(1180000L, r.computed().totalPaise());
        assertEquals("2026-10-31", r.dueDateFromCredit().toString());
        assertTrue(r.flags().stream().noneMatch(f -> f.code().endsWith("MISMATCH")), r.flags().toString());
        assertTrue(r.uncertain().isEmpty());
    }

    @Test
    void wrongPrintedTotalIsFlagged() throws Exception {
        ScanResult r = InvoiceScanService.build(j("{'isInvoice':true,'invoiceNumber':'A1','invoiceDate':'2026-10-01','customerGstin':'24AAHFM8812E1ZF',"
                + "'taxableTotal':10000,'igst':1800,'total':12000,"
                + "'items':[{'description':'Boxes','hsnSac':'4819','quantity':100,'rate':100,'gstRatePercent':18}],'uncertainFields':[]}"), CTX);
        assertEquals("24", r.placeOfSupply());
        assertEquals(180000L, r.computed().igstPaise());
        assertTrue(has(r, "totals.total", "TOTAL_MISMATCH"));
        assertFalse(has(r, "totals.igst", "TOTAL_MISMATCH"));
    }

    @Test
    void badGstinCheckDigitIsFlaggedAndUnsure() throws Exception {
        ScanResult r = InvoiceScanService.build(j("{'isInvoice':true,'customerName':'Someone New','customerGstin':'27AAPFU0939F1ZX',"
                + "'items':[{'description':'X','hsnSac':'4819','quantity':1,'rate':50,'gstRatePercent':18}],'uncertainFields':['items.0.rate']}"), CTX);
        assertFalse(r.customerGstinValid());
        assertTrue(has(r, "customerGstin", "GSTIN_CHECK_FAILED"));
        assertTrue(r.uncertain().contains("customerGstin"));
        assertTrue(r.uncertain().contains("items.0.rate"));
        assertTrue(has(r, "customer", "CUSTOMER_NOT_FOUND"));
        assertTrue(has(r, "invoiceNumber", "MISSING"));
    }

    @Test
    void duplicateNumberRateSnapAndLineMismatch() throws Exception {
        ScanResult r = InvoiceScanService.build(j("{'isInvoice':true,'invoiceNumber':'inv/26-27/0001','customerName':'M/s Deshmukh Traders',"
                + "'items':[{'description':'Tape','hsnSac':'39','quantity':'2','rate':'1,000.00','gstRatePercent':12,'taxableValue':2500}],'uncertainFields':[]}"), CTX);
        assertTrue(has(r, "invoiceNumber", "ALREADY_IN_BAAKI"));
        assertEquals("NAME", r.matchedBy());
        assertTrue(has(r, "items.0.gstRate", "RATE_NOT_ALLOWED"));
        assertTrue(has(r, "items.0.hsnSac", "HSN_TOO_SHORT"));
        assertTrue(has(r, "items.0.rate", "LINE_MISMATCH"));
        assertEquals(100000L, r.lines().get(0).ratePaise());
    }

    @Test
    void notAnInvoiceAndFileTypes() throws Exception {
        assertThrows(ScanException.class, () -> InvoiceScanService.build(j("{'isInvoice':false,'items':[],'uncertainFields':[]}"), CTX));
    }
}
