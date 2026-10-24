/** Kampala Fare Collector 0.2.0 — attach to the workbook from Extensions → Apps Script. */
const COLLECTOR = Object.freeze({
  version: '0.2.0',
  timezone: 'Africa/Kampala',
  providers: ["SafeBoda", "Bolt", "Faras", "Lolo Cab", "Konnecta", "Little", "Ride Now", "Union", "Tinka"],
  routes: 'FC Routes', products: 'FC Products', quotes: 'FC Quotes', comparison: 'FC Comparison',
  maxSpreadSeconds: 60,
  quoteHeaders: ['Quote ID','Device ID','Run ID','Route ID','Pickup','Dropoff','Provider','Mode','Product ID','Product','Low UGX','High UGX','Currency','Capture epoch ms','Capture time (Kampala)','Received time (Kampala)','Route verification','Payment method','Promotion status','Raw fare','Fare row text','Reviewed']
});

function onOpen() {
  SpreadsheetApp.getUi().createMenu('Fare Collector')
    .addItem('Set up test tabs','installCollector')
    .addItem('Show connection settings','showCollectorConnection')
    .addItem('Refresh comparisons','refreshCollectorComparisons')
    .addSeparator().addItem('Rotate pairing token','rotateCollectorToken').addToUi();
}

function collectorSpreadsheet_() {
  const id=PropertiesService.getScriptProperties().getProperty('FC_SPREADSHEET_ID');
  if(!id)throw new Error('Run installCollector in the target spreadsheet first.');
  return SpreadsheetApp.openById(id);
}

function installCollector() {
  const lock = LockService.getScriptLock(); lock.waitLock(20000);
  try {
    const props = PropertiesService.getScriptProperties();
    const bound = SpreadsheetApp.getActiveSpreadsheet(), saved=props.getProperty('FC_SPREADSHEET_ID');
    if(saved&&bound&&bound.getId()!==saved)throw new Error('This project is already paired with a different spreadsheet. Use its original bound file.');
    if(!saved){
      if(!bound)throw new Error('Open the target spreadsheet and use Extensions → Apps Script, or set FC_SPREADSHEET_ID privately in Script properties.');
      props.setProperty('FC_SPREADSHEET_ID',bound.getId());
    }
    const ss = collectorSpreadsheet_();
    collectorSheet_(ss,COLLECTOR.routes,['Route ID','Pickup search','Dropoff search','Active','Notes']);
    const products = collectorSheet_(ss,COLLECTOR.products,['Product ID','Provider','Mode','Product','Aliases separated by |','Active']);
    collectorSheet_(ss,COLLECTOR.quotes,COLLECTOR.quoteHeaders);
    if (products.getLastRow() === 1) products.getRange(2,1,defaultCollectorProducts_().length,6).setValues(defaultCollectorProducts_());
    if (!props.getProperty('FC_PAIRING_TOKEN')) props.setProperty('FC_PAIRING_TOKEN',newCollectorToken_());
    props.setProperty('FC_INSTALLED','1');
    refreshComparisons_(ss);
  } finally { lock.releaseLock(); }
  showCollectorConnection();
}

function collectorSheet_(ss,name,headers) {
  let sheet=ss.getSheetByName(name);
  if(!sheet) sheet=ss.insertSheet(name);
  ensureCollectorGrid_(sheet,1,headers.length);
  if(sheet.getLastRow()>0) {
    const actual=sheet.getRange(1,1,1,headers.length).getValues()[0];
    if(actual.join('\u001f')!==headers.join('\u001f')) throw new Error(name+' already contains a different layout. Rename that tab before installing.');
  } else {
    sheet.getRange(1,1,1,headers.length).setValues([headers]).setBackground('#eeeeee').setFontColor('#000000').setFontWeight('bold');
    sheet.setFrozenRows(1); sheet.setHiddenGridlines(true); sheet.setColumnWidths(1,headers.length,150); sheet.setRowHeight(1,30);
  }
  return sheet;
}
function ensureCollectorGrid_(sheet,rows,columns) {
  if(sheet.getMaxRows()<rows)sheet.insertRowsAfter(sheet.getMaxRows(),rows-sheet.getMaxRows());
  if(sheet.getMaxColumns()<columns)sheet.insertColumnsAfter(sheet.getMaxColumns(),columns-sheet.getMaxColumns());
}

function defaultCollectorProducts_() { return [
  [
    "sb_saver",
    "SafeBoda",
    "Boda",
    "SafeBoda Saver",
    "SafeBoda Saver|Saver",
    true
  ],
  [
    "sb_plus",
    "SafeBoda",
    "Boda",
    "SafeBoda Plus",
    "SafeBoda Plus|Plus",
    true
  ],
  [
    "sb_car",
    "SafeBoda",
    "Car",
    "SafeCar",
    "SafeCar",
    true
  ],
  [
    "sb_xl",
    "SafeBoda",
    "Car",
    "SafeCar XL",
    "SafeCar XL",
    true
  ],
  [
    "bolt_boda",
    "Bolt",
    "Boda",
    "Bolt Boda",
    "Bolt Boda|Boda",
    true
  ],
  [
    "bolt_car",
    "Bolt",
    "Car",
    "Bolt",
    "Bolt",
    true
  ],
  [
    "bolt_xl",
    "Bolt",
    "Car",
    "Bolt XL",
    "Bolt XL|XL",
    true
  ],
  [
    "faras_boda",
    "Faras",
    "Boda",
    "Faras Boda",
    "Faras Boda|Boda",
    true
  ],
  [
    "faras_car",
    "Faras",
    "Car",
    "Faras",
    "Faras",
    true
  ],
  [
    "faras_xl",
    "Faras",
    "Car",
    "Faras XL",
    "Faras XL|XL",
    true
  ],
  [
    "sb_comfort",
    "SafeBoda",
    "Car",
    "SafeCar Comfort",
    "SafeCar Comfort|Comfort",
    true
  ],
  [
    "faras_economy",
    "Faras",
    "Car",
    "Faras Economy",
    "Faras Economy|Economy",
    true
  ],
  [
    "lolo_cab",
    "Lolo Cab",
    "Car",
    "Lolo Cab",
    "Lolo Cab|Lolo",
    true
  ],
  [
    "lolo_xtra",
    "Lolo Cab",
    "Car",
    "Lolo Xtra",
    "Lolo Xtra|Xtra",
    true
  ],
  [
    "lolo_cashless",
    "Lolo Cab",
    "Car",
    "Go Cashless",
    "Go Cashless",
    true
  ],
  [
    "lolo_corporate",
    "Lolo Cab",
    "Car",
    "Corporate",
    "Corporate",
    true
  ],
  [
    "lolo_boda",
    "Lolo Cab",
    "Boda",
    "Lolo Boda",
    "Lolo Boda|Boda",
    true
  ],
  [
    "konnecta_car",
    "Konnecta",
    "Car",
    "Konnecta",
    "Konnecta",
    true
  ],
  [
    "konnecta_boda",
    "Konnecta",
    "Boda",
    "Konnecta Boda",
    "Konnecta Boda|Boda",
    true
  ],
  [
    "little_basic",
    "Little",
    "Car",
    "Little Basic",
    "Little Basic|Basic",
    true
  ],
  [
    "little_comfort",
    "Little",
    "Car",
    "Little Comfort",
    "Little Comfort|Comfort",
    true
  ],
  [
    "little_boda",
    "Little",
    "Boda",
    "Little Boda",
    "Little Boda|Boda",
    true
  ],
  [
    "ridenow_car",
    "Ride Now",
    "Car",
    "Ride Now",
    "Ride Now|RideNow",
    true
  ],
  [
    "ridenow_xl",
    "Ride Now",
    "Car",
    "Ride Now XL",
    "Ride Now XL|RideNow XL|XL",
    true
  ],
  [
    "ridenow_boda",
    "Ride Now",
    "Boda",
    "Ride Now Boda",
    "Ride Now Boda|RideNow Boda|Boda",
    true
  ],
  [
    "union_car",
    "Union",
    "Car",
    "Union car",
    "Union car|Union",
    true
  ],
  [
    "union_max",
    "Union",
    "Car",
    "Union max",
    "Union max|Max",
    true
  ],
  [
    "union_boda",
    "Union",
    "Boda",
    "Union boda",
    "Union boda|Boda",
    true
  ],
  [
    "tinka_car",
    "Tinka",
    "Car",
    "Tinka",
    "Tinka|Tinka Go",
    true
  ],
  [
    "tinka_xl",
    "Tinka",
    "Car",
    "Tinka XL",
    "Tinka XL|XL",
    true
  ],
  [
    "tinka_boda",
    "Tinka",
    "Boda",
    "Tinka Boda",
    "Tinka Boda|Boda",
    true
  ]
]; }

function newCollectorToken_() { return (Utilities.getUuid()+Utilities.getUuid()).replace(/-/g,''); }
function collectorConnectionUrl_(saved,reported) {
  const published=value=>typeof value==='string'&&/^https:\/\/script\.google\.com\/macros\/s\/[A-Za-z0-9_-]+\/exec$/.test(value);
  return [saved,reported].find(published)||'Copy the published /exec URL from Deploy → Manage deployments.';
}
function showCollectorConnection() {
  const props=PropertiesService.getScriptProperties(),token=props.getProperty('FC_PAIRING_TOKEN');
  if(!token) throw new Error('Run installCollector first.');
  if(!SpreadsheetApp.getActiveSpreadsheet()) {
    console.log('Setup complete. For a standalone project, copy FC_PAIRING_TOKEN from Project Settings → Script properties. Copy the /exec URL from Deploy → Manage deployments.');
    return;
  }
  const url=collectorConnectionUrl_(props.getProperty('FC_WEB_APP_URL'),ScriptApp.getService().getUrl());
  const esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const html='<div style="font:15px Arial;padding:18px"><h2>Connect your phone</h2><p>Deployment URL</p><textarea readonly style="width:100%;height:70px">'+esc(url)+'</textarea><p>Pairing token — keep private</p><textarea readonly style="width:100%;height:65px">'+esc(token)+'</textarea><p>Paste both into the phone app’s Connection screen. Reload routes after changing FC Routes or FC Products.</p></div>';
  SpreadsheetApp.getUi().showModalDialog(HtmlService.createHtmlOutput(html).setWidth(540).setHeight(390),'Fare Collector connection');
}

function rotateCollectorToken() {
  const ui=SpreadsheetApp.getUi();
  if(ui.alert('Replace pairing token?','Phones using the old token will need the new token. Captured quotes stay in the sheet.',ui.ButtonSet.YES_NO)!==ui.Button.YES) return;
  PropertiesService.getScriptProperties().setProperty('FC_PAIRING_TOKEN',newCollectorToken_());
  showCollectorConnection();
}

function doGet() { return collectorJson_({ok:true,service:'Kampala Fare Collector',version:COLLECTOR.version}); }
function doPost(e) {
  try {
    if(!e || !e.postData || e.postData.contents.length>250000) throw new Error('Invalid request.');
    const request=JSON.parse(e.postData.contents);
    const props=PropertiesService.getScriptProperties();
    if(props.getProperty('FC_INSTALLED')!=='1') throw new Error('Run installCollector first.');
    if(!tokenMatches_(request.token,props.getProperty('FC_PAIRING_TOKEN'))) throw new Error('Connection not authorized.');
    if(!/^[a-zA-Z0-9-]{16,80}$/.test(String(request.device_id||''))) throw new Error('Invalid device ID.');
    const ss=collectorSpreadsheet_(),config=readCollectorConfig_(ss);
    if(request.action==='config') return collectorJson_({ok:true,version:COLLECTOR.version,config:config});
    if(request.action!=='submit') throw new Error('Unsupported action.');
    if(!Array.isArray(request.records)||request.records.length<1||request.records.length>100) throw new Error('Submit 1–100 quotes.');
    const now=Date.now();
    const validated=request.records.map(record=>validateCollectorRecord_(record,config,request.device_id,now));
    const lock=LockService.getScriptLock();lock.waitLock(20000);
    try {
      const sheet=ss.getSheetByName(COLLECTOR.quotes);if(!sheet)throw new Error('Run installCollector first.');
      const existing=sheet.getLastRow()>1?sheet.getRange(2,1,sheet.getLastRow()-1,COLLECTOR.quoteHeaders.length).getValues():[];
      const byId=new Map(existing.map(row=>[String(row[0]),row]));
      const append=[],accepted=[];
      for(const row of validated) {
        const old=byId.get(row[0]);
        if(old) {
          // An acknowledgement retry must identify the same quote, not overwrite one.
          const stable=[1,2,3,6,7,8,10,11,12,13,16,17,18,19,20,21];
          if(stable.some(index=>String(old[index])!==String(row[index]))) throw new Error('Quote ID conflict. No rows in this batch were appended.');
        } else { append.push(row);byId.set(row[0],row); }
        accepted.push(row[0]);
      }
      if(append.length) {ensureCollectorGrid_(sheet,sheet.getLastRow()+append.length,COLLECTOR.quoteHeaders.length);sheet.getRange(sheet.getLastRow()+1,1,append.length,COLLECTOR.quoteHeaders.length).setValues(append);}
      refreshComparisons_(ss);
      return collectorJson_({ok:true,accepted_ids:accepted,appended:append.length});
    } finally { lock.releaseLock(); }
  } catch(error) { return collectorJson_({ok:false,error:String(error.message||'Request failed.').slice(0,240)}); }
}

function collectorJson_(value) {return ContentService.createTextOutput(JSON.stringify(value)).setMimeType(ContentService.MimeType.JSON);}
function tokenMatches_(provided,expected) {
  if(typeof provided!=='string'||typeof expected!=='string'||!expected||provided.length>200) return false;
  const a=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,provided),b=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,expected);
  let difference=0;for(let i=0;i<a.length;i++)difference|=a[i]^b[i];return difference===0;
}
function activeCollectorRow_(value){return value===true||String(value).toLowerCase()==='true';}
function readCollectorConfig_(ss) {
  const get=(name,width)=>{const sheet=ss.getSheetByName(name);if(!sheet)throw new Error('Run installCollector first.');return sheet.getLastRow()>1?sheet.getRange(2,1,sheet.getLastRow()-1,width).getValues():[];};
  const routes=get(COLLECTOR.routes,5).filter(r=>activeCollectorRow_(r[3])).map(r=>({id:String(r[0]).trim(),pickup:String(r[1]).trim(),dropoff:String(r[2]).trim()}));
  const products=get(COLLECTOR.products,6).filter(r=>activeCollectorRow_(r[5])).map(r=>({id:String(r[0]).trim(),provider:String(r[1]).trim(),mode:String(r[2]).trim(),label:String(r[3]).trim(),aliases:String(r[4]).split('|').map(v=>v.trim()).filter(Boolean)}));
  if(routes.length>200||products.length<1||products.length>50)throw new Error('Check active routes and products.');
  if(new Set(routes.map(r=>r.id)).size!==routes.length||routes.some(r=>!r.id||!r.pickup||!r.dropoff))throw new Error('Routes need unique IDs, pickup and dropoff.');
  if(new Set(products.map(r=>r.id)).size!==products.length||products.some(r=>!r.id||!r.label||!COLLECTOR.providers.includes(r.provider)||!['Boda','Car'].includes(r.mode)||!r.aliases.length))throw new Error('Check product IDs, provider, mode and aliases.');
  return {routes:routes,products:products,timezone:COLLECTOR.timezone,max_spread_seconds:COLLECTOR.maxSpreadSeconds};
}

function parseCollectorPrice_(raw) {
  if(typeof raw!=='string'||raw.length>100)return null;
  const number='(?:\\d{1,3}(?:,\\d{3})+|\\d{3,7})',currency='(?:UGX|UShs?|Shs?)';
  const re=new RegExp('^\\s*(?:'+currency+'\\s*)?('+number+')(?:\\s*[-–—]\\s*(?:'+currency+'\\s*)?('+number+'))?\\s*(?:'+currency+')?\\s*$','i');
  const match=raw.replace(/\u00a0/g,' ').match(re);if(!match)return null;
  const low=Number(match[1].replace(/,/g,'')),high=match[2]?Number(match[2].replace(/,/g,'')):low;
  return Number.isSafeInteger(low)&&Number.isSafeInteger(high)&&low>=100&&high<=2000000&&low<=high?{low:low,high:high}:null;
}
function collectorCell_(value){const s=String(value==null?'':value);return /^[=+@-]/.test(s)?"'"+s:s;}
function kampalaTime_(epoch){return Utilities.formatDate(new Date(epoch),COLLECTOR.timezone,'yyyy-MM-dd HH:mm:ss');}

function validateCollectorRecord_(r,config,deviceId,now) {
  if(!r||typeof r!=='object')throw new Error('Invalid quote record.');
  for(const key of ['id','run_id'])if(!/^[a-zA-Z0-9-]{16,80}$/.test(String(r[key]||'')))throw new Error('Invalid quote or run ID.');
  const route=config.routes.find(x=>x.id===r.route_id),product=config.products.find(x=>x.id===r.product_id);
  if(!route||!product||product.provider!==r.provider||product.mode!==r.mode)throw new Error('Unknown route, provider, product or mode. Reload routes in the phone app.');
  if(route.pickup!==r.pickup||route.dropoff!==r.dropoff)throw new Error('The route changed. Review the saved phone records before uploading.');
  const parsed=parseCollectorPrice_(r.raw);
  if(!parsed||typeof r.low!=='number'||typeof r.high!=='number'||r.low!==parsed.low||r.high!==parsed.high||r.currency!=='UGX')throw new Error('Invalid or inconsistent UGX fare.');
  if(typeof r.captured_at!=='number'||!Number.isSafeInteger(r.captured_at)||r.captured_at<Date.UTC(2020,0,1)||r.captured_at>now+300000)throw new Error('Invalid capture time. Check the phone clock.');
  if(r.reviewed!==true||r.route_verification!=='user_confirmed')throw new Error('Review and confirm quotes on the phone before upload.');
  if(!['unspecified','cash','wallet','other'].includes(r.payment_method)||!['unspecified','none_shown','shown'].includes(r.promotion_status))throw new Error('Invalid payment or promotion selection.');
  if(typeof r.evidence!=='string'||r.evidence.length>1500)throw new Error('Invalid fare-row evidence.');
  return [r.id,deviceId,r.run_id,route.id,collectorCell_(route.pickup),collectorCell_(route.dropoff),product.provider,product.mode,product.id,collectorCell_(product.label),r.low,r.high,'UGX',r.captured_at,kampalaTime_(r.captured_at),kampalaTime_(now),r.route_verification,r.payment_method,r.promotion_status,collectorCell_(r.raw),collectorCell_(r.evidence),true];
}

function refreshCollectorComparisons(){const lock=LockService.getScriptLock();lock.waitLock(20000);try{refreshComparisons_(collectorSpreadsheet_());}finally{lock.releaseLock();}}
function comparisonRows_(quotes,config) {
  const groups=new Map();
  for(const q of quotes){
    const key=[q[2],q[3],q[7]].join('|');
    if(!groups.has(key))groups.set(key,{run:q[2],route:q[3],pickup:q[4],dropoff:q[5],mode:q[7],byProduct:new Map()});
    const g=groups.get(key),old=g.byProduct.get(q[8]);if(!old||Number(q[13])>Number(old[13]))g.byProduct.set(q[8],q);
  }
  const latest=new Map();
  for(const group of groups.values()){
    group.quotes=Array.from(group.byProduct.values());group.first=Math.min(...group.quotes.map(q=>Number(q[13])));group.last=Math.max(...group.quotes.map(q=>Number(q[13])));
    const key=group.route+'|'+group.mode,old=latest.get(key);if(!old||group.last>old.last)latest.set(key,group);
  }
  return Array.from(latest.values()).sort((a,b)=>a.route.localeCompare(b.route)||a.mode.localeCompare(b.mode)).map(g=>{
    const expected=config.products.filter(p=>p.mode===g.mode),spread=(g.last-g.first)/1000,flags=[];
    if(expected.some(p=>!g.byProduct.has(p.id)))flags.push('Missing categories');
    if(spread>config.max_spread_seconds)flags.push('Capture gap over '+config.max_spread_seconds+'s');
    const methods=new Set(g.quotes.map(q=>q[17])),promos=new Set(g.quotes.map(q=>q[18]));
    if(methods.has('unspecified'))flags.push('Payment unspecified');else if(methods.size>1)flags.push('Different payment methods');
    if(promos.has('unspecified'))flags.push('Promotion unspecified');else if(promos.size>1)flags.push('Different promotion states');
    const row=[g.route,g.pickup,g.dropoff,g.mode,g.run,kampalaTime_(g.first),kampalaTime_(g.last),spread,g.byProduct.size,expected.length,flags.length?flags.join('; '):'Reviewed; timing and payment checks met'];
    for(const p of config.products){const q=g.byProduct.get(p.id);row.push(q?q[10]:'',q?q[11]:'');}
    return row;
  });
}
function refreshComparisons_(ss) {
  const config=readCollectorConfig_(ss),raw=ss.getSheetByName(COLLECTOR.quotes);
  const quotes=raw&&raw.getLastRow()>1?raw.getRange(2,1,raw.getLastRow()-1,COLLECTOR.quoteHeaders.length).getValues():[];
  const headers=['Route ID','Pickup','Dropoff','Mode','Run ID','First capture (Kampala)','Last capture (Kampala)','Gap seconds','Captured categories','Expected categories','Review notes'];
  for(const p of config.products){headers.push(p.label+' low UGX',p.label+' high UGX');}
  let sheet=ss.getSheetByName(COLLECTOR.comparison);
  if(!sheet)sheet=ss.insertSheet(COLLECTOR.comparison);
  else if(sheet.getLastRow()>0&&sheet.getRange(1,1).getValue()!=='Route ID')throw new Error('FC Comparison contains another layout. Rename it before setup.');
  const old=sheet.getDataRange();old.clearContent();
  const rows=comparisonRows_(quotes,config);ensureCollectorGrid_(sheet,Math.max(1,rows.length+1),headers.length);
  sheet.getRange(1,1,1,headers.length).setValues([headers]).setBackground('#eeeeee').setFontColor('#000000').setFontWeight('bold').setWrap(true);
  if(rows.length){sheet.getRange(2,1,rows.length,headers.length).setValues(rows);sheet.getRange(2,8,rows.length,1).setNumberFormat('0.0');sheet.getRange(2,9,rows.length,2).setNumberFormat('0');sheet.getRange(2,12,rows.length,headers.length-11).setNumberFormat('#,##0');}
  sheet.setFrozenRows(1);sheet.setFrozenColumns(1);sheet.setHiddenGridlines(true);sheet.setColumnWidths(1,headers.length,140);sheet.setColumnWidth(2,230);sheet.setColumnWidth(3,230);sheet.setColumnWidth(11,350);sheet.setRowHeight(1,48);
  SpreadsheetApp.flush();
}
