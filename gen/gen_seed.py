import random, datetime as dt, os, json
random.seed(7)
OUT = '/home/daytona/workspace/baaki'
CH = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
def gstin(base14):
    s = 0
    for i, c in enumerate(base14):
        p = CH.index(c) * (1 if i % 2 == 0 else 2)
        s += p // 36 + p % 36
    return base14 + CH[(36 - s % 36) % 36]
def rd(a, b):  # half-up division, a>=0
    return (2 * a + b) // (2 * b)
def q(s): return "'" + s.replace("'", "''") + "'" if s is not None else 'NULL'
D = dt.date
TODAY = D(2026, 10, 24)
BIZ_STATE = '27'
biz_gstin = gstin('27AAKFS4521Q1Z')

# id, name, contact, phone, email, city, address, state, gstin14|None, credit, lateness(mean days late), sd
C = [
 (1,'Deshmukh Foods Pvt Ltd','Anil Deshmukh','+91 98220 11001','accounts@deshmukhfoods.example','Pune','Plot 14, MIDC Bhosari, Pune 411026','27','27AADCD3311K1Z',30,0,1),
 (2,'Kulkarni Pharma Distributors','Meera Kulkarni','+91 98220 11002','meera@kulkarnipharma.example','Mumbai','204 Lotus House, Andheri East, Mumbai 400069','27','27AAFFK7720M1Z',30,1,1),
 (3,'Patil Agro Exports','Sunil Patil','+91 98220 11003','sunil@patilagro.example','Nashik','Gat 88, Dindori Road, Nashik 422004','27','27AAGFP5150B1Z',30,8,3),
 (4,'Joshi Electronics','Prakash Joshi','+91 98220 11004','prakash@joshielec.example','Pune','Shop 7, Budhwar Peth, Pune 411002','27','27AAIPJ6643C1Z',30,24,5),
 (5,'Gaikwad Sweets & Snacks','Rohit Gaikwad','+91 98220 11005','rohit@gaikwadsweets.example','Pune','12 Laxmi Road, Pune 411030','27','27ABJPG2290D1Z',30,6,2),
 (6,'Mehta Textiles','Hiren Mehta','+91 98250 11006','hiren@mehtatex.example','Surat','Ring Road Textile Market, Surat 395002','24','24AAHFM8812E1Z',45,10,3),
 (7,'Shinde Hardware Mart','Ganesh Shinde','+91 98220 11007','ganesh.shinde@mail.example','Satara','Powai Naka, Satara 415001','27',None,15,20,6),
 (8,'Pawar Dairy Products','Kavita Pawar','+91 98220 11008','kavita@pawardairy.example','Baramati','MIDC Area, Baramati 413133','27','27AAKFP4471F1Z',30,0,1),
 (9,'Bhosale Auto Components','Vikram Bhosale','+91 98220 11009','vikram@bhosaleauto.example','Chakan','Survey 210, Chakan MIDC, Pune 410501','27','27AALCB9034G1Z',45,28,6),
 (10,'Naik Cosmetics','Pooja Naik','+91 98220 11010','pooja@naikcosmetics.example','Thane','Wagle Estate, Thane 400604','27','27AAMFN3368H1Z',30,7,3),
 (11,'Rane Garments','Sachin Rane','+91 98220 11011','sachin@ranegarments.example','Kolhapur','Shahupuri, Kolhapur 416001','27','27AANFR5527J1Z',30,1,1),
 (12,'Jadhav Ayurvedic Products','Dr. Neha Jadhav','+91 98220 11012','neha@jadhavayur.example','Chhatrapati Sambhajinagar','Waluj MIDC, Chhatrapati Sambhajinagar 431136','27','27AAPFJ1185L1Z',30,30,8),
]
ITEMS = [  # desc, hsn, unit, rate paise, rate bp
 ('3-ply corrugated carton 12x10x8 in','4819','pcs',1850,500),
 ('5-ply corrugated carton 18x14x12 in','4819','pcs',4200,500),
 ('Printed mono carton (2 colour)','4819','pcs',950,500),
 ('Corrugated partition inserts','4808','pcs',600,500),
 ('BOPP adhesive tape 48mm x 65m','3919','roll',5500,1800),
 ('Carton design & printing plate charges','998912','job',350000,1800),
]
sql = []
sql.append("INSERT INTO business_profile (id,name,owner_name,address,city,state_code,gstin,registration_type,turnover_paise,udyam_registered,udyam_number,msme_category,phone,email,bank_name,account_name,account_number,ifsc,upi_id,cash_in_bank_paise,safety_buffer_paise,demo_date,language,text_size) VALUES "
           f"(1,'Sharma Packaging','Rakesh Sharma','Gat No. 412, Kudalwadi, Chikhali, Pune 411062','Pune','27','{biz_gstin}','REGULAR',{3_20_00_000*100},TRUE,'UDYAM-MH-26-0123456','SMALL','+91 98900 12345','accounts@sharmapackaging.example','Example Co-operative Bank','Sharma Packaging','000123456789012','EXCB0000123','sharmapackaging@exampleupi',{2_60_000*100},{1_50_000*100},'2026-10-24','en','normal');")
cust_rows = []
for c in C:
    g = gstin(c[8]) if c[8] else None
    cust_rows.append(f"({c[0]},{q(c[1])},{q(c[2])},{q(c[3])},{q(c[4])},{q(c[6])},{q(c[5])},'{c[7]}',{q(g)},{c[9]})")
sql.append("INSERT INTO customers (id,name,contact_person,phone,email,address,city,state_code,gstin,credit_days) VALUES\n" + ",\n".join(cust_rows) + ";")
CUST = {c[0]: c for c in C}

# payment history Apr-Jun (older months)
hist = []
hist_data = []
for c in C:
    for k in range(3):
        due = D(2026, 4, 10) + dt.timedelta(days=28 * k + c[0])
        late = max(-3, int(random.gauss(c[10], c[11])))
        amt = random.randint(30, 120) * 100000
        hist.append(f"({c[0]},'OLD/25-26/{c[0]:02d}{k}','{due}','{due + dt.timedelta(days=late)}',{amt})")
        hist_data.append(dict(customerId=c[0], reference=f'OLD/25-26/{c[0]:02d}{k}', dueDate=str(due), paidDate=str(due + dt.timedelta(days=late)), amountPaise=amt))
sql.append("INSERT INTO payment_history (customer_id,reference,due_date,paid_date,amount_paise) VALUES\n" + ",\n".join(hist) + ";")

def make_lines(cid, big=False):
    n = random.randint(1, 3)
    picks = random.sample(range(5), n)
    if random.random() < 0.15: picks.append(5)
    lines = []
    for i in picks:
        d, h, u, r, bp = ITEMS[i]
        qty = 1 if u == 'job' else random.choice([500, 800, 1000, 1500, 2000, 2500, 3000]) * (2 if big else 1)
        if u == 'roll': qty = random.choice([24, 48, 72, 96])
        gross = qty * r
        disc = rd(gross * 2, 100) if random.random() < 0.25 else 0
        lines.append([d, h, u, qty, r, disc, bp])
    return lines

def compute(lines, same):
    out, T = [], dict(taxable=0, cgst=0, sgst=0, igst=0)
    for d, h, u, qty, r, disc, bp in lines:
        tx = qty * r - disc
        if same:
            cg = rd(tx * bp, 20000); sg = cg; ig = 0
        else:
            cg = sg = 0; ig = rd(tx * bp, 10000)
        out.append((d, h, u, qty, r, disc, bp, tx, cg, sg, ig, tx + cg + sg + ig))
        T['taxable'] += tx; T['cgst'] += cg; T['sgst'] += sg; T['igst'] += ig
    gross = T['taxable'] + T['cgst'] + T['sgst'] + T['igst']
    T['round'] = rd(gross, 100) * 100 - gross
    T['total'] = gross + T['round']
    return out, T

# schedule of 40 invoices Jul 1 .. Oct 22
dates = sorted(D(2026, 7, 1) + dt.timedelta(days=random.randint(0, 113)) for _ in range(40))
custs = [((i * 5) % 12) + 1 for i in range(40)]
inv, lines_sql, pays, allocs, events = [], [], [], [], []
seq = 0; pay_id = 0
special = {}  # filled after
records = []
for i, (d, cid) in enumerate(zip(dates, custs)):
    c = CUST[cid]
    lines = make_lines(cid, big=(cid in (4, 9, 12) and i % 3 == 0))
    ln, T = compute(lines, c[7] == BIZ_STATE)
    records.append(dict(idx=i, date=d, cid=cid, lines=ln, T=T, due=d + dt.timedelta(days=c[9])))
# make the last invoice a draft
records[-1]['draft'] = True
# mark disputed: latest overdue Bhosale invoice
for r in records:
    r['outstanding'] = r['T']['total']
for r in records:
    if r.get('draft'): continue
    seq += 1; r['no'] = f"INV/26-27/{seq:04d}"
# decide payments
reserved = set()
def pick(cid, cond):
    for r in records:
        if r['cid'] == cid and not r.get('draft') and r['idx'] not in reserved and cond(r):
            reserved.add(r['idx']); return r
    return None
over = lambda r: r['due'] < TODAY
# reserve invoices for bank statement scenarios (must stay unpaid)
bs = {}
bs['exact'] = pick(2, lambda r: r['due'] >= TODAY - dt.timedelta(days=10))
bs['part'] = pick(4, over)
bs['multi'] = [pick(3, lambda r: r['date'] >= D(2026, 9, 1)), pick(3, lambda r: r['date'] >= D(2026, 9, 1))]
bs['tds'] = pick(10, lambda r: r['date'] >= D(2026, 9, 1))
bs['overpay'] = pick(5, lambda r: r['date'] >= D(2026, 9, 1))
disputed = pick(9, over)
for k, v in bs.items(): print(k, v and ([x['no'] for x in v] if isinstance(v, list) else v['no']))
print('disputed', disputed and disputed['no'])
partials = 0
for r in records:
    r['status'] = 'DRAFT' if r.get('draft') else 'SENT'
    if r.get('draft') or r['idx'] in reserved: continue
    c = CUST[r['cid']]
    late = max(-3, int(random.gauss(c[10], c[11])))
    paid_on = r['due'] + dt.timedelta(days=late)
    if paid_on <= TODAY - dt.timedelta(days=1):
        amt = r['T']['total']
        if partials < 2 and c[10] >= 7 and r['T']['total'] > 4000000:
            amt = rd(r['T']['total'], 200000) * 100000 // 100000 * 1  # ~half
            amt = (r['T']['total'] // 2) // 100000 * 100000
            partials += 1
        pay_id += 1
        pays.append(f"({pay_id},'{paid_on}',{amt},'NEFT{random.randint(100000,999999)}','NEFT CR {c[1].upper()} {r['no']}',{q(c[1])},'HISTORY','CONFIRMED')")
        allocs.append(f"({pay_id},{r['idx']+1},{amt})")
        r['outstanding'] -= amt
        events.append(f"({r['idx']+1},{r['cid']},'{paid_on} 11:00:00','PAYMENT',NULL,NULL,'Payment of {amt} paise received')")
        if r['outstanding'] == 0: r['status'] = 'PAID'
if disputed:
    disputed['status'] = 'DISPUTED'
for r in records:
    c = CUST[r['cid']]; T = r['T']
    place = c[7]
    rid = r['idx'] + 1
    token = ''.join(random.choice('abcdefghjkmnpqrstuvwxyz23456789') for _ in range(12))
    sent_at = 'NULL' if r['status'] == 'DRAFT' else f"'{r['date']} 10:00:00'"
    inv.append(f"({rid},{q(r.get('no'))},{r['cid']},'{r['date']}','{r['due']}','{r['status']}','TAX_INVOICE','{place}',FALSE,NULL,{T['taxable']},{T['cgst']},{T['sgst']},{T['igst']},{T['round']},{T['total']},'{token}',{sent_at})")
    for n, l in enumerate(r['lines']):
        d, h, u, qty, rate, disc, bp, tx, cg, sg, ig, tot = l
        lines_sql.append(f"({rid},{n+1},{q(d)},'{h}',{qty},'{u}',{rate},{disc},{bp},{tx},{cg},{sg},{ig},{tot})")
    events.append(f"({rid},{r['cid']},'{r['date']} 09:30:00','CREATED',NULL,NULL,'Invoice created')")
    if r['status'] != 'DRAFT':
        events.append(f"({rid},{r['cid']},'{r['date']} 10:00:00','SENT',NULL,NULL,'Invoice sent to customer')")
sql.append("INSERT INTO invoices (id,invoice_number,customer_id,invoice_date,due_date,status,doc_type,place_of_supply,reverse_charge,delivery_address,taxable_paise,cgst_paise,sgst_paise,igst_paise,round_off_paise,total_paise,pay_token,sent_at) VALUES\n" + ",\n".join(inv) + ";")
sql.append("INSERT INTO invoice_lines (invoice_id,line_no,description,hsn_sac,quantity,unit,rate_paise,discount_paise,gst_rate_bp,taxable_paise,cgst_paise,sgst_paise,igst_paise,total_paise) VALUES\n" + ",\n".join(lines_sql) + ";")
sql.append("INSERT INTO payments (id,payment_date,amount_paise,reference,description,payer_name,source,status) VALUES\n" + ",\n".join(pays) + ";")
sql.append("INSERT INTO payment_allocations (payment_id,invoice_id,amount_paise) VALUES\n" + ",\n".join(allocs) + ";")
sql.append(f"INSERT INTO invoice_sequence (seq_key,next_value) VALUES ('INV/26-27',{seq+1});")
if disputed:
    rid = disputed['idx'] + 1
    sql.append(f"INSERT INTO inbox_items (invoice_id,kind,reason,status,created_at) VALUES ({rid},'DISPUTE','10% of the 5-ply cartons arrived crushed; please send a revised invoice for the damaged quantity.','OPEN','2026-10-18 15:20:00');")
    events.append(f"({rid},{disputed['cid']},'2026-10-18 15:20:00','DISPUTE',NULL,NULL,'Customer raised a dispute: damaged cartons')")
# one promise to pay on a Jadhav overdue invoice
pr = next((r for r in records if r['cid'] == 12 and r['status'] == 'SENT' and r['due'] < TODAY and r['idx'] not in reserved), None)
if pr:
    sql.append(f"UPDATE invoices SET promised_date='2026-10-30' WHERE id={pr['idx']+1};")
    events.append(f"({pr['idx']+1},12,'2026-10-21 12:00:00','PROMISE',NULL,NULL,'Customer promised to pay on 2026-10-30')")
sql.append("INSERT INTO invoice_events (invoice_id,customer_id,event_at,type,stage,channel,details) VALUES\n" + ",\n".join(events) + ";")
# expenses
exp = [
 ("Salaries (8 staff)","SALARY",2_40_000,'MONTHLY',1,None),
 ("Factory rent","RENT",45_000,'MONTHLY',5,None),
 ("Electricity (MSEDCL)","ELECTRICITY",22_000,'MONTHLY',12,None),
 ("GST payment (GSTR-3B)","GST",38_000,'MONTHLY',20,None),
 ("Kraft paper - Shree Paper Mills","SUPPLIER",1_85_000,'ONCE',None,'2026-11-04'),
 ("Kraft paper - Shree Paper Mills","SUPPLIER",2_10_000,'ONCE',None,'2026-12-02'),
 ("Printing ink & adhesive - Colourtech","SUPPLIER",48_000,'ONCE',None,'2026-11-18'),
 ("Corrugation machine EMI","LOAN",32_000,'MONTHLY',10,None),
 ("Kraft paper - Shree Paper Mills","SUPPLIER",1_95_000,'ONCE',None,'2027-01-06'),
]
sql.append("INSERT INTO expenses (name,category,amount_paise,recurrence,day_of_month,due_date) VALUES\n" + ",\n".join(
    f"({q(n)},'{cat}',{a*100},'{rec}',{dm if dm else 'NULL'},{q(dd)})" for n, cat, a, rec, dm, dd in exp) + ";")
os.makedirs(f'{OUT}/backend/src/main/resources/db/migration', exist_ok=True)
open(f'{OUT}/backend/src/main/resources/db/migration/V2__seed.sql', 'w').write("-- Generated by gen/gen_seed.py. All businesses, GSTINs, phones and bank details are made up.\n" + "\n\n".join(sql) + "\n")
# bank statement
def outst(r): return r['outstanding']
rows = ["Date,Description,Reference,Debit,Credit,Balance"]
bal = 260000_00
def add(d, desc, ref, cr=0, dr=0):
    global bal; bal += cr - dr
    rows.append(f"{d},\"{desc}\",{ref},{dr/100:.2f},{cr/100:.2f},{bal/100:.2f}")
e = bs['exact']; add('2026-10-20', f"NEFT CR KULKARNI PHARMA DISTRIBUTORS {e['no']}", 'N2930011', cr=outst(e))
p = bs['part']; amt = (outst(p) // 2) // 100000 * 100000; add('2026-10-21', f"IMPS JOSHI ELECTRONICS PART PAYMENT {p['no']}", 'I7781202', cr=amt)
m1, m2 = bs['multi']; add('2026-10-21', f"RTGS PATIL AGRO EXPORTS {m1['no']} {m2['no']}", 'R5520193', cr=outst(m1) + outst(m2))
add('2026-10-22', 'UPI DR ELECTRICITY MSEDCL', 'U8812004', dr=1_250_00)
t = bs['tds']; add('2026-10-22', f"NEFT CR NAIK COSMETICS {t['no']}", 'N2930785', cr=outst(t) - rd(t['T']['taxable'] * 2, 100))
o = bs['overpay']; add('2026-10-23', f"NEFT CR GAIKWAD SWEETS {o['no']}", 'N2931120', cr=outst(o) + 2_500_00)
if disputed: add('2026-10-23', f"NEFT CR BHOSALE AUTO COMPONENTS {disputed['no']}", 'N2931466', cr=outst(disputed) * 9 // 10)
add('2026-10-23', 'CASH DEPOSIT PUNE CHIKHALI BRANCH', 'C0041877', cr=15_000_00)
os.makedirs(f'{OUT}/frontend/src/data', exist_ok=True)
open(f'{OUT}/frontend/src/data/bank-statement.csv', 'w').write("\n".join(rows) + "\n")
print(biz_gstin, [gstin(c[8]) for c in C if c[8]])
print('seq', seq, 'payments', pay_id)

# Same sample data as readable JSON in frontend/src/data (the database seed above is generated from the same values).
data = dict(
    note='Generated by gen/gen_seed.py together with V2__seed.sql. All names, GSTINs, phones and bank details are made up. Money is in paise.',
    demoDate=str(TODAY),
    business=dict(name='Sharma Packaging', ownerName='Rakesh Sharma', gstin=biz_gstin, stateCode=BIZ_STATE, registrationType='REGULAR',
                  turnoverPaise=3_20_00_000*100, udyamNumber='UDYAM-MH-26-0123456', msmeCategory='SMALL', cashInBankPaise=2_60_000*100, safetyBufferPaise=1_50_000*100),
    customers=[dict(id=c[0], name=c[1], contactPerson=c[2], phone=c[3], email=c[4], city=c[5], address=c[6], stateCode=c[7],
                    gstin=gstin(c[8]) if c[8] else None, creditDays=c[9],
                    preferredLanguage={3:'mr',5:'mr',7:'mr',8:'mr',11:'mr',12:'hi'}.get(c[0], 'en')) for c in C],  # set in V4__languages.sql
    invoices=[dict(id=r['idx']+1, invoiceNumber=r.get('no'), customerId=r['cid'], invoiceDate=str(r['date']), dueDate=str(r['due']), status=r['status'],
                   taxablePaise=r['T']['taxable'], cgstPaise=r['T']['cgst'], sgstPaise=r['T']['sgst'], igstPaise=r['T']['igst'], roundOffPaise=r['T']['round'],
                   totalPaise=r['T']['total'], outstandingPaise=r['outstanding'],
                   lines=[dict(description=l[0], hsnSac=l[1], unit=l[2], quantity=l[3], ratePaise=l[4], discountPaise=l[5], gstRateBp=l[6]) for l in r['lines']])
              for r in records],
    paymentHistory=hist_data,
    expenses=[dict(name=n, category=cat, amountPaise=a*100, recurrence=rec, dayOfMonth=dm, dueDate=dd) for n, cat, a, rec, dm, dd in exp],
)
open(f'{OUT}/frontend/src/data/sample-data.json', 'w').write(json.dumps(data, indent=1, ensure_ascii=False) + "\n")
