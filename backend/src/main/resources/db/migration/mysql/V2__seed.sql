-- Seed: business profile + 12 customers (schema only, no invoices/payments/expenses).
-- GSTINs are kept because GstinValidatorTest reads this file from the classpath.
-- All names, GSTINs, phones and bank details are made up.
INSERT INTO business_profile (id,name,owner_name,address,city,state_code,gstin,registration_type,turnover_paise,udyam_registered,udyam_number,msme_category,phone,email,bank_name,account_name,account_number,ifsc,upi_id,cash_in_bank_paise,safety_buffer_paise,demo_date,language,text_size) VALUES (1,'My Business','Owner Name','','My City','27','27AAKFS4521Q1ZL','REGULAR',0,FALSE,NULL,NULL,'','','','','','','',0,15000000,NULL,'en','normal');

INSERT INTO customers (id,name,contact_person,phone,email,address,city,state_code,gstin,credit_days) VALUES
(1,'Deshmukh Foods Pvt Ltd','Anil Deshmukh','+91 98220 11001','accounts@deshmukhfoods.example','Plot 14, MIDC Bhosari, Pune 411026','Pune','27','27AADCD3311K1ZV',30),
(2,'Kulkarni Pharma Distributors','Meera Kulkarni','+91 98220 11002','meera@kulkarnipharma.example','204 Lotus House, Andheri East, Mumbai 400069','Mumbai','27','27AAFFK7720M1ZZ',30),
(3,'Patil Agro Exports','Sunil Patil','+91 98220 11003','sunil@patilagro.example','Gat 88, Dindori Road, Nashik 422004','Nashik','27','27AAGFP5150B1ZK',30),
(4,'Joshi Electronics','Prakash Joshi','+91 98220 11004','prakash@joshielec.example','Shop 7, Budhwar Peth, Pune 411002','Pune','27','27AAIPJ6643C1ZT',30),
(5,'Gaikwad Sweets & Snacks','Rohit Gaikwad','+91 98220 11005','rohit@gaikwadsweets.example','12 Laxmi Road, Pune 411030','Pune','27','27ABJPG2290D1ZW',30),
(6,'Mehta Textiles','Hiren Mehta','+91 98250 11006','hiren@mehtatex.example','Ring Road Textile Market, Surat 395002','Surat','24','24AAHFM8812E1ZF',45),
(7,'Shinde Hardware Mart','Ganesh Shinde','+91 98220 11007','ganesh.shinde@mail.example','Powai Naka, Satara 415001','Satara','27',NULL,15),
(8,'Pawar Dairy Products','Kavita Pawar','+91 98220 11008','kavita@pawardairy.example','MIDC Area, Baramati 413133','Baramati','27','27AAKFP4471F1Z2',30),
(9,'Bhosale Auto Components','Vikram Bhosale','+91 98220 11009','vikram@bhosaleauto.example','Survey 210, Chakan MIDC, Pune 410501','Chakan','27','27AALCB9034G1ZI',45),
(10,'Naik Cosmetics','Pooja Naik','+91 98220 11010','pooja@naikcosmetics.example','Wagle Estate, Thane 400604','Thane','27','27AAMFN3368H1ZW',30),
(11,'Rane Garments','Sachin Rane','+91 98220 11011','sachin@ranegarments.example','Shahupuri, Kolhapur 416001','Kolhapur','27','27AANFR5527J1ZP',30),
(12,'Jadhav Ayurvedic Products','Dr. Neha Jadhav','+91 98220 11012','neha@jadhavayur.example','Waluj MIDC, Chhatrapati Sambhajinagar 431136','Chhatrapati Sambhajinagar','27','27AAPFJ1185L1ZT',30);
