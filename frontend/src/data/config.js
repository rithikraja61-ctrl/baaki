// Frontend copy of key settings. The backend (application.yml) is the source of truth;
// the app loads live values from GET /api/config.
// GST rates: 0%, 5% and 18% are the main slabs since 22 September 2025, plus 3% and 40%.
// Check current rates on cbic-gst.gov.in before relying on them.
const config = {
  gstRatesBp: [0, 300, 500, 1800, 4000],
  rbiBankRatePercent: 5.75, // check rbi.org.in
  letterDeadlineDays: 7,
  safetyBufferPaise: 150000_00,
  units: ['pcs', 'box', 'kg', 'roll', 'set', 'job', 'hrs'],
  ladder: [
    { code: 'HEADS_UP', offsetDays: -3 },
    { code: 'DUE_TODAY', offsetDays: 0 },
    { code: 'FIRM', offsetDays: 7 },
    { code: 'FINAL', offsetDays: 15 },
    { code: 'DEMAND_LETTER', offsetDays: 30 },
    { code: 'NEXT_STEPS', offsetDays: 45 },
  ],
};
export default config;
