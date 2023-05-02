


## Thermis

A flutter plugin for CHEQ flutter apps to print CHEQ receipts through USB Thermal Printer. 

Thermis uses [Receiptify](https://github.com/CHEQPlease/Receiptify) under the hood to generate receipts.

**How to use ?**
---
This plugin in yet not published on [pub.dev](https://pub.dev). Yet you can use this git repository directly in you project.

Add this line on your **pubspec.yaml**

```yaml
thermis:
    git:
      url: https://github.com/CHEQPlease/Thermis.git
      ref: release/1.0.0
```

**Printing a Receipt**
--
Prepare a receipt a JSON string (Receipt DTO) based on order object and send it to thermis for printing.
```css
	   //Preapare JSON DTO   
	   String receiptDTOJSON = 
			""" 
			{
		   "brandName":"CHEQ Diner1",
		   "orderType":"Self-Order",
		   "orderSubtitle":"Kiosk-Order",
		   "totalItems":"2",
		   "orderNo":"K10",
		   "tableNo":"234",
		   "receiptType":"customer",
		   "deviceType": "handheld"
		   "timeOfOrder":"Placed at : 01/12/2023 03:57 AM AKST",
		   "items":[
		      {
		         "itemName":"Salmon Fry",
		         "description":"  -- Olive\n  -- Deep Fried Salmon\n  -- ADD Addition 1\n  -- no Nuts\n  -- no Olive Oil\n  -- Substitution 1 SUB\n  -- allergy 1 ALLERGY\n",
		         "quantity":"1",
		         "price":"\$10.0",
		         "strikethrough":false
		      },
		      {
		         "itemName":"Water + Apple Pay",
		         "description":"  -- Onions\n",
		         "quantity":"1",
		         "price":"\$3.0",
		         "strikethrough":true
		      }
		   ],
		   "breakdown":[
		      {
		         "key":"Payment Type",
		         "value":"Card"
		      },
		      {
		         "key":"Card Type",
		         "value":"mc"
		      },
		      {
		         "key":"Card #:",
		         "value":"541333 **** 9999"
		      },
		      {
		         "key":"Card Entry",
		         "value":"CONTACTLESS"
		      },
		      {
		         "key":"",
		         "value":""
		      },
		      {
		         "key":"Sub Total",
		         "value":"\$21.01"
		      },
		      {
		         "key":"Area Tax",
		         "value":"\$1.00"
		      },
		      {
		         "key":"VAT",
		         "value":"\$2.10"
		      },
		      {
		         "key":"Customer Fee",
		         "value":"\$0.63"
		      },
		      {
		         "key":"Service Fee",
		         "value":"\$0.91"
		      },
		      {
		         "key":"Tax",
		         "value":"\$0.01"
		      },
		      {
		         "key":"GRAND TOTAL",
		         "value":"\$25.66",
		         "important":true
		      }
		   ]
		}
		""";

	   // Send the DTO to Thermis for Printing
       await Thermis.printCHEQReceipt(receiptDTOJSON);

```
**Check for Printer USB Connection**
--
```css
    await Thermis.isPrinterConnected();
```

**Opening the Cash Drawer**
--
```css
    await Thermis.openCashDrawer();
```
**Cut Receipt Paper**
--
```css
    Thermis.cutPaper();
```


Note
---------------
**Supported values for "receiptType" :**
*Customer,
Merchant,
Kitchen,
Kiosk*

**Supported values for "deviceType":**
*POS,
Handheld*
 
