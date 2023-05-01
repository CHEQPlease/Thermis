## Thermis

A flutter plugin for CHEQ flutter apps to print CHEQ receipts through USB Thermal Printer.

Allowed Values for receiptType :  
CUSTOMER_P
MERCHANT_P
KITCHEN_P
KIOSK_P
CUSTOMER_H
MERCHANT_H
KITCHEN_H
KIOSK_H

Details  
CUSTOMER : Customer Receipt  
MERCHANT : Merchant Receipt  
KITCHEN : Kitchen Receipt  
KIOSK : Kiosk Receipt

H -  Handheld  
P -  POS

If we want to print a receipt for a customer from a handheld device, we need to use **CUSTOMER_H**  
Similarly, if we want to print a receipt for a merchant from a POS device, we need to use **MERCHANT_P**

**Printing a Receipt**

Allowed Values for receiptType :  
CUSTOMER_P
MERCHANT_P
KITCHEN_P
KIOSK_P
CUSTOMER_H
MERCHANT_H
KITCHEN_H
KIOSK_H

Details  
CUSTOMER : Customer Receipt  
MERCHANT : Merchant Receipt  
KITCHEN : Kitchen Receipt  
KIOSK : Kiosk Receipt

H -  Handheld  
P -  POS

If we want to print a receipt for a customer from a handheld device, we need to use **CUSTOMER_H**  
Similarly, if we want to print a receipt for a merchant from a POS device, we need to use **MERCHANT_P**

Prepare a receipt a JSON string (Receipt DTO) based on order object and send it to thermis for printing.

	   //Preapare JSON DTO   
	   String receiptDTOJSON = 
			   """ {
		   "brandName":"CHEQ Diner1",
		   "orderType":"Self-Order",
		   "orderSubtitle":"Kiosk-Order",
		   "totalItems":"2",
		   "orderNo":"K10",
		   "tableNo":"234",
		   "receiptType":"kiosk_p", 
		   "timeOfOrder":"Placed at : 01/12/2023 03:57 AM AKST",
		   "items":[
		      {
		         "itemName":"Salmon Fry",
		         "description":"  -- Olive\n  -- Deep Fried Salmon\n  -- ADD Addition 1\n  -- no Nuts\n  -- no Olive Oil\n  -- Substitution 1 SUB\n  -- allergy 1 ALLERGY\n",
		         "quantity":"1",
		         "price":"$10.0",
		         "strikethrough":false
		      },
		      {
		         "itemName":"Water + Apple Pay",
		         "description":"  -- Onions\n",
		         "quantity":"1",
		         "price":"$1.0",
		         "strikethrough":true
		      }
		   ],
		   "breakdown":[
		      {
		         "key":"Payment Type",
		         "value":"Card",
		         "important":null
		      },
		      {
		         "key":"Card Type",
		         "value":"mc",
		         "important":null
		      },
		      {
		         "key":"Card #:",
		         "value":"541333 **** 9999",
		         "important":null
		      },
		      {
		         "key":"Card Entry",
		         "value":"CONTACTLESS",
		         "important":null
		      },
		      {
		         "key":"",
		         "value":"",
		         "important":null
		      },
		      {
		         "key":"Sub Total",
		         "value":"$21.01",
		         "important":null
		      },
		      {
		         "key":"Area Tax",
		         "value":"$1.00",
		         "important":null
		      },
		      {
		         "key":"VAT",
		         "value":"$2.10",
		         "important":null
		      },
		      {
		         "key":"Customer Fee",
		         "value":"$0.63",
		         "important":null
		      },
		      {
		         "key":"Service Fee",
		         "value":"$0.91",
		         "important":null
		      },
		      {
		         "key":"Tax",
		         "value":"$0.01",
		         "important":null
		      },
		      {
		         "key":"GRAND TOTAL",
		         "value":"$25.66",
		         "important":true
		      }
		   ]
		}"""

	   // Send the DTO to Thermis for Printing
       await Thermis.printCHEQReceipt(receiptDTOJSON);

**Check for Printer USB Connection**

    await Thermis.isPrinterConnected();

**Opening the Cash Drawer**

    await Thermis.openCashDrawer();

**Cut Receipt Paper**

    Thermis.cutPaper();


    
